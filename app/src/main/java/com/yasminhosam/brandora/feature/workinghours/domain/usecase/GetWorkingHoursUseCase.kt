package com.yasminhosam.brandora.feature.workinghours.domain.usecase

import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.workinghours.domain.model.WorkingDay
import com.yasminhosam.brandora.feature.workinghours.domain.repository.WorkingHoursRepository
import com.yasminhosam.brandora.feature.workinghours.domain.repository.WorkingSchedule
import javax.inject.Inject

class GetWorkingHoursUseCase @Inject constructor(
    private val repository: WorkingHoursRepository
) {
    suspend operator fun invoke(): ApiResult<WorkingSchedule> = repository.getWorkingHours()
}

class SaveWorkingHoursUseCase @Inject constructor(
    private val repository: WorkingHoursRepository
) {
    suspend operator fun invoke(days: List<WorkingDay>): ApiResult<WorkingSchedule> =
        repository.saveWorkingHours(days)
}