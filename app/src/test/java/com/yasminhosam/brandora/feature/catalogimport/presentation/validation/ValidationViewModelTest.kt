package com.yasminhosam.brandora.feature.catalogimport.presentation.validation

import androidx.lifecycle.SavedStateHandle
import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.core.network.ApiParams
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.MappedColumn
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.MappingPayload
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ExcelFile
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ExecuteResult
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ImportAnalysis
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ImportJobStatus
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ImportStatus
import com.yasminhosam.brandora.feature.catalogimport.domain.model.PageInfo
import com.yasminhosam.brandora.feature.catalogimport.domain.model.PreviewResult
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ValidationRow
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ValidationResult
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ValidationSummary
import com.yasminhosam.brandora.feature.catalogimport.domain.repository.ExcelImportRepository
import com.yasminhosam.brandora.feature.catalogimport.domain.usecase.GetValidationResultsUseCase
import com.yasminhosam.brandora.feature.catalogimport.domain.usecase.StartValidationUseCase
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ValidationViewModelTest {

    private class FakeRepository : ExcelImportRepository {
        var validateResult: ApiResult<ValidationResult> = ApiResult.Success(READY_RESULT)
        var resultsResult: ApiResult<ValidationResult> = ApiResult.Success(PAGE_RESULT)
        /** When set, [validate] parks here until the test completes it. */
        var validateGate: CompletableDeferred<Unit>? = null

        var validateCalls = 0
            private set

        override suspend fun validate(importId: String): ApiResult<ValidationResult> {
            validateCalls++
            validateGate?.await()
            return validateResult
        }

        override suspend fun getValidationResults(
            importId: String,
            limit: Int,
            offset: Int,
        ): ApiResult<ValidationResult> = resultsResult

        override suspend fun analyze(
            file: ExcelFile,
            sheetName: String?,
        ): ApiResult<ImportAnalysis> = ApiResult.Error(ApiErrorType.UNKNOWN)

        override suspend fun getMapping(importId: String): ApiResult<MappingPayload> =
            ApiResult.Error(ApiErrorType.UNKNOWN)

        override suspend fun confirmMapping(
            importId: String,
            columns: List<MappedColumn>,
        ): ApiResult<Unit> = ApiResult.Error(ApiErrorType.UNKNOWN)

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

    private fun model(repository: FakeRepository = FakeRepository()) =
        ValidationViewModel(
            startValidation = StartValidationUseCase(repository),
            getValidationResults = GetValidationResultsUseCase(repository),
            savedStateHandle = SavedStateHandle(mapOf(ApiParams.PATH_IMPORT_ID to "job-1")),
        )

    @Test
    fun `double continue sends one navigation and re-arms on re-entry`() = runTest {
        val viewModel = model()
        val effects = mutableListOf<ValidationEffect>()
        val collection = launch(mainDispatcher) { viewModel.effects.collect { effects += it } }

        viewModel.onEvent(ValidationEvent.ContinueClicked)
        viewModel.onEvent(ValidationEvent.ContinueClicked)

        assertEquals(listOf(ValidationEffect.NavigateToPreview), effects)

        // Coming back from Preview must allow navigating forward again.
        viewModel.onEvent(ValidationEvent.ScreenEntered)
        viewModel.onEvent(ValidationEvent.ContinueClicked)

        assertEquals(2, effects.size)
        collection.cancel()
    }

    @Test
    fun `retry while a start is in flight sends a single request`() = runTest {
        val repository = FakeRepository()
        repository.validateResult = ApiResult.Error(ApiErrorType.NETWORK)
        val viewModel = model(repository)
        assertEquals(1, repository.validateCalls)

        repository.validateGate = CompletableDeferred()
        viewModel.onEvent(ValidationEvent.RetryClicked)
        viewModel.onEvent(ValidationEvent.RetryClicked)

        // The second tap must be swallowed while the first POST is still open (a repeat 409s).
        assertEquals(2, repository.validateCalls)

        repository.validateGate?.complete(Unit)
        mainDispatcher.scheduler.runCurrent()

        // The in-flight flag is released once the call settles: retrying works again.
        viewModel.onEvent(ValidationEvent.RetryClicked)
        assertEquals(3, repository.validateCalls)
    }

    @Test
    fun `retry after a first page failure clears the error and loads rows`() = runTest {
        val repository = FakeRepository()
        repository.resultsResult = ApiResult.Error(ApiErrorType.NETWORK)
        val viewModel = model(repository)
        assertNotNull(viewModel.uiState.value.errorRes)

        repository.resultsResult = ApiResult.Success(PAGE_RESULT)
        viewModel.onEvent(ValidationEvent.RetryClicked)

        assertNull(viewModel.uiState.value.errorRes)
        assertEquals(1, viewModel.uiState.value.rows.size)
    }

    private companion object {
        val SUMMARY =
            ValidationSummary(totalRows = 1, validRows = 1, invalidRows = 0, skippedRows = 0)

        val READY_RESULT =
            ValidationResult(
                importId = "job-1",
                status = ImportStatus.READY,
                summary = SUMMARY,
                rows = emptyList(),
                page = PageInfo(totalCount = 1, hasMore = false, nextOffset = null),
            )

        val PAGE_RESULT =
            ValidationResult(
                importId = "job-1",
                status = ImportStatus.READY,
                summary = SUMMARY,
                rows = listOf(ValidationRow(rowNumber = 2, isValid = true, productName = "Shirt")),
                page = PageInfo(totalCount = 1, hasMore = false, nextOffset = null),
            )
    }
}
