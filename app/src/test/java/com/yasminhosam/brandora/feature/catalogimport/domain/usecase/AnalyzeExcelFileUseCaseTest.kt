package com.yasminhosam.brandora.feature.catalogimport.domain.usecase

import com.yasminhosam.brandora.core.network.ApiConfig
import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.MappedColumn
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.MappingPayload
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ExcelFile
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ImportAnalysis
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ImportStatus
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ExecuteResult
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ImportJobStatus
import com.yasminhosam.brandora.feature.catalogimport.domain.model.PreviewResult
import com.yasminhosam.brandora.feature.catalogimport.domain.model.SampleRow
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ValidationResult
import com.yasminhosam.brandora.feature.catalogimport.domain.repository.ExcelImportRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class AnalyzeExcelFileUseCaseTest {

    private class RecordingRepository(
        private val response: ApiResult<ImportAnalysis> = SUCCESS
    ) : ExcelImportRepository {

        var calls = 0
            private set

        var lastFile: ExcelFile? = null
            private set

        var lastSheetName: String? = null
            private set

        override suspend fun analyze(file: ExcelFile, sheetName: String?): ApiResult<ImportAnalysis> {
            calls++
            lastFile = file
            lastSheetName = sheetName
            return response
        }

        override suspend fun getMapping(importId: String): ApiResult<MappingPayload> =
            ApiResult.Error(ApiErrorType.UNKNOWN)

        override suspend fun confirmMapping(importId: String, columns: List<MappedColumn>): ApiResult<Unit> =
            ApiResult.Error(ApiErrorType.UNKNOWN)

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

    private fun file(
        name: String,
        sizeBytes: Long = 1_024L
    ) = ExcelFile(name, sizeBytes) { ByteArray(0).inputStream() }

    private fun errorTypeOf(result: ApiResult<ImportAnalysis>): ApiErrorType =
        (result as ApiResult.Error).type

    @Test
    fun `valid xlsx reaches the repository unchanged`() = runBlocking {
        val repository = RecordingRepository()
        val useCase = AnalyzeExcelFileUseCase(repository)
        val picked = file("catalog.xlsx")

        val result = useCase(picked)

        assertSame(SUCCESS, result)
        assertEquals(1, repository.calls)
        assertSame(picked, repository.lastFile)
        assertNull(repository.lastSheetName)
    }

    @Test
    fun `sheet name is forwarded to the repository`() = runBlocking {
        val repository = RecordingRepository()
        val useCase = AnalyzeExcelFileUseCase(repository)

        useCase(file("catalog.xlsx"), sheetName = "Products")

        assertEquals("Products", repository.lastSheetName)
    }

    @Test
    fun `xls extension is rejected without calling the repository`() = runBlocking {
        val repository = RecordingRepository()
        val useCase = AnalyzeExcelFileUseCase(repository)

        val result = useCase(file("catalog.xls"))

        assertEquals(ApiErrorType.INVALID_FILE_TYPE, errorTypeOf(result))
        assertEquals(0, repository.calls)
    }

    @Test
    fun `file without extension is rejected without calling the repository`() = runBlocking {
        val repository = RecordingRepository()
        val useCase = AnalyzeExcelFileUseCase(repository)

        val result = useCase(file("unknown"))

        assertEquals(ApiErrorType.INVALID_FILE_TYPE, errorTypeOf(result))
        assertEquals(0, repository.calls)
    }

    @Test
    fun `uppercase xlsx extension is accepted`() {
        assertNull(AnalyzeExcelFileUseCase(RecordingRepository()).validate(file("CATALOG.XLSX")))
    }

    @Test
    fun `exactly 10 MiB passes local validation`() {
        assertNull(
            AnalyzeExcelFileUseCase(RecordingRepository())
                .validate(file("catalog.xlsx", ApiConfig.MAX_UPLOAD_BYTES))
        )
    }

    @Test
    fun `10 MiB plus one byte fails with FILE_TOO_LARGE and never calls the repository`() = runBlocking {
        val repository = RecordingRepository()
        val useCase = AnalyzeExcelFileUseCase(repository)

        val result = useCase(file("catalog.xlsx", ApiConfig.MAX_UPLOAD_BYTES + 1))

        assertEquals(ApiErrorType.FILE_TOO_LARGE, errorTypeOf(result))
        assertEquals(0, repository.calls)
    }

    @Test
    fun `unknown size skips the local size check so the server 413 becomes the guard`() {
        assertNull(
            AnalyzeExcelFileUseCase(RecordingRepository())
                .validate(file("catalog.xlsx", ExcelFile.UNKNOWN_SIZE))
        )
    }

    @Test
    fun `repository errors are returned untouched`() = runBlocking {
        val failure = ApiResult.Error(ApiErrorType.NETWORK)
        val useCase = AnalyzeExcelFileUseCase(RecordingRepository(failure))

        assertEquals(ApiErrorType.NETWORK, errorTypeOf(useCase(file("catalog.xlsx"))))
    }

    private companion object {
        val SUCCESS = ApiResult.Success(
            ImportAnalysis(
                importJobId = "7dc471e1-0a82-44f3-9906-d7e0d0aefc39",
                status = ImportStatus.MAPPING_REQUIRED,
                sheetNames = listOf("Products"),
                selectedSheetName = "Products",
                headerRow = 1,
                headers = listOf("SKU", "Product Name", "Description", "Category", "Price", "Quantity", "Material"),
                sampleRows = listOf(
                    SampleRow(
                        rowNumber = 2,
                        values = listOf("SKU-NEW-001", "T-Shirt", "New product", "Shirts", "299.99", "10", "Cotton")
                    )
                ),
                totalRows = 7
            )
        )
    }
}
