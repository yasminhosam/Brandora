package com.yasminhosam.brandora.feature.products.domain.usecase

import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.feature.products.domain.ProductImageReader
import com.yasminhosam.brandora.feature.products.domain.ProductRules
import com.yasminhosam.brandora.feature.products.domain.ProductsRepository
import com.yasminhosam.brandora.feature.products.domain.model.ImageDelete
import com.yasminhosam.brandora.feature.products.domain.model.Images
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

/**
 * Image writes with the batch cap and file reading folded in: an oversized batch or an unreadable
 * file surfaces PRODUCT_IMAGE_INVALID before anything touches the network.
 */
class ImageUseCases
@Inject
constructor(
    private val repository: ProductsRepository,
    private val reader: ProductImageReader,
) {
    suspend fun upload(
        business: String,
        productId: String,
        uris: List<String>,
        existing: Int,
    ): ApiResult<Images> {
        if (!ProductRules.batchFits(existing, uris.size))
            return ApiResult.Error(ApiErrorType.BAD_REQUEST, errorCode = "PRODUCT_IMAGE_INVALID")
        val files =
            try {
                reader.read(uris)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                return ApiResult.Error(ApiErrorType.BAD_REQUEST, errorCode = "PRODUCT_IMAGE_INVALID")
            }
        return repository.upload(business, productId, files)
    }

    suspend fun setMain(business: String, productId: String, imageId: String): ApiResult<Images> =
        repository.main(business, productId, imageId)

    /**
     * Reorder request. Returns null when [from]/[to] are out of range: the move is ignored and
     * nothing is sent.
     */
    suspend fun reorder(
        business: String,
        productId: String,
        ids: List<String>,
        from: Int,
        to: Int,
    ): ApiResult<Images>? {
        val ordered = ProductRules.orderedIds(ids, from, to) ?: return null
        return repository.order(business, productId, ordered)
    }

    suspend fun delete(business: String, productId: String, imageId: String): ApiResult<ImageDelete> =
        repository.deleteImage(business, productId, imageId)
}
