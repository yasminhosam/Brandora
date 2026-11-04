package com.yasminhosam.brandora.feature.auth.domain.reset

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The reset link is the only way into the Reset screen, so every branch of the pure
 * extractor is pinned here: exact origin checks BEFORE the fragment, fragment-only
 * token extraction, and rejection of anything ambiguous (including duplicates, which
 * the backend's reference extractor also treats as invalid).
 */
class ResetLinkParserTest {

    private val valid =
        "https://brandora-backend-production-85ec.up.railway.app/auth/reset-password"

    @Test
    fun `the canonical link yields its token`() {
        assertEquals("abc123", extractResetToken("$valid#token=abc123"))
    }

    @Test
    fun `the scheme is matched case-insensitively (Android normalizes schemes too)`() {
        assertEquals("abc123", extractResetToken("HTTPS://brandora-backend-production-85ec.up.railway.app/auth/reset-password#token=abc123"))
    }

    @Test
    fun `a percent-encoded token is decoded`() {
        assertEquals("a b+c", extractResetToken("$valid#token=a%20b%2Bc"))
    }

    @Test
    fun `a '+' in the fragment stays a plus, never a space`() {
        // A URI fragment is not a form body: URLDecoder would corrupt this.
        assertEquals("a+b", extractResetToken("$valid#token=a+b"))
    }

    @Test
    fun `a token value may contain '=' (padding survives)`() {
        assertEquals("ab=", extractResetToken("$valid#token=ab="))
    }

    @Test
    fun `an http link is rejected`() {
        assertNull(extractResetToken("http://brandora-backend-production-85ec.up.railway.app/auth/reset-password#token=abc"))
    }

    @Test
    fun `a different host is rejected`() {
        assertNull(extractResetToken("https://example.com/auth/reset-password#token=abc"))
    }

    @Test
    fun `a lookalike host suffix is rejected`() {
        assertNull(extractResetToken("https://evil-brandora-backend-production-85ec.up.railway.app/auth/reset-password#token=abc"))
        assertNull(extractResetToken("https://brandora-backend-production-85ec.up.railway.app.evil.com/auth/reset-password#token=abc"))
    }

    @Test
    fun `userinfo before the host is rejected (authority must match exactly)`() {
        assertNull(extractResetToken("https://evil.com@brandora-backend-production-85ec.up.railway.app/auth/reset-password#token=abc"))
    }

    @Test
    fun `an explicit port is rejected (authority must match exactly)`() {
        assertNull(extractResetToken("https://brandora-backend-production-85ec.up.railway.app:443/auth/reset-password#token=abc"))
    }

    @Test
    fun `a different path is rejected`() {
        assertNull(extractResetToken("$valid X#token=abc"))
        assertNull(extractResetToken("https://brandora-backend-production-85ec.up.railway.app/auth/reset-passwordx#token=abc"))
        assertNull(extractResetToken("https://brandora-backend-production-85ec.up.railway.app/auth/other#token=abc"))
        assertNull(extractResetToken("https://brandora-backend-production-85ec.up.railway.app#token=abc"))
    }

    @Test
    fun `a duplicated token parameter rejects the whole link`() {
        assertNull(extractResetToken("$valid#token=first&token=second"))
        assertNull(extractResetToken("$valid#foo=1&token=first&token=second"))
    }

    @Test
    fun `an empty token is rejected`() {
        assertNull(extractResetToken("$valid#token="))
        assertNull(extractResetToken("$valid#token=&token="))
    }

    @Test
    fun `no fragment at all is rejected`() {
        assertNull(extractResetToken(valid))
        assertNull(extractResetToken(""))
    }

    @Test
    fun `a fragment without a token parameter is rejected`() {
        assertNull(extractResetToken("$valid#foo=bar"))
        assertNull(extractResetToken("$valid#Token=abc")) // parameter names are exact
    }

    @Test
    fun `a malformed percent sequence rejects the link`() {
        assertNull(extractResetToken("$valid#token=%"))
        assertNull(extractResetToken("$valid#token=%A"))
        assertNull(extractResetToken("$valid#token=%ZZ"))
    }

    @Test
    fun `a query on the base is ignored - only the fragment can carry the token`() {
        assertEquals("good", extractResetToken("$valid?token=evil#token=good"))
    }

    @Test
    fun `garbage input never crashes and never yields a token`() {
        assertNull(extractResetToken("not a url"))
        assertNull(extractResetToken("://"))
        assertNull(extractResetToken("https://"))
        assertNull(extractResetToken("#"))
        assertNull(extractResetToken("https://brandora-backend-production-85ec.up.railway.app/auth/reset-password#"))
    }
}
