package com.yasminhosam.brandora.feature.catalogimport.domain.mapping

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MappingValidationTest {

    private fun column(
        name: String = "Item Name",
        index: Int = 1,
        choice: MappingChoice = MappingChoice.Standard(StandardTarget.NAME)
    ) = MappedColumn(name, index, emptyList(), choice)

    private fun invalidReasonsOf(result: MappingValidationResult): List<MappingInvalidReason> =
        (result as MappingValidationResult.Invalid).reasons

    @Test
    fun `a fully mapped sheet is valid`() {
        val result = validateMapping(
            listOf(
                column(choice = MappingChoice.Standard(StandardTarget.NAME)),
                column("Price", 2, MappingChoice.Standard(StandardTarget.BASE_PRICE)),
                column("Fabric", 3, MappingChoice.CustomAttribute("fabric")),
                column("Notes", 4, MappingChoice.Ignore)
            )
        )

        assertEquals(MappingValidationResult.Valid, result)
    }

    @Test
    fun `an empty sheet is invalid`() {
        assertEquals(
            listOf(MappingInvalidReason.EmptySheet),
            invalidReasonsOf(validateMapping(emptyList()))
        )
    }

    @Test
    fun `a missing name blocks confirm`() {
        val reasons = invalidReasonsOf(
            validateMapping(listOf(column(choice = MappingChoice.Ignore)))
        )

        assertTrue(reasons.contains(MappingInvalidReason.NameMissing))
    }

    @Test
    fun `a standard target used twice is reported`() {
        val reasons = invalidReasonsOf(
            validateMapping(
                listOf(
                    column("A", 1, MappingChoice.Standard(StandardTarget.NAME)),
                    column("B", 2, MappingChoice.Standard(StandardTarget.NAME))
                )
            )
        )

        assertTrue(reasons.contains(MappingInvalidReason.StandardTargetUsedTwice(StandardTarget.NAME)))
    }

    @Test
    fun `an empty custom key blocks confirm without a format error`() {
        val result = validateMapping(
            listOf(
                column(choice = MappingChoice.Standard(StandardTarget.NAME)),
                column("Fabric", 2, MappingChoice.CustomAttribute(""))
            )
        )

        assertEquals(listOf(MappingInvalidReason.EmptyCustomKey(1)), invalidReasonsOf(result))
    }

    @Test
    fun `uppercase, arabic, leading digit and overlong keys are rejected`() {
        listOf("Fabric", "قماش", "1fabric", "a".repeat(65), "_fabric").forEach { key ->
            val reasons = invalidReasonsOf(
                validateMapping(
                    listOf(
                        column(choice = MappingChoice.Standard(StandardTarget.NAME)),
                        column("Fabric", 2, MappingChoice.CustomAttribute(key))
                    )
                )
            )

            assertEquals(listOf(MappingInvalidReason.BadCustomKey(1)), reasons)
        }
    }

    @Test
    fun `reserved prototype keys are rejected`() {
        listOf("constructor", "prototype", "__proto__").forEach { key ->
            val reasons = invalidReasonsOf(
                validateMapping(
                    listOf(
                        column(choice = MappingChoice.Standard(StandardTarget.NAME)),
                        column("Fabric", 2, MappingChoice.CustomAttribute(key))
                    )
                )
            )

            assertEquals(listOf(MappingInvalidReason.BadCustomKey(1)), reasons)
        }
    }

    @Test
    fun `a 64 character key is accepted`() {
        val result = validateMapping(
            listOf(
                column(choice = MappingChoice.Standard(StandardTarget.NAME)),
                column("Fabric", 2, MappingChoice.CustomAttribute("a".repeat(64)))
            )
        )

        assertEquals(MappingValidationResult.Valid, result)
    }

    @Test
    fun `a custom key used twice flags only the later column`() {
        val reasons = invalidReasonsOf(
            validateMapping(
                listOf(
                    column(choice = MappingChoice.Standard(StandardTarget.NAME)),
                    column("A", 2, MappingChoice.CustomAttribute("fabric")),
                    column("B", 3, MappingChoice.CustomAttribute("fabric"))
                )
            )
        )

        assertEquals(listOf(MappingInvalidReason.CustomKeyUsedTwice(2)), reasons)
    }

    @Test
    fun `a custom key may equal a standard target name`() {
        val result = validateMapping(
            listOf(
                column(choice = MappingChoice.Standard(StandardTarget.NAME)),
                column("Label", 2, MappingChoice.CustomAttribute("name"))
            )
        )

        assertEquals(MappingValidationResult.Valid, result)
    }

    @Test
    fun `two untouched custom fields do not count as duplicates`() {
        val result = validateMapping(
            listOf(
                column(choice = MappingChoice.Standard(StandardTarget.NAME)),
                column("A", 2, MappingChoice.CustomAttribute("")),
                column("B", 3, MappingChoice.CustomAttribute(""))
            )
        )

        val reasons = invalidReasonsOf(result)
        assertTrue(reasons.none { it is MappingInvalidReason.CustomKeyUsedTwice })
    }
}
