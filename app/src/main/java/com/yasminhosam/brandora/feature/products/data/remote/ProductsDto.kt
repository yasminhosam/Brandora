package com.yasminhosam.brandora.feature.products.data.remote

import com.yasminhosam.brandora.core.network.dto.PaginationDto
import kotlinx.serialization.Serializable

/** Shared `{ items, pagination }` envelope used by every collection endpoint. */
@Serializable
data class PageDto<T>(
    val items: List<T> = emptyList(),
    val pagination: PaginationDto = PaginationDto(),
)


@Serializable
data class CategoryDto(
    val id: String,
    val name: String,
    val businessId: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class ProductImageDto(
    val id: String,
    val productId: String = "",
    val secureUrl: String = "",
    val position: Int = 0,
    val isMain: Boolean = false,
    val width: Int = 0,
    val height: Int = 0,
    val bytes: Int = 0,
    val format: String = "",
)

@Serializable
data class VariantDto(
    val id: String,
    val productId: String = "",
    val businessId: String = "",
    val sku: String? = null,
    val unitPrice: String? = null,
    val effectivePrice: String? = null,
    val optionValues: Map<String, String> = emptyMap(),
    val isActive: Boolean = true,
    val trackInventory: Boolean = false,
    val stockQuantity: Int? = null,
)

@Serializable
data class ProductDto(
    val id: String,
    val businessId: String = "",
    val name: String,
    val externalKey: String? = null,
    val description: String? = null,
    val categoryId: String? = null,
    val basePrice: String? = null,
    val options: Map<String, List<String>> = emptyMap(),
    val isActive: Boolean = true,
    val isSellable: Boolean = false,
    val images: List<ProductImageDto> = emptyList(),
    val variants: List<VariantDto> = emptyList(),
    val variantsPagination: PaginationDto = PaginationDto(),
)

@Serializable
data class ImagesDto(
    val items: List<ProductImageDto> = emptyList(),
    val cleanupPending: Boolean = false,
)

@Serializable
data class ImageDeleteDto(val deleted: Boolean = false, val cleanupPending: Boolean = false)

@Serializable
data class InventoryAdjustmentDto(
    val changed: Boolean = false,
    val quantityBefore: Int? = null,
    val quantityAfter: Int? = null,
    val quantityDelta: Int = 0,
    val movementId: String? = null,
)

/**
 * Inventory movement per `inventory-api.md`: items add variantId, trackingBefore/After,
 * requestedQuantity and the retained catalogItemId alias; quantityAfter is nullable.
 */
@Serializable
data class StockMovementDto(
    val id: String,
    val type: String,
    val catalogItemId: String? = null,
    val variantId: String? = null,
    val quantityBefore: Int? = null,
    val quantityAfter: Int? = null,
    val quantityDelta: Int = 0,
    val trackingBefore: Boolean = false,
    val trackingAfter: Boolean = false,
    val requestedQuantity: Int? = null,
    val referenceType: String? = null,
    val referenceId: String? = null,
    val createdAt: String,
)
