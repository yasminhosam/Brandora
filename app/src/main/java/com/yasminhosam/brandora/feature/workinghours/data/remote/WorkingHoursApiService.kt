package com.yasminhosam.brandora.feature.workinghours.data.remote

import com.yasminhosam.brandora.core.network.ApiEndpoints
import com.yasminhosam.brandora.core.network.ApiParams
import com.yasminhosam.brandora.feature.workinghours.data.remote.dto.PutWorkingHoursRequestDto
import com.yasminhosam.brandora.feature.workinghours.data.remote.dto.WorkingDayDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

internal interface WorkingHoursApiService {

    /** Returns an empty list when no schedule has been saved yet. */
    @GET(ApiEndpoints.WORKING_HOURS)
    suspend fun getWorkingHours(
        @Path(ApiParams.PATH_BUSINESS_ID) businessId: String
    ): List<WorkingDayDto>

    @PUT(ApiEndpoints.WORKING_HOURS)
    suspend fun putWorkingHours(
        @Path(ApiParams.PATH_BUSINESS_ID) businessId: String,
        @Body body: PutWorkingHoursRequestDto
    ): List<WorkingDayDto>
}