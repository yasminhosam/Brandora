package com.yasminhosam.brandora.feature.products.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

/**
 * Typed Retrofit contract for Products/Categories/Variants/Images/Inventory. [categoryId] on the
 * list call is the server-side filter seam; it stays null under the bound fallback implementation
 * because the current backend rejects unknown query params. Bodies are pre-encoded by the data
 * layer with [writeBody] so explicit-null PATCH payloads survive encoding.
 */
internal interface ProductsApiService {
    @GET("businesses/{businessId}/products")
    suspend fun products(
        @Path("businessId") business: String,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("categoryId") categoryId: String? = null,
    ): PageDto<ProductDto>

    @GET("businesses/{businessId}/products/{productId}")
    suspend fun product(
        @Path("businessId") business: String,
        @Path("productId") product: String,
    ): ProductDto

    @POST("businesses/{businessId}/products")
    suspend fun create(
        @Path("businessId") business: String,
        @Body body: RequestBody,
    ): ProductDto

    @PATCH("businesses/{businessId}/products/{productId}")
    suspend fun update(
        @Path("businessId") business: String,
        @Path("productId") product: String,
        @Body body: RequestBody,
    ): ProductDto

    @PATCH("businesses/{businessId}/products/{productId}")
    suspend fun updateActive(
        @Path("businessId") business: String,
        @Path("productId") product: String,
        @Body body: RequestBody,
    ): ProductDto

    @GET("businesses/{businessId}/categories")
    suspend fun categories(
        @Path("businessId") business: String,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0,
    ): PageDto<CategoryDto>

    @POST("businesses/{businessId}/categories")
    suspend fun createCategory(
        @Path("businessId") business: String,
        @Body body: RequestBody,
    ): CategoryDto

    @PATCH("businesses/{businessId}/categories/{categoryId}")
    suspend fun renameCategory(
        @Path("businessId") business: String,
        @Path("categoryId") category: String,
        @Body body: RequestBody,
    ): CategoryDto

    @DELETE("businesses/{businessId}/categories/{categoryId}")
    suspend fun deleteCategory(
        @Path("businessId") business: String,
        @Path("categoryId") category: String,
    )

    @GET("businesses/{businessId}/products/{productId}/variants")
    suspend fun variants(
        @Path("businessId") business: String,
        @Path("productId") product: String,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
    ): PageDto<VariantDto>

    @POST("businesses/{businessId}/products/{productId}/variants")
    suspend fun createVariant(
        @Path("businessId") business: String,
        @Path("productId") product: String,
        @Body body: RequestBody,
    ): VariantDto

    @PATCH("businesses/{businessId}/products/{productId}/variants/{variantId}")
    suspend fun updateVariant(
        @Path("businessId") business: String,
        @Path("productId") product: String,
        @Path("variantId") variant: String,
        @Body body: RequestBody,
    ): VariantDto

    @GET("businesses/{businessId}/products/{productId}/images")
    suspend fun images(
        @Path("businessId") business: String,
        @Path("productId") product: String,
    ): ImagesDto

    @Multipart
    @POST("businesses/{businessId}/products/{productId}/images")
    suspend fun upload(
        @Path("businessId") business: String,
        @Path("productId") product: String,
        @Part files: List<MultipartBody.Part>,
    ): ImagesDto

    @PATCH("businesses/{businessId}/products/{productId}/images/{imageId}")
    suspend fun main(
        @Path("businessId") business: String,
        @Path("productId") product: String,
        @Path("imageId") image: String,
        @Body body: RequestBody,
    ): ImagesDto

    @PATCH("businesses/{businessId}/products/{productId}/images")
    suspend fun order(
        @Path("businessId") business: String,
        @Path("productId") product: String,
        @Body body: RequestBody,
    ): ImagesDto

    @DELETE("businesses/{businessId}/products/{productId}/images/{imageId}")
    suspend fun deleteImage(
        @Path("businessId") business: String,
        @Path("productId") product: String,
        @Path("imageId") image: String,
    ): ImageDeleteDto

    @POST("businesses/{businessId}/products/{productId}/variants/{variantId}/inventory/adjust")
    suspend fun adjust(
        @Path("businessId") business: String,
        @Path("productId") product: String,
        @Path("variantId") variant: String,
        @Body body: RequestBody,
    ): InventoryAdjustmentDto

    @GET("businesses/{businessId}/products/{productId}/variants/{variantId}/inventory/movements")
    suspend fun movements(
        @Path("businessId") business: String,
        @Path("productId") product: String,
        @Path("variantId") variant: String,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
    ): PageDto<StockMovementDto>
}
