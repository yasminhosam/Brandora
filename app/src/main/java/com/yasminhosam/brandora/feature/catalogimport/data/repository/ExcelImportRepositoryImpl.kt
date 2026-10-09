package com.yasminhosam.brandora.feature.catalogimport.data.repository

import com.yasminhosam.brandora.core.network.ApiConfig
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.catalogimport.data.remote.CatalogImportRemoteDataSource
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ExcelFile
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ImportAnalysis
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
}
