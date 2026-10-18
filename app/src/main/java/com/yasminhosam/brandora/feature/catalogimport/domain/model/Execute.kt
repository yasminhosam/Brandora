package com.yasminhosam.brandora.feature.catalogimport.domain.model

data class ExecuteCounts(
    val inserted: Int,
    val updated: Int,
    val unchanged: Int,
    val failed: Int
)

/** A valid row that failed during execution. Localized by the job-level [code]. */
data class FailedRow(
    val rowNumber: Int,
    val code: String
)

data class ExecuteResult(
    val importId: String,
    val status: ImportStatus,
    val counts: ExecuteCounts,
    val invalidRows: List<ValidationRow>,
    val failedRows: List<FailedRow>
)

/** Compact persisted job status from GET /imports/{importId}; used for recovery after an interruption. */
data class ImportJobStatus(
    val importId: String,
    val status: ImportStatus,
    val totalRows: Int,
    val validRows: Int,
    val invalidRows: Int,
    val skippedRows: Int,
    val insertedCount: Int,
    val updatedCount: Int,
    val failedCount: Int
) {
    /** The status endpoint omits unchangedCount; valid rows are insert + update + unchanged + failed. */
    val unchangedCount: Int
        get() = (validRows - insertedCount - updatedCount - failedCount).coerceAtLeast(0)

    val counts: ExecuteCounts
        get() = ExecuteCounts(insertedCount, updatedCount, unchangedCount, failedCount)
}

/**
 * Outcome of running the import. [Performed] means Execute returned a terminal result with row
 * detail; [Recovered] means Execute could not confirm (409 / 500 / timeout) and the persisted job
 * status was re-read instead — row detail is unavailable, only counts.
 */
sealed interface ExecuteOutcome {
    data class Performed(val result: ExecuteResult) : ExecuteOutcome
    data class Recovered(val status: ImportJobStatus) : ExecuteOutcome
}
