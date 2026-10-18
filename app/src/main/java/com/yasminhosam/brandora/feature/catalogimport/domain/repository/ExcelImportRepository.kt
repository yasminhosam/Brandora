package com.yasminhosam.brandora.feature.catalogimport.domain.repository

import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.MappedColumn
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.MappingPayload
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ExcelFile
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ExecuteResult
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ImportAnalysis
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ImportJobStatus
import com.yasminhosam.brandora.feature.catalogimport.domain.model.PreviewResult
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ValidationResult

interface ExcelImportRepository {

    /**
     * @param sheetName Null analyzes the first sheet. Re-analyzing another sheet returns a new importJobId.
     */
    suspend fun analyze(file: ExcelFile, sheetName: String? = null): ApiResult<ImportAnalysis>

    suspend fun getMapping(importId: String): ApiResult<MappingPayload>

    suspend fun confirmMapping(importId: String, columns: List<MappedColumn>): ApiResult<Unit>

    /** Triggers validation on the backend and returns the summary + first page of rows. */
    suspend fun validate(importId: String): ApiResult<ValidationResult>

    /** Fetches the next page of validation rows. */
    suspend fun getValidationResults(importId: String, limit: Int, offset: Int): ApiResult<ValidationResult>

    /** Fetches one advisory preview page, recomputed on the backend per request. */
    suspend fun getPreview(importId: String, limit: Int, offset: Int): ApiResult<PreviewResult>

    /** Runs the import over every row. Synchronous; may leave the job IMPORTING on interruption. */
    suspend fun execute(importId: String): ApiResult<ExecuteResult>

    /** Reads the persisted job status. Used to recover after an interrupted or failed execute. */
    suspend fun getImportStatus(importId: String): ApiResult<ImportJobStatus>
}
