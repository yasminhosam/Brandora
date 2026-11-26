package com.yasminhosam.brandora.feature.catalogimport.presentation.mapping

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.yasminhosam.brandora.core.base.BaseViewModel
import com.yasminhosam.brandora.core.network.ApiParams
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.core.network.ImportErrorCodes
import com.yasminhosam.brandora.core.ui.mappingErrorRes
import com.yasminhosam.brandora.core.ui.messageRes
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.MappedColumn
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.MappingChoice
import com.yasminhosam.brandora.feature.catalogimport.domain.usecase.ConfirmMappingUseCase
import com.yasminhosam.brandora.feature.catalogimport.domain.usecase.GetMappingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class MappingViewModel
@Inject
constructor(
    private val getMapping: GetMappingUseCase,
    private val confirmMapping: ConfirmMappingUseCase,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<MappingUiState, MappingEvent, MappingEffect>(MappingUiState()) {

    private val importId: String = savedStateHandle.get<String>(ApiParams.PATH_IMPORT_ID).orEmpty()

    init {
        load()
    }

    override fun onEvent(event: MappingEvent) {
        when (event) {
            MappingEvent.RetryLoad -> load()
            MappingEvent.BackClicked ->
                // One pop per tap: a double-tapped back would skip past the Upload step.
                if (!uiState.value.isNavigating) {
                    setState { copy(isNavigating = true) }
                    sendEffect(MappingEffect.NavigateBack)
                }
            is MappingEvent.ChoiceChanged ->
                updateColumn(event.index) { copy(choice = event.choice) }
            is MappingEvent.CustomKeyChanged ->
                updateColumn(event.index) {
                    copy(
                        choice =
                            if (choice is MappingChoice.VariantAttribute)
                                MappingChoice.VariantAttribute(event.text)
                            else MappingChoice.CustomAttribute(event.text)
                    )
                }
            MappingEvent.ConfirmClicked -> onConfirm()
            MappingEvent.ConfirmErrorDismissed -> setState { copy(confirmFailureRes = null) }
        }
    }

    private fun updateColumn(index: Int, transform: MappedColumn.() -> MappedColumn) {
        setState {
            val updated = columns.mapIndexed { i, column ->
                if (i == index) column.transform() else column
            }
            copy(
                columns = updated,
                validation = confirmMapping.validate(updated),
                confirmFailureRes = null,
            )
        }
    }

    private fun load() {
        setState { copy(isLoading = true, loadFailure = null) }
        viewModelScope.launch {
            when (val result = getMapping(importId)) {
                is ApiResult.Success ->
                    setState {
                        copy(
                            isLoading = false,
                            columns = result.data,
                            validation = confirmMapping.validate(result.data),
                        )
                    }
                is ApiResult.Error ->
                    setState {
                        copy(
                            isLoading = false,
                            loadFailure =
                                LoadFailure(
                                    messageRes =
                                        result.errorCode?.mappingErrorRes()
                                            ?: result.messageRes(),
                                    retryable = result.errorCode !in BACK_CODES,
                                ),
                        )
                    }
            }
        }
    }

    private fun onConfirm() {
        val state = uiState.value
        if (!state.canConfirm) return
        setState { copy(isConfirming = true, confirmFailureRes = null) }
        viewModelScope.launch {
            when (val result = confirmMapping(importId, uiState.value.columns)) {
                is ApiResult.Success -> {
                    // Latch: canConfirm stays false, so a tap landing after this point must
                    // not re-confirm (a repeat would 409 and bounce the user back out).
                    setState { copy(isConfirming = false, isNavigating = true) }
                    sendEffect(MappingEffect.NavigateToValidate(importId))
                }
                is ApiResult.Error ->
                    if (result.errorCode in BACK_CODES) {
                        setState { copy(isConfirming = false, isNavigating = true) }
                        sendEffect(MappingEffect.NavigateBack)
                    } else {
                        setState {
                            copy(
                                isConfirming = false,
                                confirmFailureRes =
                                    result.errorCode?.mappingErrorRes() ?: result.messageRes(),
                            )
                        }
                    }
            }
        }
    }

    private companion object {
        val BACK_CODES =
            setOf(
                ImportErrorCodes.IMPORT_JOB_NOT_FOUND,
                ImportErrorCodes.IMPORT_NOT_AWAITING_MAPPING,
                ImportErrorCodes.IMPORT_MAPPING_CHANGED,
                ImportErrorCodes.IMPORT_WORKBOOK_UNAVAILABLE,
                ImportErrorCodes.IMPORT_WORKBOOK_READ_FAILED,
            )
    }
}
