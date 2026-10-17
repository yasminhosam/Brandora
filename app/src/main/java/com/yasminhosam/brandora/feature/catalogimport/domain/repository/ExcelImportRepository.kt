package com.yasminhosam.brandora.feature.catalogimport.domain.repository

import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.MappedColumn
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.MappingPayload
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ExcelFile
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ImportAnalysis

interface ExcelImportRepository {

    /**
     * @param sheetName Null analyzes the first sheet. Re-analyzing another sheet returns a new importJobId.
     */
    suspend fun analyze(file: ExcelFile, sheetName: String? = null): ApiResult<ImportAnalysis>

    suspend fun getMapping(importId: String): ApiResult<MappingPayload>

    suspend fun confirmMapping(importId: String, columns: List<MappedColumn>): ApiResult<Unit>
}
