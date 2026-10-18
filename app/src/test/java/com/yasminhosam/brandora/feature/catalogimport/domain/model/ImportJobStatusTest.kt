package com.yasminhosam.brandora.feature.catalogimport.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ImportJobStatusTest {

    private fun status(
        validRows: Int,
        inserted: Int,
        updated: Int,
        failed: Int
    ) = ImportJobStatus(
        importId = "job-1",
        status = ImportStatus.COMPLETED_WITH_ERRORS,
        totalRows = 10,
        validRows = validRows,
        invalidRows = 0,
        skippedRows = 0,
        insertedCount = inserted,
        updatedCount = updated,
        failedCount = failed
    )

    @Test
    fun `unchanged is the remainder of valid rows after insert, update and failed`() {
        val status = status(validRows = 10, inserted = 3, updated = 2, failed = 1)

        assertEquals(4, status.unchangedCount)
    }

    @Test
    fun `unchanged never goes negative when counters exceed valid rows`() {
        val status = status(validRows = 2, inserted = 3, updated = 2, failed = 1)

        assertEquals(0, status.unchangedCount)
    }

    @Test
    fun `counts expose the four buckets together`() {
        val status = status(validRows = 7, inserted = 3, updated = 2, failed = 0)

        assertEquals(ExecuteCounts(3, 2, 2, 0), status.counts)
    }
}
