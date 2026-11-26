package com.yasminhosam.brandora.feature.products.presentation

import com.yasminhosam.brandora.R
import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.core.ui.messageRes
import org.junit.Assert.*
import org.junit.Test

class ProductsErrorMapperTest {
    private fun error(code: String?, type: ApiErrorType = ApiErrorType.BAD_REQUEST) =
        ApiResult.Error(type, errorCode = code)

    @Test
    fun `server conflict and lifecycle codes map to their own messages`() {
        assertEquals(
            R.string.pm_external_conflict,
            ProductsErrorMapper.messageRes(error("PRODUCT_EXTERNAL_KEY_CONFLICT")),
        )
        assertEquals(
            R.string.pm_variant_sku_conflict,
            ProductsErrorMapper.messageRes(error("VARIANT_SKU_CONFLICT")),
        )
        assertEquals(
            R.string.pm_variant_options_conflict,
            ProductsErrorMapper.messageRes(error("VARIANT_OPTIONS_CONFLICT")),
        )
        assertEquals(
            R.string.pm_options_in_use,
            ProductsErrorMapper.messageRes(error("PRODUCT_OPTIONS_IN_USE")),
        )
        assertEquals(
            R.string.pm_write_conflict,
            ProductsErrorMapper.messageRes(error("PRODUCT_WRITE_CONFLICT")),
        )
        assertEquals(
            R.string.pm_category_in_use,
            ProductsErrorMapper.messageRes(error("CATEGORY_IN_USE")),
        )
        assertEquals(
            R.string.pm_inventory_conflict,
            ProductsErrorMapper.messageRes(error("INVENTORY_CONFLICT")),
        )
        assertEquals(
            R.string.pm_inventory_inactive,
            ProductsErrorMapper.messageRes(error("INVENTORY_INACTIVE")),
        )
        assertEquals(
            R.string.pm_inactive_error,
            ProductsErrorMapper.messageRes(error("PRODUCT_INACTIVE")),
        )
    }

    @Test
    fun `client side validation codes share the same table`() {
        assertEquals(
            R.string.pm_validation_error,
            ProductsErrorMapper.messageRes(error("VALIDATION_ERROR")),
        )
        assertEquals(
            R.string.pm_variant_options_invalid,
            ProductsErrorMapper.messageRes(error("VARIANT_OPTIONS_INVALID")),
        )
        assertEquals(
            R.string.pm_quantity_invalid,
            ProductsErrorMapper.messageRes(error("INVENTORY_QUANTITY_INVALID")),
        )
        assertEquals(
            R.string.pm_image_invalid,
            ProductsErrorMapper.messageRes(error("PRODUCT_IMAGE_INVALID")),
        )
        assertEquals(
            R.string.pm_image_invalid,
            ProductsErrorMapper.messageRes(error("PRODUCT_IMAGE_TOO_LARGE")),
        )
        assertEquals(
            R.string.pm_image_unavailable,
            ProductsErrorMapper.messageRes(error("PRODUCT_IMAGE_UNAVAILABLE")),
        )
    }

    @Test
    fun `unknown codes fall back to the error type message`() {
        assertEquals(
            ApiErrorType.NETWORK.messageRes(),
            ProductsErrorMapper.messageRes(error(null, ApiErrorType.NETWORK)),
        )
        assertEquals(
            ApiErrorType.BAD_REQUEST.messageRes(),
            ProductsErrorMapper.messageRes(error("SOMETHING_NEW")),
        )
    }

    @Test
    fun `404 causes reach their specific messages through the type fallback`() {
        assertEquals(
            R.string.error_business_not_found,
            ProductsErrorMapper.messageRes(error("BUSINESS_NOT_FOUND", ApiErrorType.NOT_FOUND)),
        )
        assertEquals(
            R.string.error_product_not_found,
            ProductsErrorMapper.messageRes(error("PRODUCT_NOT_FOUND", ApiErrorType.NOT_FOUND)),
        )
        assertEquals(
            R.string.error_variant_not_found,
            ProductsErrorMapper.messageRes(error("VARIANT_NOT_FOUND", ApiErrorType.NOT_FOUND)),
        )
        assertEquals(
            R.string.error_product_image_not_found,
            ProductsErrorMapper.messageRes(error("PRODUCT_IMAGE_NOT_FOUND", ApiErrorType.NOT_FOUND)),
        )
    }

    @Test
    fun `only write conflicts offer retry`() {
        assertTrue(
            ProductsErrorMapper.isRetryable(ApiResult.Error(ApiErrorType.CONFLICT, errorCode = "PRODUCT_WRITE_CONFLICT"))
        )
        assertFalse(
            ProductsErrorMapper.isRetryable(ApiResult.Error(ApiErrorType.CONFLICT, errorCode = "CATEGORY_IN_USE"))
        )
        assertFalse(
            ProductsErrorMapper.isRetryable(ApiResult.Error(ApiErrorType.NETWORK))
        )
    }
}
