package com.yasminhosam.brandora.feature.catalogimport.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// ───────── Shared ─────────

@Serializable
internal data class ImportSummaryDto(
    val totalRows: Int = 0,
    val validRows: Int = 0,
    val invalidRows: Int = 0,
    val skippedRows: Int = 0
)

@Serializable
internal data class PaginationDto(
    val limit: Int = 0,
    val offset: Int = 0,
    val totalCount: Int = 0,
    val hasMore: Boolean = false,
    // Null on the last page.
    val nextOffset: Int? = null
)

@Serializable
internal data class RowErrorDto(
    val field: String = "",
    val code: String = "",
    // English backend message; the app localizes by [code] instead.
    val message: String? = null
)

@Serializable
internal data class InvalidRowDto(
    val rowNumber: Int,
    val errors: List<RowErrorDto> = emptyList()
)

/**
 * Normalized row data. Every field is optional because the shape depends on the confirmed
 * mapping: unmapped fields are absent and mapped blanks are null.
 */
@Serializable
internal data class RowDataDto(
    val externalKey: String? = null,
    val name: String? = null,
    val description: String? = null,
    val category: String? = null,
    // Decimal string such as "299.99"; null when the cell was blank.
    val basePrice: String? = null,
    // Zero is a valid value and differs from null.
    val stockQuantity: Int? = null,
    // Dynamic keys (e.g. "material"); values may be string, number or null.
    val attributes: Map<String, JsonElement>? = null
)