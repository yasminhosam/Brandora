package com.yasminhosam.brandora.feature.products.data.page

import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.products.domain.CategoryFilter
import com.yasminhosam.brandora.feature.products.domain.ProductPageSource
import com.yasminhosam.brandora.feature.products.domain.ProductsRepository
import com.yasminhosam.brandora.feature.products.domain.model.Page
import com.yasminhosam.brandora.feature.products.domain.model.PageInfo
import com.yasminhosam.brandora.feature.products.domain.model.Product
import javax.inject.Inject

/**
 * Primary [ProductPageSource]: the backend has no category query param, so each requested page
 * gathers whole backend pages, keeps only the matching products, and stops once [targetCount]
 * filtered items are collected or the backend reports no more pages. Never filters only the
 * already-loaded page. Pagination mirrors the last consumed backend page so "Load more" resumes
 * exactly where the backend left off.
 */
class FetchAllFilterPageSource
@Inject
constructor(private val repository: ProductsRepository) : ProductPageSource {
    override suspend fun page(
        business: String,
        filter: CategoryFilter,
        offset: Int,
        targetCount: Int,
    ): ApiResult<Page<Product>> {
        val matches: (Product) -> Boolean =
            when (filter) {
                CategoryFilter.All -> ({ _: Product -> true })
                is CategoryFilter.Category -> ({ product -> product.categoryId == filter.id })
            }
        var nextOffset: Int? = offset
        val collected = mutableListOf<Product>()
        var last: Page<Product>? = null
        while (nextOffset != null) {
            val current = nextOffset
            when (val result = repository.list(business, current, categoryId = null)) {
                is ApiResult.Error -> return result
                is ApiResult.Success -> {
                    collected += result.data.items.filter(matches)
                    last = result.data
                    val following = result.data.pagination.nextOffset
                    nextOffset =
                        if (
                            collected.size >= targetCount ||
                                !result.data.pagination.hasMore ||
                                following == null ||
                                following <= current
                        ) {
                            null
                        } else {
                            following
                        }
                }
            }
        }
        // The loop always runs once (offset is a non-null Int), so last is set.
        val page = last ?: return ApiResult.Success(Page(emptyList(), PageInfo()))
        return ApiResult.Success(Page(collected.distinctBy { it.id }, page.pagination))
    }
}
