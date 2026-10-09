package com.yasminhosam.brandora.feature.catalogimport.data.mapper

import com.yasminhosam.brandora.core.network.ImportStatusValues
import com.yasminhosam.brandora.feature.catalogimport.data.remote.AnalyzeExcelResponseDto
import com.yasminhosam.brandora.feature.catalogimport.data.remote.SampleRowDto
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ImportStatus
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CatalogImportMapperTest {

    @Test
    fun `status strings are parsed through ApiConstants and not literals`() {
        val dto = response(status = ImportStatusValues.COMPLETED_WITH_ERRORS)

        assertEquals(ImportStatus.COMPLETED_WITH_ERRORS, dto.toDomain().status)
    }

    @Test
    fun `an unknown status maps to UNKNOWN`() {
        assertEquals(ImportStatus.UNKNOWN, response(status = "NOT_A_STATUS").toDomain().status)
    }

    @Test
    fun `mixed string and number cells become aligned text`() {
        val dto = response(
            sampleRows = listOf(
                SampleRowDto(
                    rowNumber = 2,
                    values = listOf(
                        JsonPrimitive("SKU-NEW-001"),
                        JsonPrimitive("299.99"),
                        JsonPrimitive(10)
                    )
                )
            )
        )

        val row = dto.toDomain().sampleRows.single()

        assertEquals(2, row.rowNumber)
        assertEquals(listOf("SKU-NEW-001", "299.99", "10"), row.values)
    }

    @Test
    fun `an explicit json null keeps its column position as a kotlin null`() {
        val dto = response(
            sampleRows = listOf(
                SampleRowDto(rowNumber = 3, values = listOf(JsonNull, JsonPrimitive("Cotton")))
            )
        )

        val values = dto.toDomain().sampleRows.single().values

        assertEquals(2, values.size)
        assertNull(values[0])
        assertEquals("Cotton", values[1])
    }

    @Test
    fun `every analyze field is carried across the mapping`() {
        val domain = response(
            importJobId = "job-42",
            sheetNames = listOf("Products", "Variants"),
            selectedSheetName = "Variants",
            headerRow = 3,
            headers = listOf("SKU", "Name"),
            totalRows = 99
        ).toDomain()

        assertEquals("job-42", domain.importJobId)
        assertEquals(listOf("Products", "Variants"), domain.sheetNames)
        assertEquals("Variants", domain.selectedSheetName)
        assertEquals(3, domain.headerRow)
        assertEquals(listOf("SKU", "Name"), domain.headers)
        assertEquals(99, domain.totalRows)
    }

    private fun response(
        importJobId: String = "job-1",
        status: String = ImportStatusValues.MAPPING_REQUIRED,
        sheetNames: List<String> = listOf("Products"),
        selectedSheetName: String? = "Products",
        headerRow: Int = 1,
        headers: List<String> = listOf("SKU", "Name"),
        sampleRows: List<SampleRowDto> = emptyList(),
        totalRows: Int = 7
    ) = AnalyzeExcelResponseDto(
        importJobId = importJobId,
        status = status,
        sheetNames = sheetNames,
        selectedSheetName = selectedSheetName,
        headerRow = headerRow,
        headers = headers,
        sampleRows = sampleRows,
        totalRows = totalRows
    )
}
