package com.yasminhosam.brandora.feature.business.domain.usecase

import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.business.domain.model.BusinessProfile
import com.yasminhosam.brandora.feature.business.domain.model.NewBusiness
import com.yasminhosam.brandora.feature.business.domain.repository.BusinessRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateBusinessUseCaseTest {

    private class RecordingRepository(
        private val response: ApiResult<BusinessProfile> = SUCCESS
    ) : BusinessRepository {

        var calls = 0
            private set

        var lastInput: NewBusiness? = null
            private set

        override suspend fun createBusiness(input: NewBusiness): ApiResult<BusinessProfile> {
            calls++
            lastInput = input
            return response
        }
    }

    @Test
    fun `a blank name is rejected without calling the backend`() = runBlocking {
        val repository = RecordingRepository()

        val result = CreateBusinessUseCase(repository)(NewBusiness(name = "   "))

        assertTrue(result is ApiResult.Error)
        assertEquals(ApiErrorType.BAD_REQUEST, (result as ApiResult.Error).type)
        assertEquals(0, repository.calls)
    }

    @Test
    fun `a name over 200 characters is rejected`() = runBlocking {
        val repository = RecordingRepository()

        val result = CreateBusinessUseCase(repository)(NewBusiness(name = "a".repeat(201)))

        assertTrue(result is ApiResult.Error)
        assertEquals(0, repository.calls)
    }

    @Test
    fun `a valid name is trimmed before the request`() = runBlocking {
        val repository = RecordingRepository()

        val result = CreateBusinessUseCase(repository)(NewBusiness(name = "  متجر التمر  "))

        assertSame(SUCCESS, result)
        assertEquals("متجر التمر", repository.lastInput?.name)
    }

    @Test
    fun `optional fields pass through unchanged`() = runBlocking {
        val repository = RecordingRepository()

        CreateBusinessUseCase(repository)(
            NewBusiness(
                name = "متجر التمر",
                description = " تمور فاخرة ",
                category = "تمور",
                phone = "0500000000",
                address = "الرياض"
            )
        )

        assertEquals(" تمور فاخرة ", repository.lastInput?.description)
        assertEquals("تمور", repository.lastInput?.category)
        assertEquals("0500000000", repository.lastInput?.phone)
        assertEquals("الرياض", repository.lastInput?.address)
    }

    private companion object {
        val SUCCESS = ApiResult.Success(
            BusinessProfile(
                id = "f6bc7287-3285-4e19-a819-459c41f0a138",
                name = "متجر التمر",
                description = null,
                category = null,
                phone = null,
                address = null
            )
        )
    }
}
