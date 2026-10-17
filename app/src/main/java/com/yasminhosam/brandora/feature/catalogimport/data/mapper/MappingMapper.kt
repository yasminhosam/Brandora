package com.yasminhosam.brandora.feature.catalogimport.data.mapper

import com.yasminhosam.brandora.core.network.MappingTypeValues
import com.yasminhosam.brandora.feature.catalogimport.data.remote.ColumnSuggestionDto
import com.yasminhosam.brandora.feature.catalogimport.data.remote.ConfirmMappingRequestDto
import com.yasminhosam.brandora.feature.catalogimport.data.remote.MappingEntryDto
import com.yasminhosam.brandora.feature.catalogimport.data.remote.MappingResponseDto
import com.yasminhosam.brandora.feature.catalogimport.data.remote.SavedMappingDto
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.ColumnSuggestion
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.MappedColumn
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.MappingChoice
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.MappingPayload
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.SavedMapping
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.StandardTarget

internal fun MappingResponseDto.toDomain(): MappingPayload = MappingPayload(
    columns = columns.map { it.toDomain() },
    savedMapping = mapping?.map { it.toDomain() }
)

internal fun ColumnSuggestionDto.toDomain(): ColumnSuggestion = ColumnSuggestion(
    sourceColumn = sourceColumn,
    sourceColumnIndex = sourceColumnIndex,
    samples = samples.mapNotNull { it.toCellText() },
    suggestedType = suggestedType,
    suggestedTarget = suggestedTarget
)

internal fun SavedMappingDto.toDomain(): SavedMapping = SavedMapping(
    sourceColumn = sourceColumn,
    sourceColumnIndex = sourceColumnIndex,
    type = type,
    target = target
)

internal fun List<MappedColumn>.toRequestDto(): ConfirmMappingRequestDto =
    ConfirmMappingRequestDto(mapping = mapNotNull { it.toEntryOrNull() })

internal fun MappedColumn.toEntryOrNull(): MappingEntryDto? = when (val selection = choice) {
    is MappingChoice.Standard ->
        MappingEntryDto(sourceColumn, sourceColumnIndex, MappingTypeValues.STANDARD, selection.target.value)
    is MappingChoice.CustomAttribute ->
        MappingEntryDto(sourceColumn, sourceColumnIndex, MappingTypeValues.ATTRIBUTE, selection.key)
    MappingChoice.Ignore -> null
}
