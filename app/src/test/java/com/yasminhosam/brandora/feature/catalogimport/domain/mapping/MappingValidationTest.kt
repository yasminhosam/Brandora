package com.yasminhosam.brandora.feature.catalogimport.domain.mapping

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MappingValidationTest {

    private fun validateWithCode(columns: List<MappedColumn>): MappingValidationResult =
        validateMapping(
            if (columns.isEmpty()) columns
            else
                columns +
                    MappedColumn(
                        "Product Code",
                        99,
                        emptyList(),
                        MappingChoice.Standard(StandardTarget.EXTERNAL_KEY),
                    )
        )

    private fun column(
        name: String = "Item Name",
        index: Int = 1,
        choice: MappingChoice = MappingChoice.Standard(StandardTarget.NAME),
    ) = MappedColumn(name, index, emptyList(), choice)

    private fun invalidReasonsOf(result: MappingValidationResult): List<MappingInvalidReason> =
        (result as MappingValidationResult.Invalid).reasons

    @Test
    fun `a fully mapped sheet is valid`() {
        val result =
            validateWithCode(
                listOf(
                    column(choice = MappingChoice.Standard(StandardTarget.NAME)),
                    column("Price", 2, MappingChoice.Standard(StandardTarget.BASE_PRICE)),
                    column("Fabric", 3, MappingChoice.CustomAttribute("fabric")),
                    column("Notes", 4, MappingChoice.Ignore),
                )
            )

        assertEquals(MappingValidationResult.Valid, result)
    }

    @Test
    fun `an empty sheet is invalid`() {
        assertEquals(
            listOf(MappingInvalidReason.EmptySheet),
            invalidReasonsOf(validateWithCode(emptyList())),
        )
    }

    @Test
    fun `a missing name blocks confirm`() {
        val reasons =
            invalidReasonsOf(validateWithCode(listOf(column(choice = MappingChoice.Ignore))))

        assertTrue(reasons.contains(MappingInvalidReason.NameMissing))
    }

    @Test
    fun `a standard target used twice is reported`() {
        val reasons =
            invalidReasonsOf(
                validateWithCode(
                    listOf(
                        column("A", 1, MappingChoice.Standard(StandardTarget.NAME)),
                        column("B", 2, MappingChoice.Standard(StandardTarget.NAME)),
                    )
                )
            )

        assertTrue(
            reasons.contains(MappingInvalidReason.StandardTargetUsedTwice(StandardTarget.NAME))
        )
    }

    @Test
    fun `an empty custom key blocks confirm without a format error`() {
        val result =
            validateWithCode(
                listOf(
                    column(choice = MappingChoice.Standard(StandardTarget.NAME)),
                    column("Fabric", 2, MappingChoice.CustomAttribute("")),
                )
            )

        assertEquals(listOf(MappingInvalidReason.EmptyCustomKey(1)), invalidReasonsOf(result))
    }

    @Test
    fun `uppercase, arabic, leading digit and overlong keys are rejected`() {
        listOf("Fabric", "قماش", "1fabric", "a".repeat(65), "_fabric").forEach { key ->
            val reasons =
                invalidReasonsOf(
                    validateWithCode(
                        listOf(
                            column(choice = MappingChoice.Standard(StandardTarget.NAME)),
                            column("Fabric", 2, MappingChoice.CustomAttribute(key)),
                        )
                    )
                )

            assertEquals(listOf(MappingInvalidReason.BadCustomKey(1)), reasons)
        }
    }

    @Test
    fun `reserved prototype keys are rejected`() {
        listOf("constructor", "prototype", "__proto__").forEach { key ->
            val reasons =
                invalidReasonsOf(
                    validateWithCode(
                        listOf(
                            column(choice = MappingChoice.Standard(StandardTarget.NAME)),
                            column("Fabric", 2, MappingChoice.CustomAttribute(key)),
                        )
                    )
                )

            assertEquals(listOf(MappingInvalidReason.BadCustomKey(1)), reasons)
        }
    }

    @Test
    fun `a 64 character key is accepted`() {
        val result =
            validateWithCode(
                listOf(
                    column(choice = MappingChoice.Standard(StandardTarget.NAME)),
                    column("Fabric", 2, MappingChoice.CustomAttribute("a".repeat(64))),
                )
            )

        assertEquals(MappingValidationResult.Valid, result)
    }

    @Test
    fun `a custom key used twice flags only the later column`() {
        val reasons =
            invalidReasonsOf(
                validateWithCode(
                    listOf(
                        column(choice = MappingChoice.Standard(StandardTarget.NAME)),
                        column("A", 2, MappingChoice.CustomAttribute("fabric")),
                        column("B", 3, MappingChoice.CustomAttribute("fabric")),
                    )
                )
            )

        assertEquals(listOf(MappingInvalidReason.CustomKeyUsedTwice(2)), reasons)
    }

    @Test
    fun `a custom key may equal a standard target name`() {
        val result =
            validateWithCode(
                listOf(
                    column(choice = MappingChoice.Standard(StandardTarget.NAME)),
                    column("Label", 2, MappingChoice.CustomAttribute("name")),
                )
            )

        assertEquals(MappingValidationResult.Valid, result)
    }

    @Test
    fun `two untouched custom fields do not count as duplicates`() {
        val result =
            validateWithCode(
                listOf(
                    column(choice = MappingChoice.Standard(StandardTarget.NAME)),
                    column("A", 2, MappingChoice.CustomAttribute("")),
                    column("B", 3, MappingChoice.CustomAttribute("")),
                )
            )

        val reasons = invalidReasonsOf(result)
        assertTrue(reasons.none { it is MappingInvalidReason.CustomKeyUsedTwice })
    }

    @Test
    fun `a missing external key blocks confirm`() {
        val reasons =
            invalidReasonsOf(
                validateMapping(
                    listOf(
                        column(choice = MappingChoice.Standard(StandardTarget.NAME)),
                        column("Price", 2, MappingChoice.Standard(StandardTarget.BASE_PRICE)),
                    )
                )
            )

        assertEquals(listOf(MappingInvalidReason.ExternalKeyMissing), reasons)
    }

    @Test
    fun `name and external key mapped without a synthetic code column is valid`() {
        val result =
            validateMapping(
                listOf(
                    column(choice = MappingChoice.Standard(StandardTarget.NAME)),
                    column("Code", 2, MappingChoice.Standard(StandardTarget.EXTERNAL_KEY)),
                )
            )

        assertEquals(MappingValidationResult.Valid, result)
    }

    @Test
    fun `missing name and missing external key are both reported`() {
        val reasons =
            invalidReasonsOf(
                validateMapping(
                    listOf(column("Price", 1, MappingChoice.Standard(StandardTarget.BASE_PRICE)))
                )
            )

        assertEquals(
            listOf(MappingInvalidReason.NameMissing, MappingInvalidReason.ExternalKeyMissing),
            reasons,
        )
    }

    @Test
    fun `normalize trims custom and variant keys and leaves other choices untouched`() {
        val columns =
            listOf(
                column(choice = MappingChoice.Standard(StandardTarget.NAME)),
                column("Fabric", 2, MappingChoice.CustomAttribute("  fabric  ")),
                column("Size", 3, MappingChoice.VariantAttribute(" size")),
                column("Notes", 4, MappingChoice.Ignore),
            )

        val normalized = normalizeMapping(columns)

        assertEquals("fabric", (normalized[1].choice as MappingChoice.CustomAttribute).key)
        assertEquals("size", (normalized[2].choice as MappingChoice.VariantAttribute).key)
        assertEquals(columns[0], normalized[0])
        assertEquals(columns[3], normalized[3])
        // Idempotent: a second pass changes nothing.
        assertEquals(normalized, normalizeMapping(normalized))
    }

    @Test
    fun `normalize lets a padded key validate that the raw key would reject`() {
        val columns =
            listOf(
                column(choice = MappingChoice.Standard(StandardTarget.NAME)),
                column("Code", 2, MappingChoice.Standard(StandardTarget.EXTERNAL_KEY)),
                column("Fabric", 3, MappingChoice.CustomAttribute(" fabric ")),
            )

        assertEquals(
            listOf(MappingInvalidReason.BadCustomKey(2)),
            invalidReasonsOf(validateMapping(columns)),
        )
        assertEquals(MappingValidationResult.Valid, validateMapping(normalizeMapping(columns)))
    }
}
