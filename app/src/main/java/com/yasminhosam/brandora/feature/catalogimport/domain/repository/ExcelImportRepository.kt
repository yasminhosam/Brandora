package com.yasminhosam.brandora.feature.catalogimport.domain.repository

import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ExcelFile
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ImportAnalysis

interface ExcelImportRepository {

    /**
     * @param sheetName Null analyzes the first sheet. Re-analyzing another sheet returns a new importJobId.
     */
    suspend fun analyze(file: ExcelFile, sheetName: String? = null): ApiResult<ImportAnalysis>
}
