package com.yasminhosam.brandora.feature.business.domain.model

enum class BusinessNameError { BLANK, TOO_LONG }

/**
 * Client-side mirror of the backend's name rules (required, trimmed, 1-200) so the form can
 * show an inline error before posting. Shared by the screen (UX) and the use case (gate).
 */
object BusinessRules {

    const val NAME_MAX_LENGTH = 200

    fun nameError(name: String): BusinessNameError? = when {
        name.isBlank() -> BusinessNameError.BLANK
        name.trim().length > NAME_MAX_LENGTH -> BusinessNameError.TOO_LONG
        else -> null
    }
}
