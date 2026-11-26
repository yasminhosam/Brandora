package com.yasminhosam.brandora.feature.auth.presentation

import com.yasminhosam.brandora.core.network.ApiErrorType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The shared rate-limit countdown behind every auth screen: emits an [AuthCooldown] when it
 * starts, ticks it down once a second and emits null when it ends. The scope is the ViewModel
 * scope, so the countdown stops with the screen, and starting again cancels the previous run
 * so the latest Retry-After always wins.
 */
class AuthCooldownTicker(
    private val scope: CoroutineScope,
    private val onCooldown: (AuthCooldown?) -> Unit,
) {
    private var job: Job? = null

    /** Restarts the countdown: a running one is cancelled first, then [onCooldown] sees the new value. */
    fun start(seconds: Long, type: ApiErrorType) {
        job?.cancel()
        onCooldown(AuthCooldown(seconds, type))
        job = scope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1_000)
                remaining--
                onCooldown(AuthCooldown(remaining, type))
            }
            onCooldown(null)
        }
    }
}
