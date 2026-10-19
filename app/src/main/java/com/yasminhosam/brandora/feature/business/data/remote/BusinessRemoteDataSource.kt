package com.yasminhosam.brandora.feature.business.data.remote

import com.yasminhosam.brandora.core.network.ApiErrorHandler
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.core.network.map
import com.yasminhosam.brandora.feature.business.data.mapper.toDomain
import com.yasminhosam.brandora.feature.business.data.mapper.toRequestDto
import com.yasminhosam.brandora.feature.business.domain.model.BusinessProfile
import com.yasminhosam.brandora.feature.business.domain.model.NewBusiness
import retrofit2.Retrofit
import javax.inject.Inject

class BusinessRemoteDataSource @Inject constructor(
    retrofit: Retrofit,
    private val errorHandler: ApiErrorHandler
) {
    private val api: BusinessApiService = retrofit.create(BusinessApiService::class.java)

    suspend fun createBusiness(input: NewBusiness): ApiResult<BusinessProfile> =
        errorHandler.safeApiCall { api.createBusiness(input.toRequestDto()) }.map { it.toDomain() }
}
