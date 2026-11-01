package com.yasminhosam.brandora.feature.auth.presentation

import com.yasminhosam.brandora.core.network.ApiErrorType

/**
 * A running rate-limit cooldown: submit stays disabled while [seconds] tick down, and the
 * screen shows the countdown copy picked by [type] (429 vs the 503-unavailable contract).
 */
data class AuthCooldown(
    val seconds: Long,
    val type: ApiErrorType
)
