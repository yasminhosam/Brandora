package com.yasminhosam.brandora.feature.catalogimport.data.mapper

import com.yasminhosam.brandora.feature.catalogimport.data.remote.AnalyzeExcelResponseDto
import com.yasminhosam.brandora.feature.catalogimport.data.remote.SampleRowDto
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ImportAnalysis
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ImportStatus
import com.yasminhosam.brandora.feature.catalogimport.domain.model.SampleRow
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

internal fun AnalyzeExcelResponseDto.toDomain(): ImportAnalysis = ImportAnalysis(
    importJobId = importJobId,
    status = ImportStatus.from(status),
    sheetNames = sheetNames,
    selectedSheetName = selectedSheetName,
    headerRow = headerRow,
    headers = headers,
    sampleRows = sampleRows.map { it.toDomain() },
    totalRows = totalRows
)

internal fun SampleRowDto.toDomain(): SampleRow = SampleRow(
    rowNumber = rowNumber,
    values = values.map { it.toCellText() }
)

/**
 * Mixed wire types become text; explicit JSON nulls stay null so columns keep their positions.
 */
internal fun JsonElement.toCellText(): String? = when (this) {
    is JsonNull -> null
    is JsonPrimitive -> contentOrNull
    // Cells are primitives in practice; render anything else rather than lose the column.
    else -> toString()
}
