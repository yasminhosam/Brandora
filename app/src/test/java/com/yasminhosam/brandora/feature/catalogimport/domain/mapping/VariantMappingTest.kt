package com.yasminhosam.brandora.feature.catalogimport.domain.mapping

import com.yasminhosam.brandora.feature.catalogimport.data.mapper.toRequestDto
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class VariantMappingTest {
    private fun required() =
        listOf(
            MappedColumn("Name", 1, emptyList(), MappingChoice.Standard(StandardTarget.NAME)),
            MappedColumn(
                "Product Code",
                2,
                emptyList(),
                MappingChoice.Standard(StandardTarget.EXTERNAL_KEY),
            ),
        )

    @Test
    fun `grouping code is required before confirmation`() {
        val result = validateMapping(required().take(1)) as MappingValidationResult.Invalid
        assertTrue(result.reasons.contains(MappingInvalidReason.ExternalKeyMissing))
    }

    @Test
    fun `variant choices serialize separately from product attributes`() {
        val columns =
            required() +
                MappedColumn("Material", 3, emptyList(), MappingChoice.VariantAttribute("الخامة")) +
                MappedColumn("Note", 4, emptyList(), MappingChoice.CustomAttribute("note"))
        val dto = columns.toRequestDto()
        assertEquals("VARIANT_ATTRIBUTE", dto.mapping[2].type)
        assertEquals("الخامة", dto.mapping[2].target)
        assertEquals("ATTRIBUTE", dto.mapping[3].type)
        assertEquals(MappingValidationResult.Valid, validateMapping(columns))
    }

    @Test
    fun `Arabic option names allowed but whitespace and long names rejected`() {
        assertEquals(
            MappingValidationResult.Valid,
            validateMapping(
                required() +
                    MappedColumn("A", 3, emptyList(), MappingChoice.VariantAttribute("الوزن"))
            ),
        )
        for (key in listOf(" x ", "x".repeat(61), "__proto__")) {
            assertTrue(
                validateMapping(
                    required() +
                        MappedColumn("A", 3, emptyList(), MappingChoice.VariantAttribute(key))
                )
                    is MappingValidationResult.Invalid
            )
        }
    }

    @Test
    fun `duplicates rejected within type while same key across types is intentional`() {
        val option = MappedColumn("A", 3, emptyList(), MappingChoice.VariantAttribute("material"))
        assertTrue(
            validateMapping(
                required() + option + option.copy(sourceColumn = "B", sourceColumnIndex = 4)
            )
                is MappingValidationResult.Invalid
        )
        assertEquals(
            MappingValidationResult.Valid,
            validateMapping(
                required() +
                    option +
                    MappedColumn("B", 4, emptyList(), MappingChoice.CustomAttribute("material"))
            ),
        )
    }

    @Test
    fun `saved variant choices and new standard targets survive reconstruction`() {
        val suggestions =
            listOf(
                ColumnSuggestion("Material", 1, listOf("Cotton"), "VARIANT_ATTRIBUTE", "material"),
                ColumnSuggestion("SKU", 2, listOf("S-1"), "STANDARD", "sku"),
                ColumnSuggestion("Price", 3, listOf("0.00"), "STANDARD", "unitPrice"),
            )
        val initial = initialColumns(suggestions, null)
        assertEquals(MappingChoice.VariantAttribute("material"), initial[0].choice)
        assertEquals(MappingChoice.Standard(StandardTarget.SKU), initial[1].choice)
        assertEquals(MappingChoice.Standard(StandardTarget.UNIT_PRICE), initial[2].choice)
        val saved =
            initialColumns(
                suggestions,
                listOf(SavedMapping("Material", 1, "VARIANT_ATTRIBUTE", "الخامة")),
            )
        assertEquals(MappingChoice.VariantAttribute("الخامة"), saved[0].choice)
    }
}
