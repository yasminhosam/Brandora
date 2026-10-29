package com.yasminhosam.brandora.core.network

import com.yasminhosam.brandora.feature.auth.domain.model.AuthSession
import com.yasminhosam.brandora.feature.auth.domain.model.AuthUser
import com.yasminhosam.brandora.feature.auth.domain.repository.AuthRepository
import com.yasminhosam.brandora.feature.auth.domain.usecase.RefreshSessionUseCase
import com.yasminhosam.brandora.feature.auth.testing.FakeSessionStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The heart of the single-use refresh-token contract: parallel 401s produce exactly ONE
 * refresh, and a lost/failed refresh never hands the old token back to a caller.
 */
class SessionRefresherTest {

    private class FakeAuthRepository(
        private val onRefresh: suspend () -> ApiResult<AuthSession>
    ) : AuthRepository {
        var refreshCalls = 0
            private set

        override suspend fun login(email: String, password: String): ApiResult<AuthSession> =
            error("not under test")

        override suspend fun refresh(): ApiResult<AuthSession> {
            refreshCalls++
            return onRefresh()
        }
    }

    @Test
    fun `parallel 401s with the same stale token trigger exactly one refresh`() = runBlocking {
        val store = FakeSessionStore(OLD)
        val gate = CompletableDeferred<Unit>()
        val repository = FakeAuthRepository {
            // Slow refresh: every parallel caller must queue behind the single flight.
            gate.await()
            store.save(NEW)
            ApiResult.Success(NEW)
        }
        val refresher = SessionRefresher(RefreshSessionUseCase(repository), store)

        val callers = (1..5).map {
            async { refresher.validAccessToken(OLD.accessToken) }
        }
        // Let all five start: one owns the refresh, the rest queue on the single-flight lock.
        kotlinx.coroutines.yield()
        gate.complete(Unit)
        val tokens = callers.awaitAll()

        assertEquals(1, repository.refreshCalls)
        assertEquals(List(5) { NEW.accessToken }, tokens)
        assertEquals(NEW.refreshToken, store.session.value?.refreshToken)
    }

    @Test
    fun `a caller whose token already rotated skips the refresh entirely`() = runBlocking {
        val store = FakeSessionStore(NEW)
        val repository = FakeAuthRepository { error("must not refresh") }
        val refresher = SessionRefresher(RefreshSessionUseCase(repository), store)

        val token = refresher.validAccessToken(OLD.accessToken)

        assertEquals(NEW.accessToken, token)
        assertEquals(0, repository.refreshCalls)
    }

    @Test
    fun `the second waiter after a completed refresh uses the rotated token`() = runBlocking {
        val store = FakeSessionStore(OLD)
        val repository = FakeAuthRepository {
            store.save(NEW)
            ApiResult.Success(NEW)
        }
        val refresher = SessionRefresher(RefreshSessionUseCase(repository), store)

        val first = refresher.validAccessToken(OLD.accessToken)
        val second = refresher.validAccessToken(OLD.accessToken)

        assertEquals(NEW.accessToken, first)
        // The second caller presents the OLD token again (it raced the first one), but the
        // refresh already happened - no second spend of the single-use token.
        assertEquals(NEW.accessToken, second)
        assertEquals(1, repository.refreshCalls)
    }

    @Test
    fun `a failed refresh returns null so the 401 is delivered`() = runBlocking {
        val store = FakeSessionStore(OLD)
        val repository = FakeAuthRepository {
            ApiResult.Error(ApiErrorType.AUTH_RATE_LIMITED, retryAfterSeconds = 30)
        }
        val refresher = SessionRefresher(RefreshSessionUseCase(repository), store)

        assertNull(refresher.validAccessToken(OLD.accessToken))
        // The session is kept: a rate-limited refresh does not invalidate the tokens.
        assertEquals(OLD.accessToken, store.session.value?.accessToken)
        assertEquals(0, store.clears)
    }

    @Test
    fun `no session means no refresh attempt`() = runBlocking {
        val store = FakeSessionStore(null)
        val repository = FakeAuthRepository { error("must not refresh") }
        val refresher = SessionRefresher(RefreshSessionUseCase(repository), store)

        assertNull(refresher.validAccessToken(null))
        assertEquals(0, repository.refreshCalls)
    }

    private companion object {
        val OLD = AuthSession(
            user = AuthUser(id = "u1"),
            accessToken = "access-old",
            refreshToken = "refresh-old"
        )
        val NEW = AuthSession(
            user = AuthUser(id = "u1"),
            accessToken = "access-new",
            refreshToken = "refresh-new"
        )
    }
}
