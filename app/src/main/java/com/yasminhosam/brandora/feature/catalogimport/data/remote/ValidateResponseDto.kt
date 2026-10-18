package com.yasminhosam.brandora.feature.catalogimport.data.remote
import kotlinx.serialization.Serializable

@Serializable
internal data class ValidateResponseDto(
    val importId: String,
    val status: String,
    val summary: ImportSummaryDto
)