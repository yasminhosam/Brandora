package com.yasminhosam.brandora.feature.catalogimport.domain.mapping

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MappingMergeTest {

    private fun suggestion(
        name: String = "Item Name",
        index: Int = 1,
        type: String = "STANDARD",
        target: String? = "name"
    ) = ColumnSuggestion(name, index, listOf("Shirt"), type, target)

    private fun saved(
        name: String = "Item Name",
        index: Int = 1,
        type: String = "STANDARD",
        target: String? = "name"
    ) = SavedMapping(name, index, type, target)

    @Test
    fun `suggestions become the initial choice when nothing was saved`() {
        val columns = initialColumns(
            listOf(suggestion(), suggestion("Price", 2, "STANDARD", "basePrice")),
            null
        )

        assertEquals(MappingChoice.Standard(StandardTarget.NAME), columns[0].choice)
        assertEquals(MappingChoice.Standard(StandardTarget.BASE_PRICE), columns[1].choice)
    }

    @Test
    fun `unmapped suggestions become ignore`() {
        val columns = initialColumns(listOf(suggestion(type = "UNMAPPED", target = null)), null)

        assertEquals(MappingChoice.Ignore, columns.single().choice)
    }

    @Test
    fun `a suggestion with an unknown target becomes ignore`() {
        val columns = initialColumns(listOf(suggestion(target = "something_new")), null)

        assertEquals(MappingChoice.Ignore, columns.single().choice)
    }

    @Test
    fun `a saved mapping wins over the suggestion`() {
        val columns = initialColumns(
            listOf(suggestion(target = "name")),
            listOf(saved(target = "externalKey"))
        )

        assertEquals(MappingChoice.Standard(StandardTarget.EXTERNAL_KEY), columns.single().choice)
    }

    @Test
    fun `a saved custom key starts in the field`() {
        val columns = initialColumns(
            listOf(suggestion(type = "UNMAPPED", target = null)),
            listOf(saved(type = "ATTRIBUTE", target = "fabric"))
        )

        assertEquals(MappingChoice.CustomAttribute("fabric"), columns.single().choice)
    }

    @Test
    fun `a saved variant attribute becomes ignore and is never sent back`() {
        val columns = initialColumns(
            listOf(suggestion()),
            listOf(saved(type = "VARIANT_ATTRIBUTE", target = "size"))
        )

        assertEquals(MappingChoice.Ignore, columns.single().choice)
    }

    @Test
    fun `a saved entry with an unknown type becomes ignore`() {
        val columns = initialColumns(
            listOf(suggestion()),
            listOf(saved(type = "SOMETHING_NEW", target = null))
        )

        assertEquals(MappingChoice.Ignore, columns.single().choice)
    }

    @Test
    fun `duplicate headers match their saved entry by index, not by name`() {
        val columns = initialColumns(
            listOf(
                suggestion("Note", 1, "UNMAPPED", null),
                suggestion("Note", 2, "UNMAPPED", null)
            ),
            listOf(saved("Note", 2, "ATTRIBUTE", "note2"))
        )

        assertEquals(MappingChoice.Ignore, columns[0].choice)
        assertEquals(MappingChoice.CustomAttribute("note2"), columns[1].choice)
    }

    @Test
    fun `a column without a saved entry falls back to its suggestion`() {
        val columns = initialColumns(
            listOf(suggestion(), suggestion("Price", 2, "STANDARD", "basePrice")),
            listOf(saved(index = 1))
        )

        assertEquals(MappingChoice.Standard(StandardTarget.NAME), columns[0].choice)
        assertEquals(MappingChoice.Standard(StandardTarget.BASE_PRICE), columns[1].choice)
    }

    @Test
    fun `header text, index and samples survive the merge`() {
        val columns = initialColumns(
            listOf(ColumnSuggestion("Item Name", 1, listOf("Shirt", "Hat"), "STANDARD", "name")),
            null
        )

        val column = columns.single()
        assertEquals("Item Name", column.sourceColumn)
        assertEquals(1, column.sourceColumnIndex)
        assertEquals(listOf("Shirt", "Hat"), column.samples)
    }

    @Test
    fun `an empty sheet yields no columns`() {
        assertTrue(initialColumns(emptyList(), null).isEmpty())
    }
}
