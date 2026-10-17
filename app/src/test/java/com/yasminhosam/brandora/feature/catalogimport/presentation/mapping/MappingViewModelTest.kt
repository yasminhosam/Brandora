package com.yasminhosam.brandora.feature.catalogimport.presentation.mapping

import androidx.lifecycle.SavedStateHandle
import com.yasminhosam.brandora.R
import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.core.network.ApiParams
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.ColumnSuggestion
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.MappedColumn
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.MappingChoice
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.MappingInvalidReason
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.MappingPayload
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.SavedMapping
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.StandardTarget
import com.yasminhosam.brandora.feature.catalogimport.domain.repository.ExcelImportRepository
import com.yasminhosam.brandora.feature.catalogimport.domain.usecase.ConfirmMappingUseCase
import com.yasminhosam.brandora.feature.catalogimport.domain.usecase.GetMappingUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MappingViewModelTest {

    private class FakeRepository(
        private var payload: ApiResult<MappingPayload> = PAYLOAD,
        private var confirm: ApiResult<Unit> = ApiResult.Success(Unit),
        private val confirmGate: CompletableDeferred<Unit>? = null
    ) : ExcelImportRepository {
        var getCalls = 0
            private set
        var confirmCalls = 0
            private set

        override suspend fun analyze(
            file: com.yasminhosam.brandora.feature.catalogimport.domain.model.ExcelFile,
            sheetName: String?
        ): ApiResult<com.yasminhosam.brandora.feature.catalogimport.domain.model.ImportAnalysis> =
            ApiResult.Error(ApiErrorType.UNKNOWN)

        override suspend fun getMapping(importId: String): ApiResult<MappingPayload> {
            getCalls++
            return payload
        }

        override suspend fun confirmMapping(importId: String, columns: List<MappedColumn>): ApiResult<Unit> {
            confirmCalls++
            confirmGate?.await()
            return confirm
        }

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

    private fun viewModel(repository: FakeRepository) = MappingViewModel(
        getMapping = GetMappingUseCase(repository),
        confirmMapping = ConfirmMappingUseCase(repository),
        savedStateHandle = SavedStateHandle(mapOf(ApiParams.PATH_IMPORT_ID to "job-1"))
    )

    private fun CoroutineScope.collect(viewModel: MappingViewModel): Pair<MutableList<MappingEffect>, Job> {
        val effects = mutableListOf<MappingEffect>()
        val job = launch(mainDispatcher) { viewModel.effects.collect { effects += it } }
        return effects to job
    }

    @Test
    fun `init loads suggestions with name mapped and confirm enabled`() = runTest {
        val state = viewModel(FakeRepository()).uiState.value

        assertEquals(false, state.isLoading)
        assertEquals(2, state.columns.size)
        assertEquals(MappingChoice.Standard(StandardTarget.NAME), state.columns[0].choice)
        assertTrue(state.isNameMapped)
        assertTrue(state.canConfirm)
    }

    @Test
    fun `init prefers a saved mapping over suggestions`() = runTest {
        val repository = FakeRepository(
            payload = ApiResult.Success(
                MappingPayload(
                    columns = listOf(
                        ColumnSuggestion("Item Name", 1, emptyList(), "STANDARD", "name")
                    ),
                    savedMapping = listOf(SavedMapping("Item Name", 1, "IGNORE", null))
                )
            )
        )

        val state = viewModel(repository).uiState.value

        assertEquals(MappingChoice.Ignore, state.columns.single().choice)
    }

    @Test
    fun `unmapping the name column disables confirm`() = runTest {
        val viewModel = viewModel(FakeRepository())
        viewModel.onEvent(MappingEvent.ChoiceChanged(0, MappingChoice.Ignore))

        val state = viewModel.uiState.value

        assertEquals(false, state.isNameMapped)
        assertEquals(false, state.canConfirm)
    }

    @Test
    fun `an untouched empty key shows no error but blocks confirm`() = runTest {
        val viewModel = viewModel(FakeRepository())
        viewModel.onEvent(MappingEvent.ChoiceChanged(0, MappingChoice.CustomAttribute("")))

        val state = viewModel.uiState.value

        assertNull(state.keyError(0))
        assertEquals(false, state.canConfirm)
    }

    @Test
    fun `a bad key shows an inline error after typing`() = runTest {
        val viewModel = viewModel(FakeRepository())
        viewModel.onEvent(MappingEvent.CustomKeyChanged(0, "Fabric"))

        val state = viewModel.uiState.value

        assertEquals(MappingInvalidReason.BadCustomKey(0), state.keyError(0))
        assertEquals(false, state.canConfirm)
    }

    @Test
    fun `confirm success navigates to validate`() = runTest {
        val repository = FakeRepository()
        val viewModel = viewModel(repository)
        val (effects, collection) = collect(viewModel)

        viewModel.onEvent(MappingEvent.ConfirmClicked)

        assertEquals(false, viewModel.uiState.value.isConfirming)
        assertEquals(listOf(MappingEffect.NavigateToValidate("job-1")), effects)
        assertEquals(1, repository.confirmCalls)
        collection.cancel()
    }

    @Test
    fun `confirm sends one request on double tap`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeRepository(confirmGate = gate)
        val viewModel = viewModel(repository)
        val (effects, collection) = collect(viewModel)

        viewModel.onEvent(MappingEvent.ConfirmClicked)
        assertEquals(1, repository.confirmCalls)
        viewModel.onEvent(MappingEvent.ConfirmClicked)
        gate.complete(Unit)
        runCurrent()

        assertEquals(1, repository.confirmCalls)
        assertEquals(listOf(MappingEffect.NavigateToValidate("job-1")), effects)
        collection.cancel()
    }

    @Test
    fun `confirm conflict navigates back with no banner`() = runTest {
        val repository = FakeRepository(
            confirm = ApiResult.Error(
                ApiErrorType.CONFLICT,
                errorCode = "IMPORT_NOT_AWAITING_MAPPING"
            )
        )
        val viewModel = viewModel(repository)
        val (effects, collection) = collect(viewModel)

        viewModel.onEvent(MappingEvent.ConfirmClicked)

        assertNull(viewModel.uiState.value.confirmFailureRes)
        assertEquals(listOf(MappingEffect.NavigateBack), effects)
        collection.cancel()
    }

    @Test
    fun `confirm validation error shows an inline banner`() = runTest {
        val repository = FakeRepository(
            confirm = ApiResult.Error(
                ApiErrorType.BAD_REQUEST,
                errorCode = "MAPPING_NAME_REQUIRED"
            )
        )
        val viewModel = viewModel(repository)
        val (effects, collection) = collect(viewModel)

        viewModel.onEvent(MappingEvent.ConfirmClicked)

        assertEquals(R.string.mapping_error_invalid, viewModel.uiState.value.confirmFailureRes)
        assertTrue(effects.isEmpty())
        collection.cancel()
    }

    @Test
    fun `confirm network failure shows a banner and keeps retry`() = runTest {
        val repository = FakeRepository(confirm = ApiResult.Error(ApiErrorType.NETWORK))
        val viewModel = viewModel(repository)

        viewModel.onEvent(MappingEvent.ConfirmClicked)

        val state = viewModel.uiState.value
        assertEquals(R.string.error_network, state.confirmFailureRes)
        assertTrue(state.canConfirm)
    }

    @Test
    fun `terminal load failure shows a message with back only`() = runTest {
        val repository = FakeRepository(
            payload = ApiResult.Error(ApiErrorType.NOT_FOUND, errorCode = "IMPORT_JOB_NOT_FOUND")
        )
        val viewModel = viewModel(repository)
        val (effects, collection) = collect(viewModel)

        val failure = viewModel.uiState.value.loadFailure
        assertEquals(R.string.mapping_error_job_gone, failure?.messageRes)
        assertEquals(false, failure?.retryable)
        assertEquals(false, viewModel.uiState.value.canConfirm)

        viewModel.onEvent(MappingEvent.BackClicked)

        assertEquals(listOf(MappingEffect.NavigateBack), effects)
        collection.cancel()
    }

    @Test
    fun `retryable load failure retries on demand`() = runTest {
        val repository = FakeRepository(payload = ApiResult.Error(ApiErrorType.NETWORK))
        val viewModel = viewModel(repository)

        assertEquals(true, viewModel.uiState.value.loadFailure?.retryable)

        viewModel.onEvent(MappingEvent.RetryLoad)

        assertEquals(2, repository.getCalls)
    }

    private companion object {
        val PAYLOAD = ApiResult.Success(
            MappingPayload(
                columns = listOf(
                    ColumnSuggestion("Item Name", 1, listOf("Shirt"), "STANDARD", "name"),
                    ColumnSuggestion("Price", 2, listOf("10"), "STANDARD", "basePrice")
                ),
                savedMapping = null
            )
        )
    }
}
