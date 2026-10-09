package com.yasminhosam.brandora.feature.catalogimport.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Cells are heterogeneous on the wire ("299.99" and 10 in one array), so values stay
 * [JsonElement] until the mapper converts them to text.
 */
@Serializable
internal data class SampleRowDto(
    val rowNumber: Int = 0,
    val values: List<JsonElement> = emptyList()
)
