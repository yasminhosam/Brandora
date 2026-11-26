package com.yasminhosam.brandora.feature.catalogimport.presentation.preview

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
import com.yasminhosam.brandora.feature.catalogimport.domain.model.PreviewAction
import com.yasminhosam.brandora.feature.catalogimport.domain.model.PreviewItem
import com.yasminhosam.brandora.feature.catalogimport.domain.model.PreviewResult
import com.yasminhosam.brandora.feature.catalogimport.domain.model.PreviewSummary
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ValidationResult
import com.yasminhosam.brandora.feature.catalogimport.domain.repository.ExcelImportRepository
import com.yasminhosam.brandora.feature.catalogimport.domain.usecase.GetPreviewUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PreviewViewModelTest {

    private class FakeRepository : ExcelImportRepository {
        override suspend fun getPreview(
            importId: String,
            limit: Int,
            offset: Int,
        ): ApiResult<PreviewResult> = ApiResult.Success(PREVIEW_RESULT)

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

    private fun model() =
        PreviewViewModel(
            getPreview = GetPreviewUseCase(FakeRepository()),
            savedStateHandle = SavedStateHandle(mapOf(ApiParams.PATH_IMPORT_ID to "job-1")),
        )

    @Test
    fun `double import tap sends one navigation`() = runTest {
        val viewModel = model()
        val effects = mutableListOf<PreviewEffect>()
        val collection = launch(mainDispatcher) { viewModel.effects.collect { effects += it } }

        viewModel.onEvent(PreviewEvent.ImportClicked)
        viewModel.onEvent(PreviewEvent.ImportClicked)

        // A second push would stack a second Result screen, which re-executes the import.
        assertEquals(listOf(PreviewEffect.NavigateToExecute("job-1")), effects)
        collection.cancel()
    }

    @Test
    fun `double back sends one pop`() = runTest {
        val viewModel = model()
        val effects = mutableListOf<PreviewEffect>()
        val collection = launch(mainDispatcher) { viewModel.effects.collect { effects += it } }

        viewModel.onEvent(PreviewEvent.BackToValidation)
        viewModel.onEvent(PreviewEvent.BackToValidation)

        // A second pop would skip past the Validate step back to Upload.
        assertEquals(listOf(PreviewEffect.NavigateBack), effects)
        collection.cancel()
    }

    private companion object {
        val PREVIEW_RESULT =
            PreviewResult(
                importId = "job-1",
                status = ImportStatus.READY,
                summary =
                    PreviewSummary(
                        totalRows = 1,
                        validRows = 1,
                        invalidRows = 0,
                        skippedRows = 0,
                        willInsert = 1,
                        willUpdate = 0,
                        unchanged = 0,
                    ),
                page = PageInfo(totalCount = 1, hasMore = false, nextOffset = null),
                items =
                    listOf(
                        PreviewItem(rowNumber = 2, action = PreviewAction.INSERT, name = "Shirt")
                    ),
            )
    }
}
