package com.yasminhosam.brandora.feature.products.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PageInfo(
    val limit: Int = 20,
    val offset: Int = 0,
    val totalCount: Int = 0,
    val hasMore: Boolean = false,
    val nextOffset: Int? = null,
)

@Serializable data class Page<T>(val items: List<T>, val pagination: PageInfo)

@Serializable data class Category(val id: String, val name: String)

@Serializable
data class ProductImage(
    val id: String,
    val productId: String,
    val secureUrl: String,
    val position: Int,
    val isMain: Boolean,
    val width: Int,
    val height: Int,
    val bytes: Int,
    val format: String,
)

@Serializable
data class Variant(
    val id: String,
    val productId: String,
    val businessId: String,
    val sku: String? = null,
    val unitPrice: String? = null,
    val effectivePrice: String? = null,
    val optionValues: Map<String, String> = emptyMap(),
    val isActive: Boolean = true,
    val trackInventory: Boolean = false,
    val stockQuantity: Int? = null,
) : java.io.Serializable

@Serializable
data class Product(
    val id: String,
    val businessId: String,
    val name: String,
    val externalKey: String? = null,
    val description: String? = null,
    val categoryId: String? = null,
    val basePrice: String? = null,
    val options: Map<String, List<String>> = emptyMap(),
    val isActive: Boolean = true,
    val isSellable: Boolean = false,
    val images: List<ProductImage> = emptyList(),
    val variants: List<Variant> = emptyList(),
    val variantsPagination: PageInfo = PageInfo(),
)

@Serializable data class Images(val items: List<ProductImage>, val cleanupPending: Boolean = false)

@Serializable data class ImageDelete(val deleted: Boolean, val cleanupPending: Boolean)

@Serializable
data class InventoryAdjustment(
    val changed: Boolean,
    val quantityBefore: Int? = null,
    val quantityAfter: Int? = null,
    val quantityDelta: Int,
    val movementId: String? = null,
)

@Serializable
data class StockMovement(
    val id: String,
    val type: String,
    val catalogItemId: String? = null,
    val variantId: String? = null,
    val quantityBefore: Int? = null,
    val quantityAfter: Int? = null,
    val quantityDelta: Int,
    val trackingBefore: Boolean = false,
    val trackingAfter: Boolean = false,
    val requestedQuantity: Int? = null,
    val referenceType: String? = null,
    val referenceId: String? = null,
    val createdAt: String,
)

data class ImageUpload(val bytes: ByteArray, val mime: String)
