package com.yasminhosam.brandora.feature.products.data.mapper

import com.yasminhosam.brandora.core.network.dto.PaginationDto
import com.yasminhosam.brandora.feature.products.data.remote.*
import com.yasminhosam.brandora.feature.products.domain.model.*

internal fun PaginationDto.toDomain() = PageInfo(limit, offset, totalCount, hasMore, nextOffset)

internal fun <T, R> PageDto<T>.toDomain(item: (T) -> R) = Page(items.map(item), pagination.toDomain())

internal fun CategoryDto.toDomain() = Category(id, name)

internal fun ProductImageDto.toDomain() =
    ProductImage(id, productId, secureUrl, position, isMain, width, height, bytes, format)

internal fun VariantDto.toDomain() =
    Variant(
        id = id,
        productId = productId,
        businessId = businessId,
        sku = sku,
        unitPrice = unitPrice,
        effectivePrice = effectivePrice,
        optionValues = optionValues,
        isActive = isActive,
        trackInventory = trackInventory,
        stockQuantity = stockQuantity,
    )

internal fun ProductDto.toDomain() =
    Product(
        id = id,
        businessId = businessId,
        name = name,
        externalKey = externalKey,
        description = description,
        categoryId = categoryId,
        basePrice = basePrice,
        options = options,
        isActive = isActive,
        isSellable = isSellable,
        images = images.map { it.toDomain() },
        variants = variants.map { it.toDomain() },
        variantsPagination = variantsPagination.toDomain(),
    )

internal fun ImagesDto.toDomain() = Images(items.map { it.toDomain() }, cleanupPending)

internal fun ImageDeleteDto.toDomain() = ImageDelete(deleted, cleanupPending)

internal fun InventoryAdjustmentDto.toDomain() =
    InventoryAdjustment(changed, quantityBefore, quantityAfter, quantityDelta, movementId)

internal fun StockMovementDto.toDomain() =
    StockMovement(
        id = id,
        type = type,
        catalogItemId = catalogItemId,
        variantId = variantId,
        quantityBefore = quantityBefore,
        quantityAfter = quantityAfter,
        quantityDelta = quantityDelta,
        trackingBefore = trackingBefore,
        trackingAfter = trackingAfter,
        requestedQuantity = requestedQuantity,
        referenceType = referenceType,
        referenceId = referenceId,
        createdAt = createdAt,
    )

internal fun ProductDraft.toWrite() =
    ProductWriteDto(
        name = name,
        externalKey = externalKey,
        description = description,
        basePrice = basePrice,
        categoryId = categoryId,
        options = options,
    )

internal fun VariantDraft.toCreateWrite() =
    VariantCreateDto(sku = sku, optionValues = optionValues, unitPrice = unitPrice)

internal fun VariantDraft.toUpdateWrite() =
    VariantUpdateDto(
        sku = sku,
        optionValues = optionValues,
        unitPrice = unitPrice,
        isActive = isActive ?: true,
    )
