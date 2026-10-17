package com.yasminhosam.brandora.feature.catalogimport.presentation.mapping

import com.yasminhosam.brandora.core.base.UiEffect
import com.yasminhosam.brandora.core.base.UiEvent
import com.yasminhosam.brandora.core.base.UiState
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.MappedColumn
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.MappingChoice
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.MappingInvalidReason
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.MappingValidationResult
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.StandardTarget

sealed interface MappingEvent : UiEvent {
    data object RetryLoad : MappingEvent
    data object BackClicked : MappingEvent
    data class ChoiceChanged(val index: Int, val choice: MappingChoice) : MappingEvent
    data class CustomKeyChanged(val index: Int, val text: String) : MappingEvent
    data object ConfirmClicked : MappingEvent
    data object ConfirmErrorDismissed : MappingEvent
}

data class LoadFailure(val messageRes: Int, val retryable: Boolean)

data class MappingUiState(
    val isLoading: Boolean = true,
    val columns: List<MappedColumn> = emptyList(),
    val validation: MappingValidationResult = MappingValidationResult.Valid,
    val loadFailure: LoadFailure? = null,
    val isConfirming: Boolean = false,
    val confirmFailureRes: Int? = null
) : UiState {

    val isNameMapped: Boolean get() =
        columns.any { (it.choice as? MappingChoice.Standard)?.target == StandardTarget.NAME }

    val isSkuMapped: Boolean get() =
        columns.any { (it.choice as? MappingChoice.Standard)?.target == StandardTarget.EXTERNAL_KEY }

    val canConfirm: Boolean get() =
        !isLoading && !isConfirming && loadFailure == null &&
            columns.isNotEmpty() && validation is MappingValidationResult.Valid

    fun keyError(index: Int): MappingInvalidReason? {
        val key = (columns.getOrNull(index)?.choice as? MappingChoice.CustomAttribute)?.key
            ?: return null
        if (key.isEmpty()) return null
        return (validation as? MappingValidationResult.Invalid)?.reasons?.firstOrNull {
            (it is MappingInvalidReason.EmptyCustomKey && it.index == index) ||
                (it is MappingInvalidReason.BadCustomKey && it.index == index) ||
                (it is MappingInvalidReason.CustomKeyUsedTwice && it.index == index)
        }
    }
}

sealed interface MappingEffect : UiEffect {
    data class NavigateToValidate(val importId: String) : MappingEffect
    data object NavigateBack : MappingEffect
}
