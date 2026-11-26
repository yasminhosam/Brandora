package com.yasminhosam.brandora.feature.products.domain

import org.junit.Assert.*
import org.junit.Test

class ProductRulesTest {
    @Test
    fun `prices keep decimal string semantics without floating point`() {
        listOf("", "0", "0.00", "0.01", "125.50", "9999999999.99").forEach {
            assertTrue(it, ProductRules.validMoney(it))
        }
        listOf("00", "1e2", "-1", "+1", "1,00", "1.001", "10000000000", "NaN", "١٢").forEach {
            assertFalse(it, ProductRules.validMoney(it))
        }
    }

    @Test
    fun `ASCII Arabic and non BMP external keys share backend code point boundary`() {
        for (char in listOf("A", "ع", "\uD83D\uDE00")) {
            assertTrue(ProductRules.validExternalKey(char.repeat(200)))
            assertFalse(ProductRules.validExternalKey(char.repeat(201)))
        }
        assertTrue(ProductRules.validExternalKey("  CODE  "))
    }

    @Test
    fun `combining marks count independently and normalization is not applied`() {
        assertTrue(ProductRules.validExternalKey("a\u0301".repeat(100)))
        assertFalse(ProductRules.validExternalKey("a\u0301".repeat(101)))
    }

    @Test
    fun `options are business defined and preserve case Arabic and internal spacing`() {
        assertEquals(
            mapOf("الخامة" to listOf("قطن", "حرير"), "Custom" to listOf("A  B", "a b")),
            ProductRules.definitions(
                listOf(OptionDraft(" الخامة ", "قطن\nحرير"), OptionDraft("Custom", "A  B\na b"))
            ),
        )
    }

    @Test
    fun `duplicate normalized names values reserved names and empty definitions rejected`() {
        assertNull(ProductRules.definitions(listOf(OptionDraft(" X ", "a"), OptionDraft("X", "b"))))
        assertNull(ProductRules.definitions(listOf(OptionDraft("X", "a\n a "))))
        assertNull(ProductRules.definitions(listOf(OptionDraft("__proto__", "x"))))
        assertNull(ProductRules.definitions(listOf(OptionDraft("X", "  "))))
    }

    @Test
    fun `option count name and value boundaries enforced`() {
        assertNotNull(ProductRules.definitions(emptyList()))
        assertNull(ProductRules.definitions((0..10).map { OptionDraft("K" + it, "a") }))
        assertNull(ProductRules.definitions(listOf(OptionDraft("x".repeat(61), "a"))))
        assertNull(ProductRules.definitions(listOf(OptionDraft("X", "a".repeat(101)))))
    }

    @Test
    fun `variant selections must contain exactly allowed keys and values`() {
        val options = mapOf("الخامة" to listOf("قطن", "حرير"))
        assertTrue(ProductRules.selectionMatches(options, mapOf("الخامة" to "قطن")))
        assertFalse(ProductRules.selectionMatches(options, emptyMap()))
        assertFalse(ProductRules.selectionMatches(options, mapOf("الخامة" to "أخرى")))
        assertFalse(
            ProductRules.selectionMatches(options, mapOf("الخامة" to "قطن", "Other" to "x"))
        )
        assertTrue(ProductRules.selectionMatches(emptyMap(), emptyMap()))
    }

    @Test
    fun `option edits keep every existing name and value when variants exist`() {
        val existing = mapOf("Size" to listOf("S", "M"), "Color" to listOf("Red"))
        assertTrue(ProductRules.validOptionSuperset(existing, existing))
        assertTrue(ProductRules.validOptionSuperset(emptyMap(), mapOf("Size" to listOf("S"))))
        // Expansions are allowed: new names, new values, new combinations.
        assertTrue(
            ProductRules.validOptionSuperset(
                existing,
                existing + ("Material" to listOf("Cotton")),
            )
        )
        assertTrue(
            ProductRules.validOptionSuperset(
                existing,
                mapOf("Size" to listOf("S", "M", "L"), "Color" to listOf("Red", "Blue")),
            )
        )
        // Removals are not: a dropped name or a dropped value invalidates variants.
        assertFalse(ProductRules.validOptionSuperset(existing, mapOf("Size" to listOf("S", "M"))))
        assertFalse(
            ProductRules.validOptionSuperset(
                existing,
                mapOf("Size" to listOf("S"), "Color" to listOf("Red")),
            )
        )
        assertFalse(
            ProductRules.validOptionSuperset(
                existing,
                mapOf("Size" to listOf("S", "M"), "Color" to listOf("Blue")),
            )
        )
        assertFalse(ProductRules.validOptionSuperset(existing, emptyMap()))
    }

    @Test
    fun `complete image batch must fit available slots`() {
        assertTrue(ProductRules.batchFits(0, 5))
        assertTrue(ProductRules.batchFits(4, 1))
        assertFalse(ProductRules.batchFits(4, 2))
        assertFalse(ProductRules.batchFits(5, 1))
        assertFalse(ProductRules.batchFits(0, 6))
        assertFalse(ProductRules.batchFits(0, 0))
    }

    @Test
    fun `reorder includes every image exactly once and position zero becomes Main`() {
        assertEquals(listOf("c", "a", "b"), ProductRules.orderedIds(listOf("a", "b", "c"), 2, 0))
        assertNull(ProductRules.orderedIds(listOf("a", "a"), 0, 1))
        assertNull(ProductRules.orderedIds(listOf("a"), 0, 1))
    }
}
