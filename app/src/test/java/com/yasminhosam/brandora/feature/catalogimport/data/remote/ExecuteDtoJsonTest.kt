package com.yasminhosam.brandora.feature.catalogimport.data.remote

import com.yasminhosam.brandora.feature.catalogimport.data.mapper.toDomain
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ImportStatus
import com.yasminhosam.brandora.feature.catalogimport.domain.model.PreviewAction
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExecuteDtoJsonTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Test
    fun `execute response parses counts and separates invalid from failed rows`() {
        val dto = json.decodeFromString<ExecuteResponseDto>(EXECUTE_JSON)

        assertEquals(ImportStatus.COMPLETED_WITH_ERRORS, ImportStatus.from(dto.status))
        assertEquals(1, dto.summary.insertedCount)
        assertEquals(1, dto.summary.unchangedCount)
        assertEquals(1, dto.invalidRows.size)
        assertEquals(1, dto.failedRows.size)
        assertEquals("basePrice", dto.invalidRows.single().errors.single().field)
        assertEquals("EXECUTION_CONFLICT", dto.failedRows.single().code)
    }

    @Test
    fun `job status parses the flat contract and tolerates a null sheet`() {
        val status = json.decodeFromString<JobStatusDto>(STATUS_JSON)

        assertEquals("e5b31599-e582-4a36-966b-dda8389e2fb0", status.id)
        assertEquals(3, status.insertedCount)
        assertNull(status.sheetName)
    }

    @Test
    fun `job status derives unchanged from valid minus the other buckets`() {
        val status = json.decodeFromString<JobStatusDto>(STATUS_JSON).toDomain()

        // valid 5 - inserted 3 - updated 1 - failed 1 = 0 unchanged
        assertEquals(0, status.unchangedCount)
    }

    @Test
    fun `preview maps field changes to display text and sorts by row`() {
        val result = json.decodeFromString<PreviewResponseDto>(PREVIEW_JSON).toDomain()

        // Insert, update, unchanged and invalid merge into one rowNumber-sorted window.
        assertEquals(listOf(2, 3, 4, 5), result.items.map { it.rowNumber })
        val update = result.items.first { it.action == PreviewAction.UPDATE }
        assertEquals("299.99", update.changes.first().from)
        assertEquals("249.99", update.changes.first().to)
        assertTrue(result.items.first { it.action == PreviewAction.INVALID }.errors.isNotEmpty())
    }

    private companion object {
        const val EXECUTE_JSON = """
            {
              "importId": "job-1",
              "status": "COMPLETED_WITH_ERRORS",
              "summary": { "totalRows": 4, "validRows": 3, "invalidRows": 1, "skippedRows": 0,
                           "insertedCount": 1, "updatedCount": 1, "failedCount": 0, "unchangedCount": 1 },
              "inserted": [ { "rowNumber": 2, "externalKey": "SKU-001" } ],
              "updated": [ { "rowNumber": 3, "externalKey": "SKU-002" } ],
              "unchanged": [ { "rowNumber": 4, "externalKey": "SKU-003" } ],
              "failedRows": [ { "rowNumber": 6, "code": "EXECUTION_CONFLICT", "message": "Catalog data changed concurrently" } ],
              "invalidRows": [ { "rowNumber": 5, "errors": [ { "field": "basePrice", "code": "INVALID_DECIMAL" } ] } ]
            }
        """

        const val STATUS_JSON = """
            {
              "id": "e5b31599-e582-4a36-966b-dda8389e2fb0",
              "businessId": "f6bc7287-3285-4e19-a819-459c41f0a138",
              "status": "COMPLETED_WITH_ERRORS",
              "fileName": "Products.xlsx",
              "sheetName": null,
              "headerRow": 3,
              "totalRows": 5, "validRows": 5, "invalidRows": 0, "skippedRows": 0,
              "insertedCount": 3, "updatedCount": 1, "failedCount": 1
            }
        """

        const val PREVIEW_JSON = """
            {
              "importId": "job-1",
              "status": "READY",
              "summary": { "willInsert": 1, "willUpdate": 1, "unchanged": 1, "invalidRows": 1 },
              "pagination": { "limit": 50, "offset": 0, "totalCount": 4, "hasMore": false, "nextOffset": null },
              "inserts": [ { "rowNumber": 2, "externalKey": "SKU-001", "data": { "name": "Shirt" } } ],
              "updates": [ { "rowNumber": 3, "externalKey": "SKU-002",
                             "changes": { "basePrice": { "from": "299.99", "to": "249.99" } } } ],
              "unchanged": [ { "rowNumber": 4, "externalKey": "SKU-003" } ],
              "invalidRows": [ { "rowNumber": 5, "errors": [ { "field": "name", "code": "REQUIRED" } ] } ]
            }
        """
    }
}
