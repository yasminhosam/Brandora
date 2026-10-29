package com.yasminhosam.brandora.core.network

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Root-level "the session is gone" signal, emitted when the backend rejects the refresh
 * (AUTH_REFRESH_INVALID) or a refresh response is lost. BrandoraRoot collects it to route
 * to Login and show a snackbar, so it survives any back stack.
 *
 * Emission never suspends ([tryEmit]): callers run inside OkHttp/refresh paths.
 */
@Singleton
class SessionExpiredSignal @Inject constructor() {

    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events: SharedFlow<Unit> = _events.asSharedFlow()

    fun emit() {
        _events.tryEmit(Unit)
    }
}
