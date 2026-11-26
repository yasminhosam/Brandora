package com.yasminhosam.brandora.feature.products.domain

import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.products.domain.model.*

interface ProductImageReader {
    suspend fun read(uris: List<String>): List<ImageUpload>
}

interface ProductsRepository {
    /** [categoryId] is the server-side filter seam; the bound source passes null today. */
    suspend fun list(
        business: String,
        offset: Int,
        categoryId: String? = null,
    ): ApiResult<Page<Product>>

    suspend fun get(business: String, id: String): ApiResult<Product>

    suspend fun save(business: String, id: String?, draft: ProductDraft): ApiResult<Product>

    suspend fun setActive(business: String, id: String, active: Boolean): ApiResult<Product>

    suspend fun categories(business: String, offset: Int): ApiResult<Page<Category>>

    suspend fun createCategory(business: String, name: String): ApiResult<Category>

    suspend fun renameCategory(business: String, id: String, name: String): ApiResult<Category>

    suspend fun deleteCategory(business: String, id: String): ApiResult<Unit>

    suspend fun variants(business: String, product: String, offset: Int): ApiResult<Page<Variant>>

    suspend fun saveVariant(
        business: String,
        product: String,
        id: String?,
        draft: VariantDraft,
    ): ApiResult<Variant>

    /** State-only PATCH: isActive travels alone so nulls never clear sku or price. */
    suspend fun setVariantActive(
        business: String,
        product: String,
        variant: String,
        active: Boolean,
    ): ApiResult<Variant>

    suspend fun upload(business: String, product: String, files: List<ImageUpload>): ApiResult<Images>

    suspend fun main(business: String, product: String, image: String): ApiResult<Images>

    suspend fun order(business: String, product: String, ids: List<String>): ApiResult<Images>

    suspend fun deleteImage(business: String, product: String, image: String): ApiResult<ImageDelete>

    suspend fun adjust(
        business: String,
        product: String,
        variant: String,
        quantity: Int?,
    ): ApiResult<InventoryAdjustment>

    suspend fun movements(
        business: String,
        product: String,
        variant: String,
        offset: Int,
    ): ApiResult<Page<StockMovement>>
}
