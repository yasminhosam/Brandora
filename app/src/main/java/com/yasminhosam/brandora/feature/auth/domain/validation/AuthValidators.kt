package com.yasminhosam.brandora.feature.auth.domain.validation

/**
 * Pure, framework-free validation shared by the Login and Register screens: the ViewModels
 * run these BEFORE any request, so an invalid input never reaches the backend.
 */
enum class AuthEmailError { REQUIRED, TOO_LONG, INVALID, EMAIL_UNAVAILABLE }

enum class AuthPasswordError { REQUIRED, TOO_SHORT, TOO_LONG, MISSING_COMPLEXITY }

/** Backend limits: 254 for the email (RFC 5321 path cap), 12..128 for the register password. */
const val EMAIL_MAX_LENGTH = 254
const val PASSWORD_MIN_LENGTH = 12
const val PASSWORD_MAX_LENGTH = 128

/**
 * The identity sent to the backend: trim + lowercase. Kotlin's no-arg lowercase() is
 * locale-invariant (ROOT), so casing never depends on the device locale (Turkish dotless ı).
 */
fun normalizeEmail(raw: String): String = raw.trim().lowercase()

// Flat character classes only: linear backtracking even on hostile input. The domain must
// carry at least one dot (a@b alone is rejected); the backend is the final authority.
private val EMAIL_REGEX =
    Regex("[a-z0-9.!#\$%&'*+/=?^_`{|}~-]+@[a-z0-9]+(?:[.-][a-z0-9]+)*\\.[a-z]{1,63}")

/** Runs on the normalized email. Length fails before syntax so a huge input fails fast. */
fun validateEmail(normalized: String): AuthEmailError? = when {
    normalized.isBlank() -> AuthEmailError.REQUIRED
    normalized.length > EMAIL_MAX_LENGTH -> AuthEmailError.TOO_LONG
    normalized.any { it.code > 127 } -> AuthEmailError.INVALID
    !EMAIL_REGEX.matches(normalized) -> AuthEmailError.INVALID
    else -> null
}

/**
 * Register password: NEVER trimmed - leading/trailing spaces are real characters. Symbols and
 * spaces are allowed anywhere; one lowercase, one uppercase and one digit must all appear.
 */
fun validateRegisterPassword(password: String): AuthPasswordError? = when {
    password.isBlank() -> AuthPasswordError.REQUIRED
    password.length < PASSWORD_MIN_LENGTH -> AuthPasswordError.TOO_SHORT
    password.length > PASSWORD_MAX_LENGTH -> AuthPasswordError.TOO_LONG
    password.none { it.isLowerCase() } ||
        password.none { it.isUpperCase() } ||
        password.none { it.isDigit() } -> AuthPasswordError.MISSING_COMPLEXITY
    else -> null
}

/** Login password: length is the only rule (the backend owns the rest). */
fun validateLoginPassword(password: String): AuthPasswordError? = when {
    password.isBlank() -> AuthPasswordError.REQUIRED
    password.length > PASSWORD_MAX_LENGTH -> AuthPasswordError.TOO_LONG
    else -> null
}
