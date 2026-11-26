package com.yasminhosam.brandora.feature.workinghours.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Same shape for request and response. The app's Json uses explicitNulls=false, so a closed
 * day's null times are omitted from the request, which the backend accepts. id, businessId
 * and timestamps in responses are ignored.
 */
@Serializable
internal data class WorkingDayDto(
    val dayOfWeek: String,
    val isOpen: Boolean,
    val opensAt: String? = null,
    val closesAt: String? = null
)

@Serializable
internal data class PutWorkingHoursRequestDto(
    val days: List<WorkingDayDto>
)