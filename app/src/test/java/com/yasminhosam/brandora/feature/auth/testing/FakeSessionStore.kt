package com.yasminhosam.brandora.feature.auth.testing

import com.yasminhosam.brandora.feature.auth.domain.model.AuthSession
import com.yasminhosam.brandora.feature.auth.domain.repository.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory SessionStore for JVM tests; counts saves/clears so rules can be asserted. */
class FakeSessionStore(initial: AuthSession? = null) : SessionStore {

    private val _session = MutableStateFlow(initial)
    override val session: StateFlow<AuthSession?> = _session.asStateFlow()

    var saves = 0
        private set

    var clears = 0
        private set

    override suspend fun save(session: AuthSession) {
        saves++
        _session.value = session
    }

    override suspend fun clear() {
        clears++
        _session.value = null
    }
}
