package com.yasminhosam.brandora.feature.workinghours.domain.repository

import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.workinghours.domain.model.WorkingDay

interface WorkingHoursRepository {

    /** Always seven days, Monday first. [hadSchedule] is false when the business never saved one. */
    suspend fun getWorkingHours(): ApiResult<WorkingSchedule>

    /** Replaces the whole week; [days] must contain all seven. */
    suspend fun saveWorkingHours(days: List<WorkingDay>): ApiResult<WorkingSchedule>
}

data class WorkingSchedule(
    val days: List<WorkingDay>,
    val hadSchedule: Boolean
)