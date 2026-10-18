package com.yasminhosam.brandora.feature.catalogimport.domain.usecase

import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.ColumnSuggestion
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.MappedColumn
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.MappingChoice
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.MappingPayload
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.SavedMapping
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.StandardTarget
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ExcelFile
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ImportAnalysis
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ExecuteResult
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ImportJobStatus
import com.yasminhosam.brandora.feature.catalogimport.domain.model.PreviewResult
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ValidationResult
import com.yasminhosam.brandora.feature.catalogimport.domain.repository.ExcelImportRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MappingUseCaseTest {

    private class FakeRepository(
        private val payload: ApiResult<MappingPayload> = PAYLOAD,
        private val confirm: ApiResult<Unit> = ApiResult.Success(Unit)
    ) : ExcelImportRepository {
        var getCalls = 0
            private set
        var confirmCalls = 0
            private set
        var lastColumns: List<MappedColumn>? = null
            private set

        override suspend fun analyze(file: ExcelFile, sheetName: String?): ApiResult<ImportAnalysis> =
            ApiResult.Error(ApiErrorType.UNKNOWN)

        override suspend fun getMapping(importId: String): ApiResult<MappingPayload> {
            getCalls++
            return payload
        }

        override suspend fun confirmMapping(importId: String, columns: List<MappedColumn>): ApiResult<Unit> {
            confirmCalls++
            lastColumns = columns
            return confirm
        }

        override suspend fun validate(importId: String): ApiResult<ValidationResult> =
            ApiResult.Error(ApiErrorType.UNKNOWN)

        override suspend fun getValidationResults(importId: String, limit: Int, offset: Int): ApiResult<ValidationResult> =
            ApiResult.Error(ApiErrorType.UNKNOWN)

        override suspend fun getPreview(importId: String, limit: Int, offset: Int): ApiResult<PreviewResult> =
            ApiResult.Error(ApiErrorType.UNKNOWN)

        override suspend fun execute(importId: String): ApiResult<ExecuteResult> =
            ApiResult.Error(ApiErrorType.UNKNOWN)

        override suspend fun getImportStatus(importId: String): ApiResult<ImportJobStatus> =
            ApiResult.Error(ApiErrorType.UNKNOWN)
    }

    private fun column(choice: MappingChoice = MappingChoice.Standard(StandardTarget.NAME)) =
        MappedColumn("Item Name", 1, listOf("Shirt"), choice)

    @Test
    fun `get merges suggestions into columns`() = runBlocking {
        val result = GetMappingUseCase(FakeRepository())("job-1")

        val columns = (result as ApiResult.Success).data
        assertEquals(MappingChoice.Standard(StandardTarget.NAME), columns.single().choice)
    }

    @Test
    fun `get with a blank importId never touches the network`() = runBlocking {
        val repository = FakeRepository()
        val result = GetMappingUseCase(repository)("  ")

        assertEquals(ApiErrorType.BAD_REQUEST, (result as ApiResult.Error).type)
        assertEquals(0, repository.getCalls)
    }

    @Test
    fun `get passes repository errors through`() = runBlocking {
        val failure = ApiResult.Error(ApiErrorType.NETWORK)
        val result = GetMappingUseCase(FakeRepository(payload = failure))("job-1")

        assertEquals(ApiErrorType.NETWORK, (result as ApiResult.Error).type)
    }

    @Test
    fun `confirm sends valid columns`() = runBlocking {
        val repository = FakeRepository()
        val columns = listOf(column())

        val result = ConfirmMappingUseCase(repository)("job-1", columns)

        assertTrue(result is ApiResult.Success)
        assertEquals(1, repository.confirmCalls)
        assertEquals(columns, repository.lastColumns)
    }

    @Test
    fun `confirm re-validates and never sends an invalid mapping`() = runBlocking {
        val repository = FakeRepository()
        val columns = listOf(column(choice = MappingChoice.Ignore))

        val result = ConfirmMappingUseCase(repository)("job-1", columns)

        assertEquals(ApiErrorType.BAD_REQUEST, (result as ApiResult.Error).type)
        assertEquals(0, repository.confirmCalls)
    }

    @Test
    fun `confirm with a blank importId never touches the network`() = runBlocking {
        val repository = FakeRepository()
        val result = ConfirmMappingUseCase(repository)(" ", listOf(column()))

        assertEquals(ApiErrorType.BAD_REQUEST, (result as ApiResult.Error).type)
        assertEquals(0, repository.confirmCalls)
    }

    @Test
    fun `confirm passes repository errors through`() = runBlocking {
        val failure = ApiResult.Error(ApiErrorType.CONFLICT)
        val result = ConfirmMappingUseCase(FakeRepository(confirm = failure))("job-1", listOf(column()))

        assertEquals(ApiErrorType.CONFLICT, (result as ApiResult.Error).type)
    }

    private companion object {
        val PAYLOAD = ApiResult.Success(
            MappingPayload(
                columns = listOf(
                    ColumnSuggestion("Item Name", 1, listOf("Shirt"), "STANDARD", "name")
                ),
                savedMapping = null
            )
        )
    }
}
