package com.yasminhosam.brandora.feature.catalogimport.domain.model

enum class PreviewAction { INSERT, UPDATE, UNCHANGED, INVALID }

/** One changed field on an update. Values are display text; null means blank on that side. */
data class FieldChange(
    val field: String,
    val from: String?,
    val to: String?
)

/** A single row in the preview window, tagged with the action it will take. */
data class PreviewItem(
    val rowNumber: Int,
    val action: PreviewAction,
    val externalKey: String? = null,
    /** For inserts: the resolved product name. */
    val name: String? = null,
    /** For updates and unchanged rows: the matched catalog item. */
    val catalogItemId: String? = null,
    /** For updates: the fields that will change. */
    val changes: List<FieldChange> = emptyList(),
    /** For invalid rows: one entry per failing field. */
    val errors: List<RowError> = emptyList()
)

data class PreviewSummary(
    val totalRows: Int,
    val validRows: Int,
    val invalidRows: Int,
    val skippedRows: Int,
    val willInsert: Int,
    val willUpdate: Int,
    val unchanged: Int
)

/**
 * One page of the preview. [items] merges inserts, updates, unchanged and invalid rows into a
 * single rowNumber-sorted window. The preview is advisory and recomputed on each request.
 */
data class PreviewResult(
    val importId: String,
    val status: ImportStatus,
    val summary: PreviewSummary,
    val page: PageInfo,
    val items: List<PreviewItem>
)
