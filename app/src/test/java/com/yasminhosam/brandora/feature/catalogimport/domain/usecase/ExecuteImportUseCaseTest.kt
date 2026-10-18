package com.yasminhosam.brandora.feature.catalogimport.domain.usecase

import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.core.network.ImportErrorCodes
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.MappingPayload
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ExcelFile
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ExecuteCounts
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ExecuteOutcome
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ExecuteResult
import com.yasminhosam.brandora.feature.catalogimport.domain.model.FailedRow
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ImportAnalysis
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ImportJobStatus
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ImportStatus
import com.yasminhosam.brandora.feature.catalogimport.domain.model.PreviewResult
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ValidationResult
import com.yasminhosam.brandora.feature.catalogimport.domain.repository.ExcelImportRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExecuteImportUseCaseTest {

    private class FakeRepository(
        private val execute: ApiResult<ExecuteResult>,
        private val status: ApiResult<ImportJobStatus> = ApiResult.Success(JOB_STATUS)
    ) : ExcelImportRepository {
        var executeCalls = 0
            private set
        var statusCalls = 0
            private set

        override suspend fun analyze(file: ExcelFile, sheetName: String?): ApiResult<ImportAnalysis> =
            ApiResult.Error(ApiErrorType.UNKNOWN)

        override suspend fun getMapping(importId: String): ApiResult<MappingPayload> =
            ApiResult.Error(ApiErrorType.UNKNOWN)

        override suspend fun confirmMapping(importId: String, columns: List<com.yasminhosam.brandora.feature.catalogimport.domain.mapping.MappedColumn>): ApiResult<Unit> =
            ApiResult.Error(ApiErrorType.UNKNOWN)

        override suspend fun validate(importId: String): ApiResult<ValidationResult> =
            ApiResult.Error(ApiErrorType.UNKNOWN)

        override suspend fun getValidationResults(importId: String, limit: Int, offset: Int): ApiResult<ValidationResult> =
            ApiResult.Error(ApiErrorType.UNKNOWN)

        override suspend fun getPreview(importId: String, limit: Int, offset: Int): ApiResult<PreviewResult> =
            ApiResult.Error(ApiErrorType.UNKNOWN)

        override suspend fun execute(importId: String): ApiResult<ExecuteResult> {
            executeCalls++
            return execute
        }

        override suspend fun getImportStatus(importId: String): ApiResult<ImportJobStatus> {
            statusCalls++
            return status
        }
    }

    @Test
    fun `success returns the performed outcome with row detail`() = runBlocking {
        val result = ExecuteImportUseCase(FakeRepository(ApiResult.Success(EXECUTE_RESULT)))("job-1")

        val outcome = (result as ApiResult.Success).data as ExecuteOutcome.Performed
        assertEquals(ImportStatus.COMPLETED_WITH_ERRORS, outcome.result.status)
        assertEquals(1, outcome.result.counts.inserted)
        assertEquals(1, outcome.result.failedRows.size)
    }

    @Test
    fun `409 IMPORT_NOT_READY re-reads status instead of re-posting`() = runBlocking {
        val error = ApiResult.Error(ApiErrorType.CONFLICT, 409, null, ImportErrorCodes.IMPORT_NOT_READY)

        val result = ExecuteImportUseCase(FakeRepository(error))("job-1")

        val outcome = (result as ApiResult.Success).data as ExecuteOutcome.Recovered
        assertEquals(ImportStatus.COMPLETED, outcome.status.status)
    }

    @Test
    fun `500 IMPORT_EXECUTION_FAILED re-reads status`() = runBlocking {
        val error = ApiResult.Error(ApiErrorType.SERVER, 500, null, ImportErrorCodes.IMPORT_EXECUTION_FAILED)

        val result = ExecuteImportUseCase(FakeRepository(error))("job-1")

        assertTrue((result as ApiResult.Success).data is ExecuteOutcome.Recovered)
    }

    @Test
    fun `a timeout re-reads status because rows may have committed`() = runBlocking {
        val repository = FakeRepository(ApiResult.Error(ApiErrorType.TIMEOUT))

        val result = ExecuteImportUseCase(repository)("job-1")

        assertEquals(1, repository.statusCalls)
        assertTrue((result as ApiResult.Success).data is ExecuteOutcome.Recovered)
    }

    @Test
    fun `404 IMPORT_JOB_NOT_FOUND surfaces as an error without a status read`() = runBlocking {
        val repository = FakeRepository(
            ApiResult.Error(ApiErrorType.NOT_FOUND, 404, null, ImportErrorCodes.IMPORT_JOB_NOT_FOUND)
        )

        val result = ExecuteImportUseCase(repository)("job-1")

        assertEquals(ApiErrorType.NOT_FOUND, (result as ApiResult.Error).type)
        assertEquals(0, repository.statusCalls)
    }

    @Test
    fun `a blank importId never touches the network`() = runBlocking {
        val repository = FakeRepository(ApiResult.Success(EXECUTE_RESULT))

        val result = ExecuteImportUseCase(repository)("  ")

        assertEquals(ApiErrorType.BAD_REQUEST, (result as ApiResult.Error).type)
        assertEquals(0, repository.executeCalls)
        assertEquals(0, repository.statusCalls)
    }

    private companion object {
        val EXECUTE_RESULT = ExecuteResult(
            importId = "job-1",
            status = ImportStatus.COMPLETED_WITH_ERRORS,
            counts = ExecuteCounts(inserted = 1, updated = 0, unchanged = 0, failed = 0),
            invalidRows = emptyList(),
            failedRows = listOf(FailedRow(rowNumber = 6, code = "EXECUTION_CONFLICT"))
        )

        val JOB_STATUS = ImportJobStatus(
            importId = "job-1",
            status = ImportStatus.COMPLETED,
            totalRows = 4,
            validRows = 4,
            invalidRows = 0,
            skippedRows = 0,
            insertedCount = 2,
            updatedCount = 1,
            failedCount = 0
        )
    }
}
