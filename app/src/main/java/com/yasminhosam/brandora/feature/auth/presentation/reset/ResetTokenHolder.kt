package com.yasminhosam.brandora.feature.auth.presentation.reset

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory hand-off of the reset token from an app-link intent to the Reset screen.
 * The token is never persisted, never placed on a navigation route, and never logged:
 * it lives in this singleton until the Reset screen consumes it (and is dropped from the
 * ViewModel state on success or when the screen leaves the stack).
 *
 * Two collectors watch [token]: AppNavHost opens the Reset screen when a link arrives
 * while any other screen is showing, and the Reset screen itself consumes the value -
 * a newer link arriving while the screen is open replaces the token, matching the
 * backend rule that the latest request supersedes the previous one.
 */
@Singleton
class ResetTokenHolder @Inject constructor() {
    private val _token = MutableStateFlow<String?>(null)

    val token: StateFlow<String?> = _token.asStateFlow()

    /** Called from MainActivity with a token that already passed extractResetToken(). */
    fun submit(token: String) {
        _token.value = token
    }

    /** Called by the Reset screen once the value has been handed to the ViewModel. */
    fun consume() {
        _token.value = null
    }
}
