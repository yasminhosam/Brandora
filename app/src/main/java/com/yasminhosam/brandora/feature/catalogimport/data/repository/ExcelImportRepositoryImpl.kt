package com.yasminhosam.brandora.feature.catalogimport.data.repository

import com.yasminhosam.brandora.core.network.ApiConfig
import com.yasminhosam.brandora.core.network.ApiResult
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
import javax.inject.Inject

/**
 * The only reader of [ApiConfig.CURRENT_BUSINESS_ID]; when real auth arrives, only the value
 * source here changes.
 */
class ExcelImportRepositoryImpl @Inject constructor(
    private val remote: CatalogImportRemoteDataSource
) : ExcelImportRepository {

    override suspend fun analyze(file: ExcelFile, sheetName: String?): ApiResult<ImportAnalysis> =
        remote.analyze(
            file = file,
            businessId = ApiConfig.CURRENT_BUSINESS_ID,
            sheetName = sheetName
        )

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
