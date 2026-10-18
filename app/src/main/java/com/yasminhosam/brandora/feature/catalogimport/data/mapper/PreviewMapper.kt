package com.yasminhosam.brandora.feature.catalogimport.data.mapper

import com.yasminhosam.brandora.feature.catalogimport.data.remote.PreviewInsertDto
import com.yasminhosam.brandora.feature.catalogimport.data.remote.PreviewResponseDto
import com.yasminhosam.brandora.feature.catalogimport.data.remote.PreviewSummaryDto
import com.yasminhosam.brandora.feature.catalogimport.data.remote.PreviewUpdateDto
import com.yasminhosam.brandora.feature.catalogimport.domain.model.FieldChange
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ImportStatus
import com.yasminhosam.brandora.feature.catalogimport.domain.model.PageInfo
import com.yasminhosam.brandora.feature.catalogimport.domain.model.PreviewAction
import com.yasminhosam.brandora.feature.catalogimport.domain.model.PreviewItem
import com.yasminhosam.brandora.feature.catalogimport.domain.model.PreviewResult
import com.yasminhosam.brandora.feature.catalogimport.domain.model.PreviewSummary
import com.yasminhosam.brandora.feature.catalogimport.domain.model.RowError

internal fun PreviewResponseDto.toDomain(): PreviewResult {
    val items = inserts.map { it.toDomain() } +
        updates.map { it.toDomain() } +
        unchanged.map {
            PreviewItem(
                rowNumber = it.rowNumber,
                action = PreviewAction.UNCHANGED,
                externalKey = it.externalKey,
                catalogItemId = it.catalogItemId
            )
        } +
        invalidRows.map { ir ->
            PreviewItem(
                rowNumber = ir.rowNumber,
                action = PreviewAction.INVALID,
                errors = ir.errors.map { RowError(field = it.field, code = it.code) }
            )
        }
    return PreviewResult(
        importId = importId,
        status = ImportStatus.from(status),
        summary = summary.toDomain(),
        page = pagination.toDomain(),
        items = items.sortedBy { it.rowNumber }
    )
}

internal fun PreviewInsertDto.toDomain(): PreviewItem = PreviewItem(
    rowNumber = rowNumber,
    action = PreviewAction.INSERT,
    externalKey = externalKey,
    name = data.name
)

internal fun PreviewUpdateDto.toDomain(): PreviewItem = PreviewItem(
    rowNumber = rowNumber,
    action = PreviewAction.UPDATE,
    externalKey = externalKey,
    catalogItemId = catalogItemId,
    changes = changes.map { (field, dto) ->
        FieldChange(field = field, from = dto.from?.toCellText(), to = dto.to?.toCellText())
    }
)

internal fun PreviewSummaryDto.toDomain(): PreviewSummary = PreviewSummary(
    totalRows = totalRows,
    validRows = validRows,
    invalidRows = invalidRows,
    skippedRows = skippedRows,
    willInsert = willInsert,
    willUpdate = willUpdate,
    unchanged = unchanged
)
