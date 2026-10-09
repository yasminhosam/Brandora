package com.yasminhosam.brandora.feature.catalogimport.domain.usecase

import com.yasminhosam.brandora.core.network.ApiConfig
import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ExcelFile
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ImportAnalysis
import com.yasminhosam.brandora.feature.catalogimport.domain.repository.ExcelImportRepository
import javax.inject.Inject

/**
 * [validate] is exposed separately from [invoke] so the screen can reject a file instantly
 * with no network call; [invoke] re-validates as a last line of defense.
 */
class AnalyzeExcelFileUseCase @Inject constructor(
    private val repository: ExcelImportRepository
) {

    fun validate(file: ExcelFile): ApiErrorType? = when {
        !file.isXlsx -> ApiErrorType.INVALID_FILE_TYPE
        // Unknown size (-1) is allowed through; the backend's 413 is the fallback guard.
        file.sizeBytes > ApiConfig.MAX_UPLOAD_BYTES -> ApiErrorType.FILE_TOO_LARGE
        else -> null
    }

    suspend operator fun invoke(file: ExcelFile, sheetName: String? = null): ApiResult<ImportAnalysis> {
        val invalid = validate(file)
        if (invalid != null) return ApiResult.Error(invalid)
        return repository.analyze(file, sheetName)
    }
}
