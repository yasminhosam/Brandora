package com.yasminhosam.brandora.feature.catalogimport.data.remote

import kotlinx.serialization.Serializable

@Serializable
internal data class ExecuteSummaryDto(
    val totalRows: Int = 0,
    val validRows: Int = 0,
    val invalidRows: Int = 0,
    val skippedRows: Int = 0,
    val insertedCount: Int = 0,
    val updatedCount: Int = 0,
    val failedCount: Int = 0,
    val unchangedCount: Int = 0
)

@Serializable
internal data class ExecutedRowDto(
    val rowNumber: Int,
    val externalKey: String? = null,
    val catalogItemId: String? = null
)

/** A valid row that failed during execution. Carries a single job-level [code], not field errors. */
@Serializable
internal data class FailedRowDto(
    val rowNumber: Int,
    val code: String = "",
    // English backend message; the app localizes by [code] instead.
    val message: String? = null
)

@Serializable
internal data class ExecuteResponseDto(
    val importId: String,
    val status: String,
    val summary: ExecuteSummaryDto = ExecuteSummaryDto(),
    val inserted: List<ExecutedRowDto> = emptyList(),
    val updated: List<ExecutedRowDto> = emptyList(),
    val unchanged: List<ExecutedRowDto> = emptyList(),
    val failedRows: List<FailedRowDto> = emptyList(),
    val invalidRows: List<InvalidRowDto> = emptyList()
)

/** Compact job-status contract from GET /imports/{importId}. No mapping, errors or row lists. */
@Serializable
internal data class JobStatusDto(
    val id: String,
    val businessId: String? = null,
    val status: String,
    val fileName: String? = null,
    val sheetName: String? = null,
    val headerRow: Int? = null,
    val totalRows: Int = 0,
    val validRows: Int = 0,
    val invalidRows: Int = 0,
    val skippedRows: Int = 0,
    val insertedCount: Int = 0,
    val updatedCount: Int = 0,
    val failedCount: Int = 0,
    val createdAt: String? = null,
    val updatedAt: String? = null
)
