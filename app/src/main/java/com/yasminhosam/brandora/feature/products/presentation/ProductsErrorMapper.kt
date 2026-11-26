package com.yasminhosam.brandora.feature.products.presentation

import androidx.annotation.StringRes
import com.yasminhosam.brandora.R
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.core.ui.messageRes

/**
 * Single code → message table for product errors. Server codes and the client-side validation
 * codes the use cases emit share one vocabulary, so both land here; an unknown code falls back to
 * the ApiErrorType message. [isRetryable] marks failures where refreshing and re-running the
 * failed event can succeed — the error banner offers Retry for those.
 */
object ProductsErrorMapper {
    @StringRes
    fun messageRes(error: ApiResult.Error): Int =
        when (error.errorCode) {
            "PRODUCT_EXTERNAL_KEY_CONFLICT" -> R.string.pm_external_conflict
            "VARIANT_SKU_CONFLICT" -> R.string.pm_variant_sku_conflict
            "VARIANT_OPTIONS_CONFLICT" -> R.string.pm_variant_options_conflict
            "PRODUCT_OPTIONS_IN_USE" -> R.string.pm_options_in_use
            "PRODUCT_IMAGE_INVALID",
            "PRODUCT_IMAGE_TOO_LARGE" -> R.string.pm_image_invalid
            "PRODUCT_IMAGE_UNAVAILABLE" -> R.string.pm_image_unavailable
            "PRODUCT_WRITE_CONFLICT" -> R.string.pm_write_conflict
            "VARIANT_OPTIONS_INVALID" -> R.string.pm_variant_options_invalid
            "VALIDATION_ERROR" -> R.string.pm_validation_error
            "INVENTORY_CONFLICT" -> R.string.pm_inventory_conflict
            "INVENTORY_INACTIVE" -> R.string.pm_inventory_inactive
            "PRODUCT_INACTIVE" -> R.string.pm_inactive_error
            "CATEGORY_IN_USE" -> R.string.pm_category_in_use
            "INVENTORY_QUANTITY_INVALID" -> R.string.pm_quantity_invalid
            else -> error.messageRes()
        }

    fun isRetryable(error: ApiResult.Error) = error.errorCode == "PRODUCT_WRITE_CONFLICT"
}
