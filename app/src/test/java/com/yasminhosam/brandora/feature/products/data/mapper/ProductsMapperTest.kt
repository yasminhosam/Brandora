package com.yasminhosam.brandora.feature.products.data.mapper

import com.yasminhosam.brandora.core.network.dto.PaginationDto
import com.yasminhosam.brandora.feature.products.data.remote.*
import com.yasminhosam.brandora.feature.products.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class ProductsMapperTest {
    @Test
    fun `product dto maps server computed fields and nested collections`() {
        val domain =
            ProductDto(
                id = "p1",
                businessId = "b1",
                name = "Shirt",
                externalKey = "EXT-1",
                description = "Nice",
                categoryId = "c1",
                basePrice = "10.00",
                options = mapOf("Color" to listOf("Red")),
                isActive = true,
                isSellable = true,
                images =
                    listOf(
                        ProductImageDto(
                            id = "i1",
                            productId = "p1",
                            secureUrl = "https://cdn.test/i1.png",
                            position = 0,
                            isMain = true,
                            width = 1,
                            height = 2,
                            bytes = 3,
                            format = "png",
                        )
                    ),
                variants =
                    listOf(
                        VariantDto(
                            id = "v1",
                            productId = "p1",
                            businessId = "b1",
                            sku = "S1",
                            unitPrice = null,
                            effectivePrice = "10.00",
                            optionValues = mapOf("Color" to "Red"),
                            isActive = true,
                            trackInventory = true,
                            stockQuantity = 7,
                        )
                    ),
                variantsPagination =
                    PaginationDto(
                        limit = 20,
                        offset = 0,
                        totalCount = 47,
                        hasMore = true,
                        nextOffset = 20
                    ),
            ).toDomain()

        assertTrue(domain.isSellable)
        assertEquals("EXT-1", domain.externalKey)
        assertEquals("c1", domain.categoryId)
        assertEquals("10.00", domain.basePrice)
        assertEquals(mapOf("Color" to listOf("Red")), domain.options)
        assertEquals(47, domain.variantsPagination.totalCount)
        assertTrue(domain.variantsPagination.hasMore)
        assertEquals(20, domain.variantsPagination.nextOffset)
        val variant = domain.variants.single()
        assertEquals("10.00", variant.effectivePrice)
        assertEquals(7, variant.stockQuantity)
        assertTrue(variant.trackInventory)
        val image = domain.images.single()
        assertEquals("i1", image.id)
        assertTrue(image.isMain)
    }

    @Test
    fun `page dto maps items and pagination together`() {
        val page =
            PageDto(
                items =
                    listOf(
                        CategoryDto(id = "c1", name = "General"),
                        CategoryDto(id = "c2", name = "Toys"),
                    ),
                pagination = PaginationDto(totalCount = 2),
            ).toDomain { it.toDomain() }

        assertEquals(listOf("c1", "c2"), page.items.map { it.id })
        assertEquals(listOf("General", "Toys"), page.items.map { it.name })
        assertEquals(2, page.pagination.totalCount)
    }

    @Test
    fun `stock movement dto carries the inventory additions`() {
        val movement =
            StockMovementDto(
                id = "m1",
                type = "MANUAL_ADJUSTMENT",
                catalogItemId = "p1",
                variantId = "v1",
                quantityBefore = null,
                quantityAfter = 15,
                quantityDelta = 15,
                trackingBefore = false,
                trackingAfter = true,
                requestedQuantity = null,
                referenceType = null,
                referenceId = null,
                createdAt = "2026-10-29T10:00:00.000Z",
            ).toDomain()

        assertEquals("v1", movement.variantId)
        assertEquals("p1", movement.catalogItemId)
        assertFalse(movement.trackingBefore)
        assertTrue(movement.trackingAfter)
        assertNull(movement.quantityBefore)
        assertEquals(15, movement.quantityAfter)
        assertEquals(15, movement.quantityDelta)
    }

    @Test
    fun `images, delete, adjustment and category dtos map to domain`() {
        val images =
            ImagesDto(
                items = listOf(ProductImageDto(id = "i1")),
                cleanupPending = true,
            ).toDomain()
        assertTrue(images.cleanupPending)
        assertEquals("i1", images.items.single().id)

        val deletion = ImageDeleteDto(deleted = true, cleanupPending = true).toDomain()
        assertTrue(deletion.deleted)
        assertTrue(deletion.cleanupPending)

        val adjustment =
            InventoryAdjustmentDto(
                changed = true,
                quantityBefore = null,
                quantityAfter = 5,
                quantityDelta = 5,
                movementId = "m1",
            ).toDomain()
        assertTrue(adjustment.changed)
        assertNull(adjustment.quantityBefore)
        assertEquals("m1", adjustment.movementId)

        assertEquals(Category("c1", "General"), CategoryDto(id = "c1", name = "General").toDomain())
    }

    @Test
    fun `drafts map to typed write bodies`() {
        val product =
            ProductDraft(
                name = "Manual",
                externalKey = null,
                description = null,
                basePrice = "9.99",
                categoryId = "c1",
                options = mapOf("Size" to listOf("M")),
            ).toWrite()
        assertEquals("Manual", product.name)
        assertNull(product.externalKey)
        assertNull(product.description)
        assertEquals("9.99", product.basePrice)
        assertEquals("c1", product.categoryId)
        assertEquals(mapOf("Size" to listOf("M")), product.options)

        val created =
            VariantDraft(sku = "S1", unitPrice = null, optionValues = mapOf("Size" to "M"))
                .toCreateWrite()
        assertEquals("S1", created.sku)
        assertNull(created.unitPrice)
        assertEquals(mapOf("Size" to "M"), created.optionValues)

        val updated =
            VariantDraft(
                sku = "S1",
                unitPrice = null,
                optionValues = mapOf("Size" to "M"),
                isActive = false,
            ).toUpdateWrite()
        assertEquals("S1", updated.sku)
        assertNull(updated.unitPrice)
        assertEquals(false, updated.isActive)

        // An update without an explicit flag falls back to the server default (active).
        assertEquals(true, VariantDraft().toUpdateWrite().isActive)
    }
}
