package com.yasminhosam.brandora.feature.catalogimport.presentation.result

import androidx.lifecycle.SavedStateHandle
import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.core.network.ApiParams
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.MappedColumn
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.MappingPayload
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ExcelFile
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ExecuteCounts
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ExecuteResult
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ImportAnalysis
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ImportJobStatus
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ImportStatus
import com.yasminhosam.brandora.feature.catalogimport.domain.model.PreviewResult
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ValidationResult
import com.yasminhosam.brandora.feature.catalogimport.domain.repository.ExcelImportRepository
import com.yasminhosam.brandora.feature.catalogimport.domain.usecase.ExecuteImportUseCase
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ResultViewModelTest {

    private class FakeRepository : ExcelImportRepository {
        /** When set, [execute] parks here until the test completes it. */
        var executeGate: CompletableDeferred<Unit>? = null

        var executeCalls = 0
            private set

        override suspend fun execute(importId: String): ApiResult<ExecuteResult> {
            executeCalls++
            executeGate?.await()
            return ApiResult.Success(EXECUTE_RESULT)
        }

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
        ResultViewModel(
            executeImport = ExecuteImportUseCase(repository),
            savedStateHandle = SavedStateHandle(mapOf(ApiParams.PATH_IMPORT_ID to "job-1")),
        )

    @Test
    fun `retry while an execute is in flight sends a single request`() = runTest {
        val repository = FakeRepository()
        repository.executeGate = CompletableDeferred()
        val viewModel = model(repository)
        assertEquals(1, repository.executeCalls)

        viewModel.onEvent(ResultEvent.RetryClicked)
        viewModel.onEvent(ResultEvent.CheckStatusClicked)

        // The second tap must be swallowed: two concurrent executes would race and the loser
        // response would overwrite the winner's state.
        assertEquals(1, repository.executeCalls)

        repository.executeGate?.complete(Unit)
        mainDispatcher.scheduler.runCurrent()

        // The guard lifts once the execute settles: checking status works again.
        assertTrue(viewModel.uiState.value.isCompleted)
        viewModel.onEvent(ResultEvent.CheckStatusClicked)
        assertEquals(2, repository.executeCalls)
    }

    @Test
    fun `double done sends one effect`() = runTest {
        val viewModel = model()
        val effects = mutableListOf<ResultEffect>()
        val collection = launch(mainDispatcher) { viewModel.effects.collect { effects += it } }

        viewModel.onEvent(ResultEvent.DoneClicked)
        viewModel.onEvent(ResultEvent.DoneClicked)

        assertEquals(listOf(ResultEffect.NavigateToHome), effects)
        collection.cancel()
    }

    private companion object {
        val EXECUTE_RESULT =
            ExecuteResult(
                importId = "job-1",
                status = ImportStatus.COMPLETED,
                counts = ExecuteCounts(inserted = 1, updated = 0, unchanged = 0, failed = 0),
                invalidRows = emptyList(),
                failedRows = emptyList(),
            )
    }
}
