package com.yasminhosam.brandora.feature.products.domain.usecase

import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.products.domain.CategoryFilter
import com.yasminhosam.brandora.feature.products.domain.ProductPageSource
import com.yasminhosam.brandora.feature.products.domain.ProductsRepository
import com.yasminhosam.brandora.feature.products.domain.model.Page
import com.yasminhosam.brandora.feature.products.domain.model.Product
import javax.inject.Inject

/**
 * Product reads and lifecycle. Pages go through the ProductPageSource seam so category filtering
 * stays a data-layer detail; saving lives in SaveProductUseCase.
 */
class ProductUseCases
@Inject
constructor(
    private val repository: ProductsRepository,
    private val pages: ProductPageSource,
) {
    suspend fun browse(
        business: String,
        filter: CategoryFilter = CategoryFilter.All,
        offset: Int = 0,
    ): ApiResult<Page<Product>> = pages.page(business, filter, offset)

    suspend fun load(business: String, id: String): ApiResult<Product> =
        repository.get(business, id)

    suspend fun setActive(business: String, id: String, active: Boolean): ApiResult<Product> =
        repository.setActive(business, id, active)
}
