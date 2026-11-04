package com.yasminhosam.brandora.feature.auth.domain.repository

import com.yasminhosam.brandora.feature.auth.domain.model.AuthSession
import kotlinx.coroutines.flow.StateFlow

/**
 * The authenticated session, stored encrypted on disk and mirrored in memory. Interceptors
 * read [session].value synchronously, so the store is loaded once before the first request.
 * Null before login and after any form of clearing.
 */
interface SessionStore {
    val session: StateFlow<AuthSession?>

    /** Persists the session (encrypted) and updates [session] before returning. */
    suspend fun save(session: AuthSession)

    /** Removes the stored session; safe to call when nothing is stored. */
    suspend fun clear()
}
