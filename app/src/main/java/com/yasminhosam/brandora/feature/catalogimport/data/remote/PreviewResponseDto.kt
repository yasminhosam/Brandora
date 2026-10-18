package com.yasminhosam.brandora.feature.catalogimport.data.remote
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class PreviewSummaryDto(
    val totalRows: Int = 0,
    val validRows: Int = 0,
    val invalidRows: Int = 0,
    val skippedRows: Int = 0,
    val willInsert: Int = 0,
    val willUpdate: Int = 0,
    val unchanged: Int = 0
)

@Serializable
internal data class PreviewInsertDto(
    val rowNumber: Int,
    val externalKey: String? = null,
    val data: RowDataDto = RowDataDto()
)

/** One changed field. Values vary by field (string, number, null or the attributes object). */
@Serializable
internal data class FieldChangeDto(
    val from: JsonElement? = null,
    val to: JsonElement? = null
)

@Serializable
internal data class PreviewUpdateDto(
    val rowNumber: Int,
    val externalKey: String? = null,
    val catalogItemId: String? = null,
    // Keys: name, description, category, basePrice, stockQuantity, attributes, ...
    val changes: Map<String, FieldChangeDto> = emptyMap()
)

@Serializable
internal data class PreviewUnchangedDto(
    val rowNumber: Int,
    val externalKey: String? = null,
    val catalogItemId: String? = null
)

@Serializable
internal data class PreviewResponseDto(
    val importId: String,
    val status: String,
    val summary: PreviewSummaryDto = PreviewSummaryDto(),
    val pagination: PaginationDto = PaginationDto(),
    val inserts: List<PreviewInsertDto> = emptyList(),
    val updates: List<PreviewUpdateDto> = emptyList(),
    val unchanged: List<PreviewUnchangedDto> = emptyList(),
    val invalidRows: List<InvalidRowDto> = emptyList()
)