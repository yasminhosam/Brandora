package com.yasminhosam.brandora.feature.workinghours.data.repository

import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.business.domain.repository.CurrentBusinessStore
import com.yasminhosam.brandora.feature.workinghours.data.remote.WorkingHoursRemoteDataSource
import com.yasminhosam.brandora.feature.workinghours.domain.model.WorkingDay
import com.yasminhosam.brandora.feature.workinghours.domain.repository.WorkingHoursRepository
import com.yasminhosam.brandora.feature.workinghours.domain.repository.WorkingSchedule
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Refuses to call the backend without a current business rather than sending a placeholder id. */
class WorkingHoursRepositoryImpl @Inject constructor(
    private val remote: WorkingHoursRemoteDataSource,
    private val currentBusinessStore: CurrentBusinessStore
) : WorkingHoursRepository {

    override suspend fun getWorkingHours(): ApiResult<WorkingSchedule> {
        val businessId = currentBusinessStore.businessId.first() ?: return noBusiness()
        return remote.getWorkingHours(businessId)
    }

    override suspend fun saveWorkingHours(days: List<WorkingDay>): ApiResult<WorkingSchedule> {
        val businessId = currentBusinessStore.businessId.first() ?: return noBusiness()
        return remote.saveWorkingHours(businessId, days)
    }

    private fun noBusiness() = ApiResult.Error(ApiErrorType.UNKNOWN)
}