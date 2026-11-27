package com.yasminhosam.brandora.feature.business.domain.usecase

import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.business.domain.model.BusinessProfile
import com.yasminhosam.brandora.feature.business.domain.model.NewBusiness
import com.yasminhosam.brandora.feature.business.domain.repository.BusinessRepository
import com.yasminhosam.brandora.feature.business.domain.repository.CurrentBusinessStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReconcileBusinessUseCaseTest {

    private class FakeBusinessRepository(
        var result: ApiResult<List<BusinessProfile>>
    ) : BusinessRepository {
        override suspend fun createBusiness(input: NewBusiness): ApiResult<BusinessProfile> =
            error("not under test")

        override suspend fun getBusinesses(limit: Int, offset: Int): ApiResult<List<BusinessProfile>> =
            result
    }

    private class FakeCurrentBusinessStore(initialId: String? = null) : CurrentBusinessStore {
        private val _businessId = MutableStateFlow(initialId)
        override val businessId: Flow<String?> = _businessId

        var savedId: String? = initialId
            private set
        var savedOwnerId: String? = null
            private set

        override suspend fun setBusinessId(id: String, ownerId: String?) {
            savedId = id
            savedOwnerId = ownerId
            _businessId.value = id
        }

        override suspend fun clearUnlessOwnedBy(userId: String) {}
    }

    @Test
    fun `server has business with empty local store saves business id and returns HasBusiness`() = runTest {
        val repo = FakeBusinessRepository(
            ApiResult.Success(listOf(BusinessProfile(id = "biz-server", name = "Server Biz")))
        )
        val store = FakeCurrentBusinessStore(initialId = null)
        val useCase = ReconcileBusinessUseCase(repo, store)

        val result = useCase("u1")

        assertEquals(BusinessSyncResult.HasBusiness, result)
        assertEquals("biz-server", store.savedId)
        assertEquals("u1", store.savedOwnerId)
    }

    @Test
    fun `server has no business returns NoBusiness`() = runTest {
        val repo = FakeBusinessRepository(ApiResult.Success(emptyList()))
        val store = FakeCurrentBusinessStore(initialId = null)
        val useCase = ReconcileBusinessUseCase(repo, store)

        val result = useCase("u1")

        assertEquals(BusinessSyncResult.NoBusiness, result)
    }

    @Test
    fun `error with a stored id returns HasBusiness`() = runTest {
        val repo = FakeBusinessRepository(ApiResult.Error(ApiErrorType.NETWORK))
        val store = FakeCurrentBusinessStore(initialId = "biz-stored")
        val useCase = ReconcileBusinessUseCase(repo, store)

        val result = useCase("u1")

        assertEquals(BusinessSyncResult.HasBusiness, result)
    }

    @Test
    fun `error without a stored id returns Error`() = runTest {
        val repo = FakeBusinessRepository(ApiResult.Error(ApiErrorType.NETWORK))
        val store = FakeCurrentBusinessStore(initialId = null)
        val useCase = ReconcileBusinessUseCase(repo, store)

        val result = useCase("u1")

        assertTrue(result is BusinessSyncResult.Error)
        assertEquals(ApiErrorType.NETWORK, (result as BusinessSyncResult.Error).error.type)
    }

    @Test
    fun `stale stored id is replaced with the first server business`() = runTest {
        val repo = FakeBusinessRepository(
            ApiResult.Success(listOf(BusinessProfile(id = "biz-new", name = "New Biz")))
        )
        val store = FakeCurrentBusinessStore(initialId = "biz-stale")
        val useCase = ReconcileBusinessUseCase(repo, store)

        val result = useCase("u1")

        assertEquals(BusinessSyncResult.HasBusiness, result)
        assertEquals("biz-new", store.savedId)
    }
}
