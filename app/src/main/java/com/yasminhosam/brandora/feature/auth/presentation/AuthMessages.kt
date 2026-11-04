package com.yasminhosam.brandora.feature.auth.presentation

import androidx.annotation.StringRes
import com.yasminhosam.brandora.R
import com.yasminhosam.brandora.feature.auth.domain.validation.AuthEmailError
import com.yasminhosam.brandora.feature.auth.domain.validation.AuthPasswordError

/**
 * Local-validation messages for both auth screens. Exhaustive without `else`, so a new
 * outcome fails compilation until it has copy in both languages.
 */
@StringRes
fun AuthEmailError.fieldMessageRes(): Int = when (this) {
    AuthEmailError.REQUIRED -> R.string.auth_field_required
    AuthEmailError.TOO_LONG -> R.string.auth_email_too_long
    AuthEmailError.INVALID -> R.string.auth_email_invalid
    AuthEmailError.EMAIL_UNAVAILABLE -> R.string.auth_email_unavailable
}

@StringRes
fun AuthPasswordError.fieldMessageRes(): Int = when (this) {
    AuthPasswordError.REQUIRED -> R.string.auth_field_required
    AuthPasswordError.TOO_SHORT -> R.string.auth_password_too_short
    AuthPasswordError.TOO_LONG -> R.string.auth_password_too_long
    AuthPasswordError.MISSING_COMPLEXITY -> R.string.auth_password_complexity
}
