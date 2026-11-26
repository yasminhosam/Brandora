package com.yasminhosam.brandora.feature.workinghours.data.remote

import com.yasminhosam.brandora.core.network.ApiErrorHandler
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.core.network.map
import com.yasminhosam.brandora.feature.workinghours.data.mapper.toRequestDto
import com.yasminhosam.brandora.feature.workinghours.data.mapper.toSchedule
import com.yasminhosam.brandora.feature.workinghours.domain.model.WorkingDay
import com.yasminhosam.brandora.feature.workinghours.domain.repository.WorkingSchedule
import retrofit2.Retrofit
import javax.inject.Inject

class WorkingHoursRemoteDataSource @Inject constructor(
    retrofit: Retrofit,
    private val errorHandler: ApiErrorHandler
) {
    private val api: WorkingHoursApiService = retrofit.create(WorkingHoursApiService::class.java)

    suspend fun getWorkingHours(businessId: String): ApiResult<WorkingSchedule> =
        errorHandler.safeApiCall { api.getWorkingHours(businessId) }.map { it.toSchedule() }

    suspend fun saveWorkingHours(businessId: String, days: List<WorkingDay>): ApiResult<WorkingSchedule> =
        errorHandler.safeApiCall { api.putWorkingHours(businessId, days.toRequestDto()) }
            .map { it.toSchedule() }
}