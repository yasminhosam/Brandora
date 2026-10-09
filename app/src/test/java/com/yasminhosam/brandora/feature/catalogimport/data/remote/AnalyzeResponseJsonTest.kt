package com.yasminhosam.brandora.feature.catalogimport.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyzeResponseJsonTest {

    // Keep in sync with NetworkModule.provideJson().
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Test
    fun `the documented analyze payload parses field by field`() {
        val parsed = json.decodeFromString<AnalyzeExcelResponseDto>(FIXTURE)

        assertEquals("7dc471e1-0a82-44f3-9906-d7e0d0aefc39", parsed.importJobId)
        assertEquals("MAPPING_REQUIRED", parsed.status)
        assertEquals(listOf("Products"), parsed.sheetNames)
        assertEquals("Products", parsed.selectedSheetName)
        assertEquals(1, parsed.headerRow)
        assertEquals(7, parsed.totalRows)

        assertEquals(
            listOf("SKU", "Product Name", "Description", "Category", "Price", "Quantity", "Material"),
            parsed.headers
        )

        val row = parsed.sampleRows.single()
        assertEquals(2, row.rowNumber)
        assertEquals(
            listOf("SKU-NEW-001", "T-Shirt", "New product", "Shirts", "299.99", "10", "Cotton"),
            row.values.map { it.jsonPrimitive.contentOrNull }
        )
    }

    @Test
    fun `an unknown extra field does not break parsing`() {
        val withExtraField =
            """{ "importJobId": "id-1", "status": "READY", "somethingNew": { "nested": [1, 2] } }"""

        val parsed = json.decodeFromString<AnalyzeExcelResponseDto>(withExtraField)

        assertEquals("id-1", parsed.importJobId)
        assertEquals("READY", parsed.status)
    }

    @Test
    fun `omitted optional fields fall back to defaults`() {
        val parsed = json.decodeFromString<AnalyzeExcelResponseDto>(
            """{ "importJobId": "id-1", "status": "READY" }"""
        )

        assertEquals("id-1", parsed.importJobId)
        assertEquals("READY", parsed.status)
        assertTrue(parsed.sheetNames.isEmpty())
        assertTrue(parsed.headers.isEmpty())
        assertTrue(parsed.sampleRows.isEmpty())
        assertEquals(0, parsed.headerRow)
        assertEquals(0, parsed.totalRows)
        assertNull(parsed.selectedSheetName)
    }

    private companion object {
        val FIXTURE = """
            {
              "importJobId": "7dc471e1-0a82-44f3-9906-d7e0d0aefc39",
              "status": "MAPPING_REQUIRED",
              "sheetNames": [
                "Products"
              ],
              "selectedSheetName": "Products",
              "headerRow": 1,
              "headers": [
                "SKU",
                "Product Name",
                "Description",
                "Category",
                "Price",
                "Quantity",
                "Material"
              ],
              "sampleRows": [
                {
                  "rowNumber": 2,
                  "values": [
                    "SKU-NEW-001",
                    "T-Shirt",
                    "New product",
                    "Shirts",
                    "299.99",
                    10,
                    "Cotton"
                  ]
                }
              ],
              "totalRows": 7
            }
        """.trimIndent()
    }
}
