package com.yasminhosam.brandora.feature.catalogimport.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class MappingResponseDto(
    val importId: String,
    val status: String,
    val columns: List<ColumnSuggestionDto> = emptyList(),
    val mapping: List<SavedMappingDto>? = null
)

@Serializable
internal data class ColumnSuggestionDto(
    val sourceColumn: String = "",
    val sourceColumnIndex: Int = 0,
    val samples: List<JsonElement> = emptyList(),
    val suggestedType: String = "UNMAPPED",
    val suggestedTarget: String? = null
)

@Serializable
internal data class SavedMappingDto(
    val sourceColumn: String = "",
    val sourceColumnIndex: Int = 0,
    val type: String = "UNMAPPED",
    val target: String? = null
)

@Serializable
internal data class ConfirmMappingRequestDto(
    val mapping: List<MappingEntryDto>
)

@Serializable
internal data class MappingEntryDto(
    val sourceColumn: String,
    val sourceColumnIndex: Int,
    val type: String,
    val target: String? = null
)

@Serializable
internal data class ConfirmMappingResponseDto(
    val importId: String,
    val status: String,
    val mapping: List<SavedMappingDto> = emptyList()
)
