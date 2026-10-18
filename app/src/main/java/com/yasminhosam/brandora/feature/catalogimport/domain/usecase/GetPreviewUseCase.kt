package com.yasminhosam.brandora.feature.catalogimport.domain.usecase

import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.catalogimport.domain.model.PreviewResult
import com.yasminhosam.brandora.feature.catalogimport.domain.repository.ExcelImportRepository
import javax.inject.Inject

/**
 * Fetches one advisory preview page. The backend recomputes the preview on each request, so the
 * action categories (insert / update / unchanged) can shift between pages; execute always
 * processes every row regardless of how many preview pages were loaded.
 */
class GetPreviewUseCase @Inject constructor(
    private val repository: ExcelImportRepository
) {
    suspend operator fun invoke(importId: String, offset: Int): ApiResult<PreviewResult> {
        if (importId.isBlank()) return ApiResult.Error(ApiErrorType.BAD_REQUEST)
        return repository.getPreview(importId, PAGE_SIZE, offset)
    }

    companion object {
        const val PAGE_SIZE = 50
    }
}
