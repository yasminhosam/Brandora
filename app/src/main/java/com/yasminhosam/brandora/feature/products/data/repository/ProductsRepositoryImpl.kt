package com.yasminhosam.brandora.feature.products.data.repository

import com.yasminhosam.brandora.core.network.*
import com.yasminhosam.brandora.feature.products.data.mapper.*
import com.yasminhosam.brandora.feature.products.data.remote.*
import com.yasminhosam.brandora.feature.products.domain.ProductsRepository
import com.yasminhosam.brandora.feature.products.domain.model.*
import javax.inject.Inject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit

class ProductsRepositoryImpl
@Inject
constructor(retrofit: Retrofit, private val errors: ApiErrorHandler) : ProductsRepository {
    private val api = retrofit.create(ProductsApiService::class.java)

    override suspend fun list(business: String, offset: Int, categoryId: String?) =
        errors.safeApiCall {
            api.products(business, offset = offset, categoryId = categoryId).toDomain {
                it.toDomain()
            }
        }

    override suspend fun get(business: String, id: String) = errors.safeApiCall {
        api.product(business, id).toDomain()
    }

    override suspend fun save(business: String, id: String?, draft: ProductDraft) =
        errors.safeApiCall {
            val body = writeBody(draft.toWrite())
            if (id == null) api.create(business, body).toDomain()
            else api.update(business, id, body).toDomain()
        }

    override suspend fun setActive(business: String, id: String, active: Boolean) =
        errors.safeApiCall {
            api.updateActive(business, id, writeBody(ActiveWriteDto(active))).toDomain()
        }

    override suspend fun categories(business: String, offset: Int) = errors.safeApiCall {
        api.categories(business, offset = offset).toDomain { it.toDomain() }
    }

    override suspend fun createCategory(business: String, name: String) = errors.safeApiCall {
        api.createCategory(business, writeBody(CategoryWriteDto(name))).toDomain()
    }

    override suspend fun renameCategory(business: String, id: String, name: String) =
        errors.safeApiCall {
            api.renameCategory(business, id, writeBody(CategoryWriteDto(name))).toDomain()
        }

    override suspend fun deleteCategory(business: String, id: String) = errors.safeApiCall {
        api.deleteCategory(business, id)
    }

    override suspend fun variants(business: String, product: String, offset: Int) =
        errors.safeApiCall {
            api.variants(business, product, offset = offset).toDomain { it.toDomain() }
        }

    override suspend fun saveVariant(
        business: String,
        product: String,
        id: String?,
        draft: VariantDraft,
    ) = errors.safeApiCall {
        if (id == null) api.createVariant(business, product, writeBody(draft.toCreateWrite()))
            .toDomain()
        else api.updateVariant(business, product, id, writeBody(draft.toUpdateWrite())).toDomain()
    }

    override suspend fun setVariantActive(
        business: String,
        product: String,
        variant: String,
        active: Boolean,
    ) = errors.safeApiCall {
        api.updateVariant(business, product, variant, writeBody(ActiveWriteDto(active))).toDomain()
    }

    override suspend fun upload(business: String, product: String, files: List<ImageUpload>) =
        errors.safeApiCall {
            api.upload(
                business,
                product,
                files.mapIndexed { i, f ->
                    MultipartBody.Part.createFormData(
                        "files",
                        "image-" +
                            i +
                            if (f.mime == "image/png") ".png"
                            else if (f.mime == "image/webp") ".webp" else ".jpg",
                        f.bytes.toRequestBody(f.mime.toMediaType()),
                    )
                },
            ).toDomain()
        }

    override suspend fun main(business: String, product: String, image: String) =
        errors.safeApiCall {
            api.main(business, product, image, writeBody(IsMainWriteDto(true))).toDomain()
        }

    override suspend fun order(business: String, product: String, ids: List<String>) =
        errors.safeApiCall {
            api.order(business, product, writeBody(ImageOrderWriteDto(ids))).toDomain()
        }

    override suspend fun deleteImage(business: String, product: String, image: String) =
        errors.safeApiCall { api.deleteImage(business, product, image).toDomain() }

    override suspend fun adjust(
        business: String,
        product: String,
        variant: String,
        quantity: Int?,
    ) = errors.safeApiCall {
        api.adjust(
            business,
            product,
            variant,
            writeBody(StockAdjustWriteDto(quantity = quantity, trackInventory = quantity != null)),
        ).toDomain()
    }

    override suspend fun movements(
        business: String,
        product: String,
        variant: String,
        offset: Int,
    ) = errors.safeApiCall {
        api.movements(business, product, variant, offset = offset).toDomain { it.toDomain() }
    }
}
