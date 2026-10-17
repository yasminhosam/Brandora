package com.yasminhosam.brandora.feature.catalogimport.presentation.upload

import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.catalogimport.domain.file.ExcelFileFactory
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.MappedColumn
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.MappingPayload
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ExcelFile
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ImportAnalysis
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ImportStatus
import com.yasminhosam.brandora.feature.catalogimport.domain.model.SampleRow
import com.yasminhosam.brandora.feature.catalogimport.domain.repository.ExcelImportRepository
import com.yasminhosam.brandora.feature.catalogimport.domain.usecase.AnalyzeExcelFileUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UploadViewModelTest {

    private class FakeExcelFileFactory(private val result: ExcelFile?) : ExcelFileFactory {
        var lastUri: String? = null
            private set

        override fun create(uri: String): ExcelFile? {
            lastUri = uri
            return result
        }
    }

    private class FakeRepository(
        private val result: ApiResult<ImportAnalysis>
    ) : ExcelImportRepository {
        var uploads = 0
            private set

        override suspend fun analyze(file: ExcelFile, sheetName: String?): ApiResult<ImportAnalysis> {
            uploads++
            return result
        }

        override suspend fun getMapping(importId: String): ApiResult<MappingPayload> =
            ApiResult.Error(ApiErrorType.UNKNOWN)

        override suspend fun confirmMapping(importId: String, columns: List<MappedColumn>): ApiResult<Unit> =
            ApiResult.Error(ApiErrorType.UNKNOWN)

    }

    private val mainDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun file(name: String = "products.xlsx", sizeBytes: Long = 1_024L) =
        ExcelFile(name, sizeBytes) { ByteArray(0).inputStream() }

    private fun viewModel(
        picked: ExcelFile?,
        result: ApiResult<ImportAnalysis> = SUCCESS
    ) = UploadViewModel(
        analyzeExcelFile = AnalyzeExcelFileUseCase(FakeRepository(result)),
        excelFileFactory = FakeExcelFileFactory(picked)
    )

    @Test
    fun `picking a valid file enables continue with no error`() = runTest {
        val viewModel = viewModel(picked = file())

        viewModel.onEvent(UploadEvent.FilePicked("content://media/1"))

        val state = viewModel.uiState.value
        assertEquals("products.xlsx", state.fileName)
        assertNull(state.visibleError)
        assertTrue(state.canContinue)
    }

    @Test
    fun `picking a non-xlsx file rejects it without keeping the file`() = runTest {
        val viewModel = viewModel(picked = file(name = "products.csv"))

        viewModel.onEvent(UploadEvent.FilePicked("content://media/1"))

        val state = viewModel.uiState.value
        assertNull(state.file)
        assertEquals(ApiErrorType.INVALID_FILE_TYPE, state.visibleError)
    }

    @Test
    fun `an unreadable document becomes a generic error, never a crash`() = runTest {
        val viewModel = viewModel(picked = null)

        viewModel.onEvent(UploadEvent.FilePicked("content://media/dead"))

        val state = viewModel.uiState.value
        assertNull(state.file)
        assertEquals(ApiErrorType.UNKNOWN, state.visibleError)
    }

    @Test
    fun `continue uploads and navigates with the job id`() = runTest {
        val viewModel = viewModel(picked = file())
        viewModel.onEvent(UploadEvent.FilePicked("content://media/1"))
        val effects = mutableListOf<UploadEffect>()
        val collection = launch(mainDispatcher) { viewModel.effects.collect { effects += it } }

        viewModel.onEvent(UploadEvent.ContinueClicked)

        val state = viewModel.uiState.value
        assertEquals(false, state.isAnalyzing)
        assertNull(state.uploadError)
        assertEquals(listOf(UploadEffect.NavigateToMapping("job-1")), effects)
        collection.cancel()
    }

    @Test
    fun `a failed upload keeps the file and leaves continue enabled as retry`() = runTest {
        val viewModel = viewModel(picked = file(), result = ApiResult.Error(ApiErrorType.NETWORK))
        viewModel.onEvent(UploadEvent.FilePicked("content://media/1"))
        val effects = mutableListOf<UploadEffect>()
        val collection = launch(mainDispatcher) { viewModel.effects.collect { effects += it } }

        viewModel.onEvent(UploadEvent.ContinueClicked)

        val state = viewModel.uiState.value
        assertEquals(false, state.isAnalyzing)
        assertEquals(ApiErrorType.NETWORK, state.uploadError)
        assertEquals("products.xlsx", state.fileName)
        assertTrue(state.canContinue)
        assertTrue(effects.isEmpty())
        collection.cancel()
    }

    @Test
    fun `continue with no file does nothing`() = runTest {
        val viewModel = viewModel(picked = file(name = "products.csv"))
        viewModel.onEvent(UploadEvent.FilePicked("content://media/1"))
        val effects = mutableListOf<UploadEffect>()
        val collection = launch(mainDispatcher) { viewModel.effects.collect { effects += it } }

        viewModel.onEvent(UploadEvent.ContinueClicked)

        assertTrue(effects.isEmpty())
        collection.cancel()
    }

    @Test
    fun `the bottom link navigates to manual entry`() = runTest {
        val viewModel = viewModel(picked = file())
        val effects = mutableListOf<UploadEffect>()
        val collection = launch(mainDispatcher) { viewModel.effects.collect { effects += it } }

        viewModel.onEvent(UploadEvent.ManualEntryClicked)

        assertEquals(listOf(UploadEffect.NavigateToManualEntry), effects)
        collection.cancel()
    }

    private companion object {
        val SUCCESS = ApiResult.Success(
            ImportAnalysis(
                importJobId = "job-1",
                status = ImportStatus.MAPPING_REQUIRED,
                sheetNames = listOf("Products"),
                selectedSheetName = "Products",
                headerRow = 1,
                headers = listOf("SKU", "Name"),
                sampleRows = listOf(SampleRow(2, listOf("SKU-1", "Shirt"))),
                totalRows = 7
            )
        )
    }
}
