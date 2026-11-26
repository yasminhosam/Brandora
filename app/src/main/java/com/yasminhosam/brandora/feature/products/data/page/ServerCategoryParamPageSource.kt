package com.yasminhosam.brandora.feature.products.data.page

import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.products.domain.CategoryFilter
import com.yasminhosam.brandora.feature.products.domain.ProductPageSource
import com.yasminhosam.brandora.feature.products.domain.ProductsRepository
import com.yasminhosam.brandora.feature.products.domain.model.Page
import com.yasminhosam.brandora.feature.products.domain.model.Product
import javax.inject.Inject

/**
 * Server-side filtering seam: a single backend call carrying `?categoryId=`. Not bound in DI yet —
 * the current backend rejects unknown query params (verified 2026-11-07). Rebinding the
 * ProductPageSource interface to this class is the entire migration once support lands.
 */
class ServerCategoryParamPageSource
@Inject
constructor(private val repository: ProductsRepository) : ProductPageSource {
    override suspend fun page(
        business: String,
        filter: CategoryFilter,
        offset: Int,
        targetCount: Int,
    ): ApiResult<Page<Product>> =
        repository.list(business, offset, categoryId = (filter as? CategoryFilter.Category)?.id)
}
