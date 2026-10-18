package com.yasminhosam.brandora.feature.catalogimport.domain.usecase

import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ValidationResult
import com.yasminhosam.brandora.feature.catalogimport.domain.repository.ExcelImportRepository
import javax.inject.Inject

/** Triggers validation on the backend. Synchronous: returns once the job settles. */
class StartValidationUseCase @Inject constructor(
    private val repository: ExcelImportRepository
) {
    suspend operator fun invoke(importId: String): ApiResult<ValidationResult> {
        if (importId.isBlank()) return ApiResult.Error(ApiErrorType.BAD_REQUEST)
        return repository.validate(importId)
    }
}
