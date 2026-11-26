package com.yasminhosam.brandora.feature.products.domain.usecase

import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.products.domain.ProductRules
import com.yasminhosam.brandora.feature.products.domain.ProductsRepository
import com.yasminhosam.brandora.feature.products.domain.model.Page
import com.yasminhosam.brandora.feature.products.domain.model.Product
import com.yasminhosam.brandora.feature.products.domain.model.StockMovement
import com.yasminhosam.brandora.feature.products.domain.model.Variant
import com.yasminhosam.brandora.feature.products.domain.model.VariantDraft
import javax.inject.Inject

/**
 * Variant pages, writes and stock history. A draft must select exactly the product's options
 * (VARIANT_OPTIONS_INVALID) and pass field rules (VALIDATION_ERROR) before any write.
 */
class VariantUseCases @Inject constructor(private val repository: ProductsRepository) {
    suspend fun page(business: String, productId: String, offset: Int = 0): ApiResult<Page<Variant>> =
        repository.variants(business, productId, offset)

    suspend fun save(
        business: String,
        product: Product,
        id: String?,
        draft: VariantDraft,
    ): ApiResult<Variant> {
        if (!ProductRules.selectionMatches(product.options, draft.optionValues))
            return ApiResult.Error(ApiErrorType.BAD_REQUEST, errorCode = "VARIANT_OPTIONS_INVALID")
        if (!ProductRules.validVariantFields(draft))
            return ApiResult.Error(ApiErrorType.BAD_REQUEST, errorCode = "VALIDATION_ERROR")
        return repository.saveVariant(business, product.id, id, draft)
    }

    /**
     * State-only PATCH: the body carries isActive alone — never sku, unitPrice or optionValues,
     * whose nulls would clear those fields. Reactivation under an inactive product surfaces
     * PRODUCT_INACTIVE from the server.
     */
    suspend fun setActive(
        business: String,
        productId: String,
        variantId: String,
        active: Boolean,
    ): ApiResult<Variant> = repository.setVariantActive(business, productId, variantId, active)

    suspend fun movements(
        business: String,
        productId: String,
        variantId: String,
        offset: Int = 0,
    ): ApiResult<Page<StockMovement>> =
        repository.movements(business, productId, variantId, offset)
}
