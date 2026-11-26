package com.yasminhosam.brandora.feature.products.data.remote

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class ProductsDtoJsonTest {
    // Keep in sync with NetworkModule.provideJson().
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    // Request bodies use the products-local codec (explicit nulls) so PATCH clears survive.
    private val writeJson = productsWriteJson

    @Test
    fun `product page decodes server computed fields and variant pagination`() {
        val parsed = json.decodeFromString<PageDto<ProductDto>>(PRODUCT_PAGE)

        val product = parsed.items.single()
        assertTrue(product.isSellable)
        assertEquals("cat-1", product.categoryId)
        assertEquals("125.00", product.basePrice)
        assertNull(product.externalKey)
        assertEquals(mapOf("Color" to listOf("Red", "Blue")), product.options)
        // Unknown server fields (colorOptionName, attributes, catalogItemId) are ignored.
        assertEquals(47, parsed.pagination.totalCount)
        assertTrue(parsed.pagination.hasMore)
        assertEquals(20, parsed.pagination.nextOffset)
        assertEquals(47, product.variantsPagination.totalCount)
        val variant = product.variants.single()
        assertEquals("12.50", variant.effectivePrice)
        assertNull(variant.stockQuantity)
        assertTrue(variant.trackInventory)
    }

    @Test
    fun `stock movement decodes the inventory API additions`() {
        val movement = json.decodeFromString<StockMovementDto>(MOVEMENT)

        assertEquals("var-1", movement.variantId)
        assertEquals("prod-1", movement.catalogItemId)
        assertFalse(movement.trackingBefore)
        assertTrue(movement.trackingAfter)
        assertNull(movement.requestedQuantity)
        assertNull(movement.referenceType)
        assertNull(movement.referenceId)
        assertEquals(15, movement.quantityAfter)
        assertEquals("2026-10-29T10:00:00.000Z", movement.createdAt)

        val nullAfter = json.decodeFromString<StockMovementDto>(MOVEMENT_NULL_AFTER)
        assertNull(nullAfter.quantityAfter)
    }

    @Test
    fun `images, image delete and adjustment responses decode`() {
        val images = json.decodeFromString<ImagesDto>(IMAGES)
        assertTrue(images.cleanupPending)
        assertTrue(images.items.single().isMain)

        val deletion = json.decodeFromString<ImageDeleteDto>(
            """{"deleted":true,"cleanupPending":true}"""
        )
        assertTrue(deletion.deleted)
        assertTrue(deletion.cleanupPending)

        val adjustment = json.decodeFromString<InventoryAdjustmentDto>(
            """{"changed":true,"quantityBefore":null,"quantityAfter":15,"quantityDelta":15,"movementId":"m1"}"""
        )
        assertTrue(adjustment.changed)
        assertNull(adjustment.quantityBefore)
        assertEquals(15, adjustment.quantityDelta)
        assertEquals("m1", adjustment.movementId)
    }

    @Test
    fun `product write always encodes every field so PATCH can clear nullable values`() {
        val encoded =
            writeJson.encodeToString(
                ProductWriteDto(
                    name = "Manual",
                    externalKey = null,
                    description = null,
                    basePrice = null,
                    categoryId = null,
                    options = emptyMap(),
                )
            )
        val sent = json.parseToJsonElement(encoded).jsonObject

        assertEquals(
            setOf("name", "externalKey", "description", "basePrice", "categoryId", "options"),
            sent.keys,
        )
        assertEquals(JsonNull, sent["externalKey"])
        assertEquals(JsonNull, sent["description"])
        assertEquals(JsonNull, sent["basePrice"])
        assertEquals(JsonNull, sent["categoryId"])
        assertEquals(JsonObject(emptyMap()), sent["options"])
    }

    @Test
    fun `variant create body carries only sku, optionValues and unitPrice`() {
        val encoded =
            writeJson.encodeToString(
                VariantCreateDto(
                    sku = null,
                    optionValues = mapOf("Color" to "Red"),
                    unitPrice = null,
                )
            )
        val sent = json.parseToJsonElement(encoded).jsonObject

        assertEquals(setOf("sku", "optionValues", "unitPrice"), sent.keys)
        assertFalse(sent.containsKey("isActive"))
        assertFalse(sent.containsKey("stockQuantity"))
        assertEquals(JsonNull, sent["sku"])
        assertEquals(JsonNull, sent["unitPrice"])
    }

    @Test
    fun `variant edit body always carries isActive and keeps null clears`() {
        val encoded =
            writeJson.encodeToString(
                VariantUpdateDto(
                    sku = null,
                    optionValues = emptyMap(),
                    unitPrice = "9.99",
                    isActive = false,
                )
            )
        val sent = json.parseToJsonElement(encoded).jsonObject

        assertEquals(setOf("sku", "optionValues", "unitPrice", "isActive"), sent.keys)
        assertEquals(false, sent["isActive"]?.jsonPrimitive?.boolean)
        assertEquals(JsonNull, sent["sku"])
        assertEquals("9.99", sent["unitPrice"]?.jsonPrimitive?.content)
    }

    @Test
    fun `stock adjust encodes explicit null quantity with its tracking flag`() {
        val off =
            writeJson.encodeToString(StockAdjustWriteDto(null, false))
        val offObject = json.parseToJsonElement(off).jsonObject
        assertEquals(JsonNull, offObject["quantity"])
        assertEquals(false, offObject["trackInventory"]?.jsonPrimitive?.boolean)

        val on =
            json.parseToJsonElement(writeJson.encodeToString(StockAdjustWriteDto(15, true))).jsonObject
        assertEquals(15, on["quantity"]?.jsonPrimitive?.int)
        assertEquals(true, on["trackInventory"]?.jsonPrimitive?.boolean)
    }

    @Test
    fun `small writes carry exactly their fields`() {
        assertEquals(
            setOf("isActive"),
            json.parseToJsonElement(writeJson.encodeToString(ActiveWriteDto(false))).jsonObject.keys,
        )
        assertEquals(
            setOf("name"),
            json.parseToJsonElement(writeJson.encodeToString(CategoryWriteDto("General Goods"))).jsonObject.keys,
        )
        assertEquals(
            setOf("isMain"),
            json.parseToJsonElement(writeJson.encodeToString(IsMainWriteDto(true))).jsonObject.keys,
        )
        val order =
            json.parseToJsonElement(writeJson.encodeToString(ImageOrderWriteDto(listOf("a", "b")))).jsonObject
        assertEquals(setOf("imageIds"), order.keys)
        assertEquals(listOf("a", "b"), order["imageIds"]?.jsonArray?.map { it.jsonPrimitive.content })
    }

    companion object {
        private const val PRODUCT_PAGE =
            """
            {
              "items": [
                {
                  "id": "prod-1",
                  "businessId": "biz-1",
                  "name": "Custom Product",
                  "externalKey": null,
                  "categoryId": "cat-1",
                  "basePrice": "125.00",
                  "options": {"Color": ["Red", "Blue"]},
                  "isActive": true,
                  "isSellable": true,
                  "colorOptionName": "Color",
                  "attributes": {"details": {}},
                  "images": [],
                  "variants": [
                    {
                      "id": "var-1",
                      "productId": "prod-1",
                      "businessId": "biz-1",
                      "catalogItemId": "prod-1",
                      "sku": null,
                      "unitPrice": null,
                      "effectivePrice": "12.50",
                      "optionValues": {"Color": "Red"},
                      "trackInventory": true,
                      "stockQuantity": null,
                      "isActive": true
                    }
                  ],
                  "variantsPagination": {"limit": 20, "offset": 0, "totalCount": 47, "hasMore": true, "nextOffset": 20}
                }
              ],
              "pagination": {"limit": 20, "offset": 0, "totalCount": 47, "hasMore": true, "nextOffset": 20}
            }
            """

        private const val MOVEMENT =
            """
            {
              "id": "f6bc7287-3285-4e19-a819-459c41f0a138",
              "catalogItemId": "prod-1",
              "variantId": "var-1",
              "type": "MANUAL_ADJUSTMENT",
              "quantityBefore": null,
              "quantityAfter": 15,
              "quantityDelta": 15,
              "trackingBefore": false,
              "trackingAfter": true,
              "requestedQuantity": null,
              "referenceType": null,
              "referenceId": null,
              "createdAt": "2026-10-29T10:00:00.000Z"
            }
            """

        private const val MOVEMENT_NULL_AFTER =
            """
            {
              "id": "m2",
              "type": "STOCK_DISABLING",
              "quantityAfter": null,
              "quantityDelta": -3,
              "trackingAfter": false,
              "createdAt": "2026-10-29T11:00:00.000Z"
            }
            """

        private const val IMAGES =
            """
            {
              "items": [
                {
                  "id": "img-1",
                  "productId": "prod-1",
                  "secureUrl": "https://cdn.test/i1.png",
                  "position": 0,
                  "isMain": true,
                  "width": 100,
                  "height": 100,
                  "bytes": 1024,
                  "format": "png"
                }
              ],
              "cleanupPending": true
            }
            """
    }
}
