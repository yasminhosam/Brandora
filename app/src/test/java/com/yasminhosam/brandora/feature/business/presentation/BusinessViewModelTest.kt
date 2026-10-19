package com.yasminhosam.brandora.feature.business.presentation

import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.business.domain.model.BusinessNameError
import com.yasminhosam.brandora.feature.business.domain.model.BusinessProfile
import com.yasminhosam.brandora.feature.business.domain.model.NewBusiness
import com.yasminhosam.brandora.feature.business.domain.repository.BusinessRepository
import com.yasminhosam.brandora.feature.business.domain.repository.CurrentBusinessStore
import com.yasminhosam.brandora.feature.business.domain.usecase.CreateBusinessUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BusinessViewModelTest {

    private val mainDispatcher = UnconfinedTestDispatcher()

    private class FakeRepository(
        private val result: ApiResult<BusinessProfile>
    ) : BusinessRepository {

        var calls = 0
            private set

        var lastInput: NewBusiness? = null
            private set

        override suspend fun createBusiness(input: NewBusiness): ApiResult<BusinessProfile> {
            calls++
            lastInput = input
            return result
        }
    }

    private class FakeStore : CurrentBusinessStore {
        private val _businessId = MutableStateFlow<String?>(null)
        override val businessId: Flow<String?> = _businessId

        var savedId: String? = null
            private set

        override suspend fun setBusinessId(id: String) {
            savedId = id
            _businessId.value = id
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        repository: FakeRepository = FakeRepository(SUCCESS),
        store: FakeStore = FakeStore()
    ) = BusinessViewModel(
        createBusiness = CreateBusinessUseCase(repository),
        currentBusinessStore = store
    )

    @Test
    fun `submitting a blank name shows the error and never calls the backend`() = runTest {
        val repository = FakeRepository(SUCCESS)
        val viewModel = viewModel(repository = repository)

        viewModel.onEvent(BusinessEvent.NameChanged("   "))
        viewModel.onEvent(BusinessEvent.CreateClicked)

        val state = viewModel.uiState.value
        assertEquals(BusinessNameError.BLANK, state.nameError)
        assertEquals(0, repository.calls)
        assertFalse(state.isCreating)
    }

    @Test
    fun `a shown name error follows typing until the name is valid`() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(BusinessEvent.NameChanged(""))
        viewModel.onEvent(BusinessEvent.CreateClicked)
        viewModel.onEvent(BusinessEvent.NameChanged("متجر التمر"))

        assertNull(viewModel.uiState.value.nameError)
    }

    @Test
    fun `a successful create persists the id and emits BusinessCreated`() = runTest {
        val store = FakeStore()
        val viewModel = viewModel(store = store)
        val effects = mutableListOf<BusinessEffect>()
        val collection = launch(mainDispatcher) { viewModel.effects.collect { effects += it } }

        viewModel.onEvent(BusinessEvent.NameChanged("  متجر التمر  "))
        viewModel.onEvent(BusinessEvent.CreateClicked)

        assertEquals(PROFILE.id, store.savedId)
        assertEquals(listOf<BusinessEffect>(BusinessEffect.BusinessCreated), effects)
        assertFalse(viewModel.uiState.value.isCreating)
        collection.cancel()
    }

    @Test
    fun `a failed create surfaces the error and re-enables the button`() = runTest {
        val repository = FakeRepository(ApiResult.Error(ApiErrorType.CONFLICT))
        val viewModel = viewModel(repository = repository)

        viewModel.onEvent(BusinessEvent.NameChanged("متجر التمر"))
        viewModel.onEvent(BusinessEvent.CreateClicked)

        val state = viewModel.uiState.value
        assertEquals(ApiErrorType.CONFLICT, state.createError)
        assertFalse(state.isCreating)
        assertEquals(1, repository.calls)
    }

    private companion object {
        val PROFILE = BusinessProfile(
            id = "f6bc7287-3285-4e19-a819-459c41f0a138",
            name = "متجر التمر",
            description = null,
            category = null,
            phone = null,
            address = null
        )
        val SUCCESS = ApiResult.Success(PROFILE)
    }
}
