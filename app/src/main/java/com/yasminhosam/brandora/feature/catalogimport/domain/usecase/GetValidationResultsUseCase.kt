package com.yasminhosam.brandora.feature.catalogimport.domain.usecase

import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ValidationResult
import com.yasminhosam.brandora.feature.catalogimport.domain.repository.ExcelImportRepository
import javax.inject.Inject

/** Fetches one page of validation rows. Offset advances by [PAGE_SIZE] per page. */
class GetValidationResultsUseCase @Inject constructor(
    private val repository: ExcelImportRepository
) {
    suspend operator fun invoke(importId: String, offset: Int): ApiResult<ValidationResult> {
        if (importId.isBlank()) return ApiResult.Error(ApiErrorType.BAD_REQUEST)
        return repository.getValidationResults(importId, PAGE_SIZE, offset)
    }

    companion object {
        const val PAGE_SIZE = 50
    }
}
