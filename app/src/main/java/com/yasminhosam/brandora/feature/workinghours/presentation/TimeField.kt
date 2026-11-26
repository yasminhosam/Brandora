package com.yasminhosam.brandora.feature.workinghours.presentation

import com.yasminhosam.brandora.core.base.UiEffect
import com.yasminhosam.brandora.core.base.UiEvent
import com.yasminhosam.brandora.core.base.UiState
import com.yasminhosam.brandora.feature.workinghours.domain.model.WorkingDay
import java.time.DayOfWeek
import java.time.LocalTime

enum class TimeField { OPENS, CLOSES }

data class TimeTarget(val day: DayOfWeek, val field: TimeField)

sealed interface WorkingHoursEvent : UiEvent {
    data object RetryClicked : WorkingHoursEvent
    data class DayToggled(val day: DayOfWeek, val isOpen: Boolean) : WorkingHoursEvent
    data class TimeClicked(val day: DayOfWeek, val field: TimeField) : WorkingHoursEvent
    data class TimePicked(val time: LocalTime) : WorkingHoursEvent
    data object PickerDismissed : WorkingHoursEvent
    data object SaveClicked : WorkingHoursEvent
}

data class WorkingHoursUiState(
    /** The draft being edited (always seven days once loaded). */
    val days: List<WorkingDay> = emptyList(),
    /** Last saved version, used to detect unsaved changes. */
    val saved: List<WorkingDay> = emptyList(),
    /** False when the business never saved a schedule; the first save is then always allowed. */
    val hadSchedule: Boolean = false,
    /** Opened right after the import: a Skip button is shown and a saved schedule auto-skips. */
    val isOnboarding: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorRes: Int? = null,
    val saveErrorRes: Int? = null,
    val invalidDays: Set<DayOfWeek> = emptySet(),
    val pickerTarget: TimeTarget? = null
) : UiState {

    val isDirty: Boolean
        get() = days.map { it.normalized() } != saved.map { it.normalized() }

    val canSave: Boolean
        get() = !isLoading && errorRes == null && !isSaving && (isDirty || !hadSchedule)
}

sealed interface WorkingHoursEffect : UiEffect {
    data object Saved : WorkingHoursEffect

    /** Onboarding opened for a business that already has a schedule: nothing left to ask. */
    data object AlreadyConfigured : WorkingHoursEffect
}

object WorkingHoursNav {
    /** Optional boolean route argument; defaults to false (opened from the Home card). */
    const val ARG_ONBOARDING = "onboarding"
}