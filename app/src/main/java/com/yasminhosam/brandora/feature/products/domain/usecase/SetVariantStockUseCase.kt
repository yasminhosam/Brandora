package com.yasminhosam.brandora.feature.products.domain.usecase

import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.products.domain.ProductsRepository
import com.yasminhosam.brandora.feature.products.domain.model.InventoryAdjustment
import javax.inject.Inject

/**
 * Absolute stock write: null disables tracking, a non-negative integer sets the quantity.
 * Negative quantities never reach the network (INVENTORY_QUANTITY_INVALID, same code the backend
 * uses).
 */
class SetVariantStockUseCase @Inject constructor(private val repository: ProductsRepository) {
    suspend operator fun invoke(
        business: String,
        productId: String,
        variantId: String,
        quantity: Int?,
    ): ApiResult<InventoryAdjustment> {
        if (quantity != null && quantity < 0)
            return ApiResult.Error(
                ApiErrorType.BAD_REQUEST,
                errorCode = "INVENTORY_QUANTITY_INVALID",
            )
        return repository.adjust(business, productId, variantId, quantity)
    }
}
