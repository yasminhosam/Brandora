package com.yasminhosam.brandora.feature.catalogimport.data.mapper

import com.yasminhosam.brandora.feature.catalogimport.data.remote.ColumnSuggestionDto
import com.yasminhosam.brandora.feature.catalogimport.data.remote.MappingResponseDto
import com.yasminhosam.brandora.feature.catalogimport.data.remote.SavedMappingDto
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.MappedColumn
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.MappingChoice
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.StandardTarget
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MappingMapperTest {

    @Test
    fun `mixed sample types become text and nulls are dropped`() {
        val dto = MappingResponseDto(
            importId = "job-1",
            status = "MAPPING_REQUIRED",
            columns = listOf(
                ColumnSuggestionDto(
                    sourceColumn = "Item Name",
                    sourceColumnIndex = 1,
                    samples = listOf(JsonPrimitive("Shirt"), JsonPrimitive(10), JsonPrimitive(true), JsonNull),
                    suggestedType = "STANDARD",
                    suggestedTarget = "name"
                )
            )
        )

        val payload = dto.toDomain()

        assertEquals(listOf("Shirt", "10", "true"), payload.columns.single().samples)
    }

    @Test
    fun `saved entries map field by field`() {
        val dto = MappingResponseDto(
            importId = "job-1",
            status = "MAPPING_REQUIRED",
            mapping = listOf(
                SavedMappingDto("Item Name", 1, "STANDARD", "name"),
                SavedMappingDto("Size", 2, "ATTRIBUTE", "size")
            )
        )

        val saved = dto.toDomain().savedMapping

        assertEquals("Item Name", saved?.get(0)?.sourceColumn)
        assertEquals(1, saved?.get(0)?.sourceColumnIndex)
        assertEquals("STANDARD", saved?.get(0)?.type)
        assertEquals("name", saved?.get(0)?.target)
        assertEquals("size", saved?.get(1)?.target)
    }

    @Test
    fun `request drops ignored columns and keeps indices`() {
        val request = listOf(
            MappedColumn("Item Name", 1, emptyList(), MappingChoice.Standard(StandardTarget.NAME)),
            MappedColumn("Fabric", 2, emptyList(), MappingChoice.CustomAttribute("fabric")),
            MappedColumn("Notes", 3, emptyList(), MappingChoice.Ignore)
        ).toRequestDto()

        assertEquals(2, request.mapping.size)
        assertEquals("Item Name", request.mapping[0].sourceColumn)
        assertEquals(1, request.mapping[0].sourceColumnIndex)
        assertEquals("STANDARD", request.mapping[0].type)
        assertEquals("name", request.mapping[0].target)
        assertEquals("ATTRIBUTE", request.mapping[1].type)
        assertEquals("fabric", request.mapping[1].target)
        assertTrue(request.mapping.none { it.sourceColumn == "Notes" })
    }

    @Test
    fun `standard choice entry is never null-targeted`() {
        val entry = MappedColumn(
            "Price", 2, emptyList(), MappingChoice.Standard(StandardTarget.BASE_PRICE)
        ).toEntryOrNull()

        assertEquals("basePrice", entry?.target)
    }

    @Test
    fun `ignore choice maps to null entry`() {
        assertNull(
            MappedColumn("Notes", 3, emptyList(), MappingChoice.Ignore).toEntryOrNull()
        )
    }
}
