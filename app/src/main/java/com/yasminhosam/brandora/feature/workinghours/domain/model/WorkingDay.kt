package com.yasminhosam.brandora.feature.workinghours.domain.model

import java.time.DayOfWeek
import java.time.LocalTime

/** One weekday of the schedule. Times are only meaningful while [isOpen] is true. */
data class WorkingDay(
    val day: DayOfWeek,
    val isOpen: Boolean,
    val opensAt: LocalTime? = null,
    val closesAt: LocalTime? = null
) {
    /** Open days need both times and closing strictly after opening (overnight is unsupported). */
    val isValid: Boolean
        get() = !isOpen || (opensAt != null && closesAt != null && closesAt.isAfter(opensAt))

    /** Closed days carry no times, matching what the backend stores. */
    fun normalized(): WorkingDay =
        if (isOpen) this else copy(opensAt = null, closesAt = null)

    /** Re-opening a day with no times starts from the default hours. */
    fun opened(): WorkingDay = copy(
        isOpen = true,
        opensAt = opensAt ?: DEFAULT_OPENS_AT,
        closesAt = closesAt ?: DEFAULT_CLOSES_AT
    )

    companion object {
        val DEFAULT_OPENS_AT: LocalTime = LocalTime.of(9, 0)
        val DEFAULT_CLOSES_AT: LocalTime = LocalTime.of(17, 0)
    }
}

/** Backend order: Monday through Sunday. */
val WEEK_ORDER: List<DayOfWeek> = DayOfWeek.entries

/** A schedule that was never saved: every day closed. */
fun emptyWeek(): List<WorkingDay> = WEEK_ORDER.map { WorkingDay(it, isOpen = false) }

fun List<WorkingDay>.invalidDays(): Set<DayOfWeek> = filterNot { it.isValid }.map { it.day }.toSet()