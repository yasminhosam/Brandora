package com.yasminhosam.brandora.feature.catalogimport.data.repository

import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.business.domain.repository.CurrentBusinessStore
import com.yasminhosam.brandora.feature.catalogimport.data.remote.CatalogImportRemoteDataSource
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.MappedColumn
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.MappingPayload
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ExcelFile
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ExecuteResult
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ImportAnalysis
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ImportJobStatus
import com.yasminhosam.brandora.feature.catalogimport.domain.model.PreviewResult
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ValidationResult
import com.yasminhosam.brandora.feature.catalogimport.domain.repository.ExcelImportRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Reads the business id from [CurrentBusinessStore] (Preferences DataStore). When real auth
 * arrives, only the value source here changes. analyze() refuses to call the backend without
 * a business id rather than sending a placeholder.
 */
class ExcelImportRepositoryImpl @Inject constructor(
    private val remote: CatalogImportRemoteDataSource,
    private val currentBusinessStore: CurrentBusinessStore
) : ExcelImportRepository {

    override suspend fun analyze(file: ExcelFile, sheetName: String?): ApiResult<ImportAnalysis> {
        val businessId = currentBusinessStore.businessId.first()
            ?: return ApiResult.Error(ApiErrorType.UNKNOWN)
        return remote.analyze(
            file = file,
            businessId = businessId,
            sheetName = sheetName
        )
    }

    override suspend fun getMapping(importId: String): ApiResult<MappingPayload> =
        remote.getMapping(importId)

    override suspend fun confirmMapping(importId: String, columns: List<MappedColumn>): ApiResult<Unit> =
        remote.confirmMapping(importId, columns)

    override suspend fun validate(importId: String): ApiResult<ValidationResult> =
        remote.validate(importId)

    override suspend fun getValidationResults(importId: String, limit: Int, offset: Int): ApiResult<ValidationResult> =
        remote.getValidationResults(importId, limit, offset)

    override suspend fun getPreview(importId: String, limit: Int, offset: Int): ApiResult<PreviewResult> =
        remote.getPreview(importId, limit, offset)

    override suspend fun execute(importId: String): ApiResult<ExecuteResult> =
        remote.execute(importId)

    override suspend fun getImportStatus(importId: String): ApiResult<ImportJobStatus> =
        remote.getImportStatus(importId)
}
