package com.yasminhosam.brandora.feature.products.domain

import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.products.domain.model.Page
import com.yasminhosam.brandora.feature.products.domain.model.Product

/** How the product list is narrowed to a category. */
sealed interface CategoryFilter {
    data object All : CategoryFilter
    data class Category(val id: String) : CategoryFilter
}

/**
 * Single seam between screens and category filtering.
 *
 * [com.yasminhosam.brandora.feature.products.data.page.FetchAllFilterPageSource] (bound in DI)
 * fills every page client-side because the backend `GET /products` accepts only limit/offset —
 * verified against backend `main` on 2026-11-07 (`products.controller.ts` takes `PaginationQueryDto`,
 * `products.service.ts` filters only by businessId, docs reject unknown query fields).
 * [com.yasminhosam.brandora.feature.products.data.page.ServerCategoryParamPageSource] switches the
 * same screens to `?categoryId=` once the backend supports it: one DI binding, zero screen changes.
 */
interface ProductPageSource {
    /**
     * Returns filtered items starting at backend [offset], gathering whole backend pages until at
     * least [targetCount] items match or the backend reports no more pages. The returned
     * pagination mirrors the last consumed backend page, so "Load more" resumes at its nextOffset
     * and never re-fetches or splits a page.
     */
    suspend fun page(
        business: String,
        filter: CategoryFilter,
        offset: Int = 0,
        targetCount: Int = 20,
    ): ApiResult<Page<Product>>
}
