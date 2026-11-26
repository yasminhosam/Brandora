package com.yasminhosam.brandora.feature.workinghours.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.yasminhosam.brandora.core.base.BaseViewModel
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.core.ui.messageRes
import com.yasminhosam.brandora.feature.workinghours.domain.model.WorkingDay
import com.yasminhosam.brandora.feature.workinghours.domain.model.invalidDays
import com.yasminhosam.brandora.feature.workinghours.domain.usecase.GetWorkingHoursUseCase
import com.yasminhosam.brandora.feature.workinghours.domain.usecase.SaveWorkingHoursUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class WorkingHoursViewModel @Inject constructor(
    private val getWorkingHours: GetWorkingHoursUseCase,
    private val saveWorkingHours: SaveWorkingHoursUseCase,
    savedStateHandle: SavedStateHandle
) : BaseViewModel<WorkingHoursUiState, WorkingHoursEvent, WorkingHoursEffect>(
    WorkingHoursUiState(
        isOnboarding = savedStateHandle.get<Boolean>(WorkingHoursNav.ARG_ONBOARDING) ?: false
    )
) {

    init { load() }

    override fun onEvent(event: WorkingHoursEvent) {
        when (event) {
            WorkingHoursEvent.RetryClicked -> {
                setState { copy(isLoading = true, errorRes = null) }
                load()
            }
            is WorkingHoursEvent.DayToggled -> updateDay(event.day) {
                if (event.isOpen) opened() else copy(isOpen = false)
            }
            is WorkingHoursEvent.TimeClicked ->
                setState { copy(pickerTarget = TimeTarget(event.day, event.field)) }
            is WorkingHoursEvent.TimePicked -> applyPickedTime(event.time)
            WorkingHoursEvent.PickerDismissed -> setState { copy(pickerTarget = null) }
            WorkingHoursEvent.SaveClicked -> save()
        }
    }

    private fun load() {
        viewModelScope.launch {
            when (val result = getWorkingHours()) {
                is ApiResult.Success -> setState {
                    copy(
                        days = result.data.days,
                        saved = result.data.days,
                        hadSchedule = result.data.hadSchedule,
                        isLoading = false,
                        errorRes = null
                    )
                }.also {
                    // Re-imports from Products should not nag about hours that are already set.
                    if (uiState.value.isOnboarding && result.data.hadSchedule) {
                        sendEffect(WorkingHoursEffect.AlreadyConfigured)
                    }
                }
                is ApiResult.Error -> setState {
                    copy(isLoading = false, errorRes = result.type.messageRes())
                }
            }
        }
    }

    private fun applyPickedTime(time: LocalTime) {
        val target = uiState.value.pickerTarget ?: return
        setState { copy(pickerTarget = null) }
        updateDay(target.day) {
            when (target.field) {
                TimeField.OPENS -> copy(opensAt = time)
                TimeField.CLOSES -> copy(closesAt = time)
            }
        }
    }

    /** Edits one day and clears its error and any stale save error. */
    private fun updateDay(day: DayOfWeek, change: WorkingDay.() -> WorkingDay) {
        setState {
            copy(
                days = days.map { if (it.day == day) it.change() else it },
                invalidDays = invalidDays - day,
                saveErrorRes = null
            )
        }
    }

    private fun save() {
        val state = uiState.value
        if (!state.canSave) return
        val invalid = state.days.invalidDays()
        if (invalid.isNotEmpty()) {
            setState { copy(invalidDays = invalid) }
            return
        }
        setState { copy(isSaving = true, saveErrorRes = null, invalidDays = emptySet()) }
        viewModelScope.launch {
            when (val result = saveWorkingHours(state.days)) {
                is ApiResult.Success -> {
                    setState {
                        copy(
                            days = result.data.days,
                            saved = result.data.days,
                            hadSchedule = true,
                            isSaving = false
                        )
                    }
                    sendEffect(WorkingHoursEffect.Saved)
                }
                is ApiResult.Error -> setState {
                    copy(isSaving = false, saveErrorRes = result.type.messageRes())
                }
            }
        }
    }
}