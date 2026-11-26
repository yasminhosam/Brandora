package com.yasminhosam.brandora.feature.workinghours.data.mapper

import com.yasminhosam.brandora.feature.workinghours.data.remote.dto.PutWorkingHoursRequestDto
import com.yasminhosam.brandora.feature.workinghours.data.remote.dto.WorkingDayDto
import com.yasminhosam.brandora.feature.workinghours.domain.model.WEEK_ORDER
import com.yasminhosam.brandora.feature.workinghours.domain.model.WorkingDay
import com.yasminhosam.brandora.feature.workinghours.domain.model.emptyWeek
import com.yasminhosam.brandora.feature.workinghours.domain.repository.WorkingSchedule
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

// Wire format is 24-hour HH:mm; Locale.ROOT keeps digits ASCII on Arabic devices.
private val WIRE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)

internal fun String?.toLocalTimeOrNull(): LocalTime? =
    if (isNullOrBlank()) null else try {
        LocalTime.parse(this, WIRE_TIME)
    } catch (_: DateTimeParseException) {
        null
    }

private fun String.toDayOrNull(): DayOfWeek? = DayOfWeek.entries.firstOrNull { it.name == this }

internal fun WorkingDayDto.toDomain(): WorkingDay? {
    val day = dayOfWeek.toDayOrNull() ?: return null
    return WorkingDay(
        day = day,
        isOpen = isOpen,
        opensAt = opensAt.toLocalTimeOrNull(),
        closesAt = closesAt.toLocalTimeOrNull()
    ).normalized()
}

/**
 * Always yields seven days in Monday-first order. A missing or unknown row (legacy partial
 * data, new weekday value) becomes a closed day instead of breaking the screen.
 */
internal fun List<WorkingDayDto>.toSchedule(): WorkingSchedule {
    if (isEmpty()) return WorkingSchedule(emptyWeek(), hadSchedule = false)
    val byDay = mapNotNull { it.toDomain() }.associateBy { it.day }
    return WorkingSchedule(
        days = WEEK_ORDER.map { byDay[it] ?: WorkingDay(it, isOpen = false) },
        hadSchedule = true
    )
}

internal fun WorkingDay.toDto(): WorkingDayDto {
    val day = normalized()
    return WorkingDayDto(
        dayOfWeek = day.day.name,
        isOpen = day.isOpen,
        opensAt = day.opensAt?.format(WIRE_TIME),
        closesAt = day.closesAt?.format(WIRE_TIME)
    )
}

internal fun List<WorkingDay>.toRequestDto() = PutWorkingHoursRequestDto(days = map { it.toDto() })