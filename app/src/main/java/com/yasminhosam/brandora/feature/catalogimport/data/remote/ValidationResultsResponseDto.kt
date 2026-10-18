package com.yasminhosam.brandora.feature.catalogimport.data.remote

import kotlinx.serialization.Serializable

@Serializable
internal data class ValidRowDto(
    val rowNumber: Int,
    val data: RowDataDto = RowDataDto()
)

@Serializable
internal data class ValidationResultsResponseDto(
    val importId: String,
    val status: String,
    val summary: ImportSummaryDto = ImportSummaryDto(),
    val pagination: PaginationDto = PaginationDto(),
    val validRows: List<ValidRowDto> = emptyList(),
    val invalidRows: List<InvalidRowDto> = emptyList()
)