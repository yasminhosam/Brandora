package com.yasminhosam.brandora.feature.catalogimport.data.mapper

import com.yasminhosam.brandora.feature.catalogimport.data.remote.ImportSummaryDto
import com.yasminhosam.brandora.feature.catalogimport.data.remote.InvalidRowDto
import com.yasminhosam.brandora.feature.catalogimport.data.remote.PaginationDto
import com.yasminhosam.brandora.feature.catalogimport.data.remote.ValidateResponseDto
import com.yasminhosam.brandora.feature.catalogimport.data.remote.ValidationResultsResponseDto
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ImportStatus
import com.yasminhosam.brandora.feature.catalogimport.domain.model.PageInfo
import com.yasminhosam.brandora.feature.catalogimport.domain.model.RowError
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ValidationResult
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ValidationRow
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ValidationSummary

internal fun ValidateResponseDto.toDomain(): ValidationResult = ValidationResult(
    importId = importId,
    status = ImportStatus.from(status),
    summary = summary.toDomain(),
    rows = emptyList(),
    page = PageInfo(totalCount = summary.totalRows, hasMore = false, nextOffset = null)
)

internal fun ValidationResultsResponseDto.toDomain(): ValidationResult {
    val rows = validRows.map { vr ->
        ValidationRow(rowNumber = vr.rowNumber, isValid = true, productName = vr.data.name)
    } + invalidRows.map { it.toDomain() }
    return ValidationResult(
        importId = importId,
        status = ImportStatus.from(status),
        summary = summary.toDomain(),
        rows = rows.sortedBy { it.rowNumber },
        page = pagination.toDomain()
    )
}

internal fun InvalidRowDto.toDomain(): ValidationRow = ValidationRow(
    rowNumber = rowNumber,
    isValid = false,
    errors = errors.map { RowError(field = it.field, code = it.code) }
)

internal fun ImportSummaryDto.toDomain(): ValidationSummary = ValidationSummary(
        totalRows = totalRows,
        validRows = validRows,
        invalidRows = invalidRows,
        skippedRows = skippedRows
    )

internal fun PaginationDto.toDomain(): PageInfo = PageInfo(totalCount = totalCount, hasMore = hasMore, nextOffset = nextOffset)
