package com.yasminhosam.brandora.feature.business.presentation

import androidx.lifecycle.viewModelScope
import com.yasminhosam.brandora.core.base.BaseViewModel
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.business.domain.model.BusinessRules
import com.yasminhosam.brandora.feature.business.domain.model.NewBusiness
import com.yasminhosam.brandora.feature.business.domain.repository.CurrentBusinessStore
import com.yasminhosam.brandora.feature.business.domain.usecase.CreateBusinessUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BusinessViewModel @Inject constructor(
    private val createBusiness: CreateBusinessUseCase,
    private val currentBusinessStore: CurrentBusinessStore
) : BaseViewModel<BusinessUiState, BusinessEvent, BusinessEffect>(BusinessUiState()) {

    override fun onEvent(event: BusinessEvent) {
        when (event) {
            is BusinessEvent.NameChanged -> onNameChanged(event.value)
            is BusinessEvent.DescriptionChanged -> setState { copy(description = event.value) }
            is BusinessEvent.CategoryChanged -> setState { copy(category = event.value) }
            is BusinessEvent.PhoneChanged -> setState { copy(phone = event.value) }
            is BusinessEvent.AddressChanged -> setState { copy(address = event.value) }
            BusinessEvent.CreateClicked -> onCreate()
        }
    }

    /** A shown error follows typing; an untouched field never shows one. */
    private fun onNameChanged(value: String) {
        setState {
            copy(
                name = value,
                nameError = if (nameError != null) BusinessRules.nameError(value) else null
            )
        }
    }

    private fun onCreate() {
        val state = uiState.value
        val error = BusinessRules.nameError(state.name)
        if (error != null) {
            setState { copy(nameError = error) }
            return
        }
        setState { copy(isCreating = true, createError = null) }
        viewModelScope.launch {
            val input = NewBusiness(
                name = state.name.trim(),
                description = state.description.ifBlank { null },
                category = state.category.ifBlank { null },
                phone = state.phone.ifBlank { null },
                address = state.address.ifBlank { null }
            )
            when (val result = createBusiness(input)) {
                is ApiResult.Success -> {
                    // Persisting here (not in the screen) keeps the id durable before navigation.
                    currentBusinessStore.setBusinessId(result.data.id)
                    setState { copy(isCreating = false) }
                    sendEffect(BusinessEffect.BusinessCreated)
                }
                is ApiResult.Error ->
                    setState { copy(isCreating = false, createError = result.type) }
            }
        }
    }
}
