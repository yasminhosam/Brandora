package com.yasminhosam.brandora.feature.products.domain.usecase

import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.products.domain.ProductRules
import com.yasminhosam.brandora.feature.products.domain.ProductsRepository
import com.yasminhosam.brandora.feature.products.domain.model.Category
import com.yasminhosam.brandora.feature.products.domain.model.Page
import javax.inject.Inject

/** Category reads and writes; names follow the backend's blank/length rules before any call. */
class CategoryUseCases @Inject constructor(private val repository: ProductsRepository) {
    suspend fun list(business: String, offset: Int = 0): ApiResult<Page<Category>> =
        repository.categories(business, offset)

    suspend fun create(business: String, name: String): ApiResult<Category> {
        val trimmed = name.trim()
        if (!ProductRules.validCategoryName(trimmed)) return invalidName()
        return repository.createCategory(business, trimmed)
    }

    suspend fun rename(business: String, id: String, name: String): ApiResult<Category> {
        val trimmed = name.trim()
        if (!ProductRules.validCategoryName(trimmed)) return invalidName()
        return repository.renameCategory(business, id, trimmed)
    }

    /** In-use categories surface CATEGORY_IN_USE from the server; the confirm sheet owns UX. */
    suspend fun delete(business: String, id: String): ApiResult<Unit> =
        repository.deleteCategory(business, id)

    private fun invalidName(): ApiResult.Error =
        ApiResult.Error(ApiErrorType.BAD_REQUEST, errorCode = "VALIDATION_ERROR")
}
