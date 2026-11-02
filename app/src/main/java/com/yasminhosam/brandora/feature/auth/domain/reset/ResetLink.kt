package com.yasminhosam.brandora.feature.auth.domain.reset

/**
 * Pure, framework-free extraction of the reset token from an app link. Takes the raw URI
 * [String] (never android.net.Uri) so every branch runs in plain JVM tests, and validates
 * scheme + exact host + exact path BEFORE the fragment is even looked at: only a link on
 * our own HTTPS origin and reset path can reach the token parser.
 *
 * The token lives in the FRAGMENT (#token=...), never a query parameter, so it can never
 * leak through referrers or query-string logging. The link is rejected outright (null)
 * when anything about it is ambiguous - including a duplicated token parameter, which the
 * backend's reference extractor also treats as invalid.
 */

// The one accepted origin and path; the intent-filter in the manifest mirrors these values.
private const val EXPECTED_SCHEME = "https"
private const val EXPECTED_HOST = "brandora-backend-production-85ec.up.railway.app"
private const val EXPECTED_PATH = "/auth/reset-password"
private const val TOKEN_PARAM = "token"
private const val TOKEN_PREFIX = "$TOKEN_PARAM="

/** Returns the reset token, or null when the link is not exactly our reset link (or has no usable token). */
fun extractResetToken(link: String): String? {
    // Split off the fragment first: everything after '#' is opaque to the origin checks.
    val hashIndex = link.indexOf('#')
    if (hashIndex < 0) return null // no fragment at all
    val base = link.substring(0, hashIndex)
    val fragment = link.substring(hashIndex + 1)

    // scheme:// — compared case-insensitively (Android normalizes schemes to lowercase).
    val schemeSeparator = base.indexOf("://")
    if (schemeSeparator != EXPECTED_SCHEME.length) return null
    if (!base.regionMatches(0, EXPECTED_SCHEME, 0, EXPECTED_SCHEME.length, ignoreCase = true)) {
        return null
    }

    val authorityAndPath = base.substring(schemeSeparator + 3)
    // The authority ends at the first '/'; an absolute path must follow.
    val pathStart = authorityAndPath.indexOf('/')
    if (pathStart < 0) return null
    val authority = authorityAndPath.substring(0, pathStart)
    // A query may be present (the intent-filter matches the path only) but is never read:
    // only the fragment can carry the token.
    val pathEnd = authorityAndPath.indexOf('?').let { if (it < 0) authorityAndPath.length else it }
    val path = authorityAndPath.substring(pathStart, pathEnd)

    // Exact match on the WHOLE authority: this rejects userinfo (evil@host), explicit
    // ports (host:443) and any lookalike host without leaning on Uri normalization.
    if (authority != EXPECTED_HOST) return null
    if (path != EXPECTED_PATH) return null

    return tokenFromFragment(fragment)
}

/** Parses `token=<value>` pairs out of the fragment; a duplicate makes the link untrustworthy. */
private fun tokenFromFragment(fragment: String): String? {
    var token: String? = null
    for (pair in fragment.split('&')) {
        if (!pair.startsWith(TOKEN_PREFIX)) continue
        if (token != null) return null // duplicated token parameter: reject the whole link
        token = percentDecode(pair.substring(TOKEN_PREFIX.length)) ?: return null
    }
    return token?.takeIf { it.isNotBlank() }
}

/**
 * Minimal percent-decoding (%XX -> byte) that keeps '+' literal: this is a URI fragment,
 * not a form body, so '+' must never become a space. A malformed sequence rejects the link.
 */
private fun percentDecode(raw: String): String? {
    if (raw.indexOf('%') < 0) return raw
    // Worst case is 3 output bytes per input char (raw non-ASCII -> UTF-8).
    val out = ByteArray(raw.length * 3)
    var written = 0
    var i = 0
    while (i < raw.length) {
        val c = raw[i]
        when {
            c == '%' -> {
                if (i + 2 >= raw.length) return null // truncated: "%", "%A"
                val hi = Character.digit(raw[i + 1], 16)
                val lo = Character.digit(raw[i + 2], 16)
                if (hi < 0 || lo < 0) return null // not hex
                out[written++] = ((hi shl 4) or lo).toByte()
                i += 3
            }
            c.code < 0x80 -> {
                out[written++] = c.code.toByte()
                i++
            }
            else -> {
                for (byte in c.toString().toByteArray(Charsets.UTF_8)) out[written++] = byte
                i++
            }
        }
    }
    return String(out, 0, written, Charsets.UTF_8)
}
