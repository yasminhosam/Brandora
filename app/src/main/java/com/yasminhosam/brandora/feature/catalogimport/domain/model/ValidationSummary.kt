package com.yasminhosam.brandora.feature.catalogimport.domain.model

data class ValidationSummary(
    val totalRows: Int,
    val validRows: Int,
    val invalidRows: Int,
    val skippedRows: Int
)

/** One failing field on an invalid row. Localized by [code]; the backend [message] is never shown. */
data class RowError(
    val field: String,
    val code: String
)

data class ValidationRow(
    val rowNumber: Int,
    val isValid: Boolean,
    /** Non-null for valid rows: the product name from the mapped column. */
    val productName: String? = null,
    /** Non-null for invalid rows: one entry per failing field. */
    val errors: List<RowError> = emptyList()
)

/** Cursor for a paged list. [nextOffset] is null on the last page. */
data class PageInfo(
    val totalCount: Int,
    val hasMore: Boolean,
    val nextOffset: Int?
)

data class ValidationResult(
    val importId: String,
    val status: ImportStatus,
    val summary: ValidationSummary,
    val rows: List<ValidationRow>,
    val page: PageInfo
)
