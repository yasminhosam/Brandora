package com.yasminhosam.brandora.feature.business.domain.usecase

import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.business.domain.model.BusinessProfile
import com.yasminhosam.brandora.feature.business.domain.model.BusinessRules
import com.yasminhosam.brandora.feature.business.domain.model.NewBusiness
import com.yasminhosam.brandora.feature.business.domain.repository.BusinessRepository
import javax.inject.Inject

/** Creates the business; the name is trimmed and validated against the backend's rules first. */
class CreateBusinessUseCase @Inject constructor(
    private val repository: BusinessRepository
) {
    suspend operator fun invoke(input: NewBusiness): ApiResult<BusinessProfile> {
        val name = input.name.trim()
        if (BusinessRules.nameError(name) != null) return ApiResult.Error(ApiErrorType.BAD_REQUEST)
        return repository.createBusiness(input.copy(name = name))
    }
}
