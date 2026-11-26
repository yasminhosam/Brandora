package com.yasminhosam.brandora.feature.products.domain.usecase

import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.products.domain.ProductRules
import com.yasminhosam.brandora.feature.products.domain.ProductsRepository
import com.yasminhosam.brandora.feature.products.domain.model.Product
import com.yasminhosam.brandora.feature.products.domain.model.ProductDraft
import javax.inject.Inject

/**
 * Saves a product draft: normalizes trims, validates every field against ProductRules (the
 * backend's VALIDATION_ERROR semantics), then writes. null clears a nullable field on update.
 */
class SaveProductUseCase @Inject constructor(private val repository: ProductsRepository) {
    suspend operator fun invoke(
        business: String,
        id: String?,
        draft: ProductDraft,
    ): ApiResult<Product> {
        val normalized =
            draft.copy(
                name = draft.name.trim(),
                externalKey = draft.externalKey?.trim()?.takeIf { it.isNotEmpty() },
                basePrice = draft.basePrice?.trim()?.takeIf { it.isNotEmpty() },
            )
        if (!ProductRules.validProduct(normalized))
            return ApiResult.Error(ApiErrorType.BAD_REQUEST, errorCode = "VALIDATION_ERROR")
        return repository.save(business, id, normalized)
    }
}
