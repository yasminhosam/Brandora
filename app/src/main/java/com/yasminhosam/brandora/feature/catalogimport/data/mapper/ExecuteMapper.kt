package com.yasminhosam.brandora.feature.catalogimport.data.mapper

import com.yasminhosam.brandora.feature.catalogimport.data.remote.ExecuteResponseDto
import com.yasminhosam.brandora.feature.catalogimport.data.remote.ExecuteSummaryDto
import com.yasminhosam.brandora.feature.catalogimport.data.remote.JobStatusDto
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ExecuteCounts
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ExecuteResult
import com.yasminhosam.brandora.feature.catalogimport.domain.model.FailedRow
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ImportJobStatus
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ImportStatus

internal fun ExecuteResponseDto.toDomain(): ExecuteResult = ExecuteResult(
    importId = importId,
    status = ImportStatus.from(status),
    counts = summary.toCounts(),
    invalidRows = invalidRows.map { it.toDomain() },
    failedRows = failedRows.map { FailedRow(rowNumber = it.rowNumber, code = it.code) }
)

internal fun ExecuteSummaryDto.toCounts(): ExecuteCounts = ExecuteCounts(
    inserted = insertedCount,
    updated = updatedCount,
    unchanged = unchangedCount,
    failed = failedCount
)

internal fun JobStatusDto.toDomain(): ImportJobStatus = ImportJobStatus(
    importId = id,
    status = ImportStatus.from(status),
    totalRows = totalRows,
    validRows = validRows,
    invalidRows = invalidRows,
    skippedRows = skippedRows,
    insertedCount = insertedCount,
    updatedCount = updatedCount,
    failedCount = failedCount
)
