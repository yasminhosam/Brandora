package com.yasminhosam.brandora.feature.inbox.domain.model

/**
 * Reply rules from the backend contract. The limit is counted in Unicode code points, the same
 * count WhatsApp uses: an Arabic letter is 1, an emoji is 1, and an emoji with a skin tone is 2,
 * so [String.length] (UTF-16 units) is not the right measure.
 */
object ChatRules {

    const val MAX_MESSAGE_CODE_POINTS = 4096

    /** Blank replies are not sendable; the composer keeps them disabled instead of failing. */
    fun isSendable(text: String): Boolean = text.isNotBlank() && !exceedsLimit(text)

    fun exceedsLimit(text: String): Boolean {
        val trimmed = text.trim()
        return trimmed.codePointCount(0, trimmed.length) > MAX_MESSAGE_CODE_POINTS
    }
}
