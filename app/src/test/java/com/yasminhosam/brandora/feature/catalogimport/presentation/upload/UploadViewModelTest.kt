package com.yasminhosam.brandora.feature.catalogimport.presentation.upload

import com.yasminhosam.brandora.R
import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.core.ui.messageRes
import com.yasminhosam.brandora.feature.catalogimport.domain.file.ExcelFileFactory
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.MappedColumn
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.MappingPayload
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ExcelFile
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ExecuteResult
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ImportAnalysis
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ImportJobStatus
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ImportStatus
import com.yasminhosam.brandora.feature.catalogimport.domain.model.PreviewResult
import com.yasminhosam.brandora.feature.catalogimport.domain.model.SampleRow
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ValidationResult
import com.yasminhosam.brandora.feature.catalogimport.domain.repository.ExcelImportRepository
import com.yasminhosam.brandora.feature.catalogimport.domain.usecase.AnalyzeExcelFileUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UploadViewModelTest {

    private class FakeExcelFileFactory(var result: ExcelFile?) : ExcelFileFactory {
        var lastUri: String? = null
            private set

        override fun create(uri: String): ExcelFile? {
            lastUri = uri
            return result
        }
    }

    private class FakeRepository(var result: ApiResult<ImportAnalysis>) : ExcelImportRepository {
        val requestedSheets = mutableListOf<String?>()
        /** When set, [analyze] parks here until the test completes it or the job is cancelled. */
        var gate: CompletableDeferred<Unit>? = null
        var uploads = 0
            private set

        override suspend fun analyze(
            file: ExcelFile,
            sheetName: String?,
        ): ApiResult<ImportAnalysis> {
            requestedSheets += sheetName
            uploads++
            gate?.await()
            return result
        }

        override suspend fun getMapping(importId: String): ApiResult<MappingPayload> =
            ApiResult.Error(ApiErrorType.UNKNOWN)

        override suspend fun confirmMapping(
            importId: String,
            columns: List<MappedColumn>,
        ): ApiResult<Unit> = ApiResult.Error(ApiErrorType.UNKNOWN)

        override suspend fun validate(importId: String): ApiResult<ValidationResult> =
            ApiResult.Error(ApiErrorType.UNKNOWN)

        override suspend fun getValidationResults(
            importId: String,
            limit: Int,
            offset: Int,
        ): ApiResult<ValidationResult> = ApiResult.Error(ApiErrorType.UNKNOWN)

        override suspend fun getPreview(
            importId: String,
            limit: Int,
            offset: Int,
        ): ApiResult<PreviewResult> = ApiResult.Error(ApiErrorType.UNKNOWN)

        override suspend fun execute(importId: String): ApiResult<ExecuteResult> =
            ApiResult.Error(ApiErrorType.UNKNOWN)

        override suspend fun getImportStatus(importId: String): ApiResult<ImportJobStatus> =
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
        result: ApiResult<ImportAnalysis> = SUCCESS,
    ) =
        UploadViewModel(
            analyzeExcelFile = AnalyzeExcelFileUseCase(FakeRepository(result)),
            excelFileFactory = FakeExcelFileFactory(picked),
        )

    @Test
    fun `picking a valid file enables continue with no error`() = runTest {
        val viewModel = viewModel(picked = file())

        viewModel.onEvent(UploadEvent.FilePicked("content://media/1"))

        val state = viewModel.uiState.value
        assertEquals("products.xlsx", state.fileName)
        assertNull(state.visibleErrorRes)
        assertTrue(state.canContinue)
    }

    @Test
    fun `picking a non-xlsx file rejects it without keeping the file`() = runTest {
        val viewModel = viewModel(picked = file(name = "products.csv"))

        viewModel.onEvent(UploadEvent.FilePicked("content://media/1"))

        val state = viewModel.uiState.value
        assertNull(state.file)
        assertEquals(R.string.error_invalid_file_type, state.visibleErrorRes)
    }

    @Test
    fun `an unreadable document becomes a generic error, never a crash`() = runTest {
        val viewModel = viewModel(picked = null)

        viewModel.onEvent(UploadEvent.FilePicked("content://media/dead"))

        val state = viewModel.uiState.value
        assertNull(state.file)
        assertEquals(R.string.error_unknown, state.visibleErrorRes)
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
        assertEquals(ApiErrorType.NETWORK, state.uploadError?.type)
        assertEquals("products.xlsx", state.fileName)
        assertTrue(state.canContinue)
        assertTrue(effects.isEmpty())
        collection.cancel()
    }

    @Test
    fun `an analyze failure keeps the backend code for a specific message`() = runTest {
        val viewModel =
            viewModel(
                picked = file(),
                result =
                    ApiResult.Error(ApiErrorType.NOT_FOUND, errorCode = "BUSINESS_NOT_FOUND"),
            )
        viewModel.onEvent(UploadEvent.FilePicked("content://media/1"))

        viewModel.onEvent(UploadEvent.ContinueClicked)

        val state = viewModel.uiState.value
        assertEquals(ApiErrorType.NOT_FOUND, state.uploadError?.type)
        assertEquals(R.string.error_business_not_found, state.uploadError?.messageRes())
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
    fun `skip leaves the import flow`() = runTest {
        val viewModel = viewModel(picked = file())
        val effects = mutableListOf<UploadEffect>()
        val collection = launch(mainDispatcher) { viewModel.effects.collect { effects += it } }

        viewModel.onEvent(UploadEvent.SkipClicked)

        assertEquals(listOf(UploadEffect.NavigateToNextStep), effects)
        collection.cancel()
    }

    @Test
    fun multipleSheetsWaitForExplicitChoice() = runTest {
        val multi = SUCCESS.data.copy(sheetNames = listOf("Products", "المخزون"))
        val repository = FakeRepository(ApiResult.Success(multi))
        val model =
            UploadViewModel(AnalyzeExcelFileUseCase(repository), FakeExcelFileFactory(file()))
        val effects = mutableListOf<UploadEffect>()
        val collection = launch(mainDispatcher) { model.effects.collect { effects += it } }
        model.onEvent(UploadEvent.FilePicked("content://test/book"))
        model.onEvent(UploadEvent.ContinueClicked)
        assertEquals("Products", model.uiState.value.chosenSheet)
        assertTrue(effects.isEmpty())
        model.onEvent(UploadEvent.ContinueClicked)
        assertEquals(1, repository.uploads)
        assertEquals(listOf(UploadEffect.NavigateToMapping("job-1")), effects)
        collection.cancel()
    }

    @Test
    fun anotherSheetReuploadsAndUsesNewJob() = runTest {
        val multi = SUCCESS.data.copy(sheetNames = listOf("Products", "المخزون"))
        val repository = FakeRepository(ApiResult.Success(multi))
        val model =
            UploadViewModel(AnalyzeExcelFileUseCase(repository), FakeExcelFileFactory(file()))
        val effects = mutableListOf<UploadEffect>()
        val collection = launch(mainDispatcher) { model.effects.collect { effects += it } }
        model.onEvent(UploadEvent.FilePicked("content://test/book"))
        model.onEvent(UploadEvent.ContinueClicked)
        model.onEvent(UploadEvent.SheetChosen("المخزون"))
        repository.result =
            ApiResult.Success(multi.copy(importJobId = "new-job", selectedSheetName = "المخزون"))
        model.onEvent(UploadEvent.ContinueClicked)
        assertEquals(listOf(null, "المخزون"), repository.requestedSheets)
        assertEquals(listOf(UploadEffect.NavigateToMapping("new-job")), effects)
        assertEquals("new-job", model.uiState.value.analysis?.importJobId)
        collection.cancel()
    }

    @Test
    fun changedFileClearsOldSheets() = runTest {
        val repository =
            FakeRepository(
                ApiResult.Success(SUCCESS.data.copy(sheetNames = listOf("Products", "Other")))
            )
        val model =
            UploadViewModel(AnalyzeExcelFileUseCase(repository), FakeExcelFileFactory(file()))
        model.onEvent(UploadEvent.FilePicked("content://test/book"))
        model.onEvent(UploadEvent.ContinueClicked)
        model.onEvent(UploadEvent.SheetChosen("unknown"))
        assertEquals("Products", model.uiState.value.chosenSheet)
        model.onEvent(UploadEvent.FilePicked("content://test/new"))
        assertNull(model.uiState.value.analysis)
        assertNull(model.uiState.value.chosenSheet)
    }

    @Test
    fun unnamedMultiSheetRequiresAnExplicitChoiceBeforeContinuing() = runTest {
        val multi =
            SUCCESS.data.copy(
                sheetNames = listOf("Products", "المخزون"),
                selectedSheetName = null, // analyzed the first sheet but named none
            )
        val repository = FakeRepository(ApiResult.Success(multi))
        val model =
            UploadViewModel(AnalyzeExcelFileUseCase(repository), FakeExcelFileFactory(file()))
        val effects = mutableListOf<UploadEffect>()
        val collection = launch(mainDispatcher) { model.effects.collect { effects += it } }
        model.onEvent(UploadEvent.FilePicked("content://test/book"))
        model.onEvent(UploadEvent.ContinueClicked)
        assertNull(model.uiState.value.chosenSheet)
        assertFalse(model.uiState.value.canContinue)
        assertTrue(effects.isEmpty())

        // Continue is disabled until a sheet is chosen, so no second analysis can start.
        model.onEvent(UploadEvent.ContinueClicked)
        assertEquals(1, repository.uploads)
        assertTrue(effects.isEmpty())

        model.onEvent(UploadEvent.SheetChosen("المخزون"))
        repository.result =
            ApiResult.Success(multi.copy(importJobId = "new-job", selectedSheetName = "المخزون"))
        assertTrue(model.uiState.value.canContinue)
        model.onEvent(UploadEvent.ContinueClicked)
        assertEquals(listOf(null, "المخزون"), repository.requestedSheets)
        assertEquals(listOf(UploadEffect.NavigateToMapping("new-job")), effects)
        collection.cancel()
    }

    @Test
    fun aNewFilePickedMidAnalysisWinsOverTheInFlightResult() = runTest {
        val repository = FakeRepository(SUCCESS)
        repository.gate = CompletableDeferred()
        val factory = FakeExcelFileFactory(file(name = "first.xlsx"))
        val model = UploadViewModel(AnalyzeExcelFileUseCase(repository), factory)

        model.onEvent(UploadEvent.FilePicked("content://test/first"))
        model.onEvent(UploadEvent.ContinueClicked)
        assertTrue(model.uiState.value.isAnalyzing)

        // A pick arriving mid-analysis must not be dropped on the floor: it replaces the
        // in-flight analysis instead of silently keeping the previous file.
        factory.result = file(name = "second.xlsx")
        model.onEvent(UploadEvent.FilePicked("content://test/second"))

        val state = model.uiState.value
        assertEquals("second.xlsx", state.fileName)
        assertFalse(state.isAnalyzing)
        assertNull(state.analysis)

        // Releasing the cancelled analysis must never resurrect its result.
        repository.gate?.complete(Unit)
        assertNull(model.uiState.value.analysis)
        assertEquals("second.xlsx", model.uiState.value.fileName)
    }

    private companion object {
        val SUCCESS =
            ApiResult.Success(
                ImportAnalysis(
                    importJobId = "job-1",
                    status = ImportStatus.MAPPING_REQUIRED,
                    sheetNames = listOf("Products"),
                    selectedSheetName = "Products",
                    headerRow = 1,
                    headers = listOf("SKU", "Name"),
                    sampleRows = listOf(SampleRow(2, listOf("SKU-1", "Shirt"))),
                    totalRows = 7,
                )
            )
    }
}
