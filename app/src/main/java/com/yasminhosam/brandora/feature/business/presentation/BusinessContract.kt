package com.yasminhosam.brandora.feature.business.presentation

import com.yasminhosam.brandora.core.base.UiEffect
import com.yasminhosam.brandora.core.base.UiEvent
import com.yasminhosam.brandora.core.base.UiState
import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.feature.business.domain.model.BusinessNameError

sealed interface BusinessEvent : UiEvent {
    data class NameChanged(val value: String) : BusinessEvent
    data class DescriptionChanged(val value: String) : BusinessEvent
    data class CategoryChanged(val value: String) : BusinessEvent
    data class PhoneChanged(val value: String) : BusinessEvent
    data class AddressChanged(val value: String) : BusinessEvent
    data object CreateClicked : BusinessEvent
}

data class BusinessUiState(
    val name: String = "",
    val description: String = "",
    val category: String = "",
    val phone: String = "",
    val address: String = "",
    val nameError: BusinessNameError? = null,
    val isCreating: Boolean = false,
    val createError: ApiErrorType? = null
) : UiState

sealed interface BusinessEffect : UiEffect {
    /** The id is already persisted when this fires; the screen only navigates. */
    data object BusinessCreated : BusinessEffect
}
