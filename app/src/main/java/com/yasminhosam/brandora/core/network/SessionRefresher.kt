package com.yasminhosam.brandora.core.network

import com.yasminhosam.brandora.feature.auth.domain.model.AuthSession
import com.yasminhosam.brandora.feature.auth.domain.repository.SessionStore
import com.yasminhosam.brandora.feature.auth.domain.usecase.RefreshSessionUseCase
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The single-flight refresh gate. The refresh token is single-use on the backend (a reuse
 * revokes the whole session), so concurrent 401s must produce exactly ONE refresh call:
 *
 * - the first caller performs the refresh while holding the lock;
 * - everyone else waits on the lock, then sees a rotated access token and returns it
 *   without refreshing again;
 * - a caller whose token is already newer than the failed one skips the refresh entirely.
 *
 * Returns the usable access token, or null when the session is gone or the refresh failed
 * (the caller must fail the request; on AUTH_REFRESH_INVALID / lost response the repository
 * has already cleared the session and emitted SessionExpiredSignal).
 */
@Singleton
class SessionRefresher @Inject constructor(
    private val refreshSession: RefreshSessionUseCase,
    private val sessionStore: SessionStore
) {

    private val mutex = Mutex()

    /** [failedAccessToken] is the token the caller sent that just got a 401. */
    suspend fun validAccessToken(failedAccessToken: String?): String? = mutex.withLock {
        val latest: AuthSession = sessionStore.session.value ?: return@withLock null

        // Someone already refreshed while we waited for the lock (or the session moved on):
        // reuse the rotated token - the old refresh token must never be spent twice.
        if (failedAccessToken == null || latest.accessToken != failedAccessToken) {
            return@withLock latest.accessToken
        }

        when (val result = refreshSession()) {
            is ApiResult.Success -> result.data.accessToken
            is ApiResult.Error -> null
        }
    }
}
