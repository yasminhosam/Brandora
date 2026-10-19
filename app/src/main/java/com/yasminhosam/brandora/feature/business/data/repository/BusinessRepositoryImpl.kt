package com.yasminhosam.brandora.feature.business.data.repository

import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.business.data.remote.BusinessRemoteDataSource
import com.yasminhosam.brandora.feature.business.domain.model.BusinessProfile
import com.yasminhosam.brandora.feature.business.domain.model.NewBusiness
import com.yasminhosam.brandora.feature.business.domain.repository.BusinessRepository
import javax.inject.Inject

class BusinessRepositoryImpl @Inject constructor(
    private val remote: BusinessRemoteDataSource
) : BusinessRepository {

    override suspend fun createBusiness(input: NewBusiness): ApiResult<BusinessProfile> =
        remote.createBusiness(input)
}
