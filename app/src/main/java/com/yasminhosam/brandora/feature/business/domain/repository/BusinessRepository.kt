package com.yasminhosam.brandora.feature.business.domain.repository

import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.business.domain.model.BusinessProfile
import com.yasminhosam.brandora.feature.business.domain.model.NewBusiness

interface BusinessRepository {
    suspend fun createBusiness(input: NewBusiness): ApiResult<BusinessProfile>
}
