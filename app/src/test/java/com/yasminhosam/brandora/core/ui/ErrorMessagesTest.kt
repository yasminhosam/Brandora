package com.yasminhosam.brandora.core.ui

import com.yasminhosam.brandora.R
import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.core.network.ImportErrorCodes
import org.junit.Assert.assertEquals
import org.junit.Test

/** Covers ApiResult.Error.messageRes(): code-specific messages with a type fallback. */
class ErrorMessagesTest {

    private fun error(code: String?, type: ApiErrorType = ApiErrorType.NOT_FOUND) =
        ApiResult.Error(type, errorCode = code)

    @Test
    fun `404 causes map to their specific messages`() {
        assertEquals(R.string.error_business_not_found, error("BUSINESS_NOT_FOUND").messageRes())
        assertEquals(
            R.string.mapping_error_job_gone,
            error(ImportErrorCodes.IMPORT_JOB_NOT_FOUND).messageRes(),
        )
        assertEquals(R.string.error_product_not_found, error("PRODUCT_NOT_FOUND").messageRes())
        assertEquals(R.string.error_variant_not_found, error("VARIANT_NOT_FOUND").messageRes())
        assertEquals(
            R.string.error_product_image_not_found,
            error("PRODUCT_IMAGE_NOT_FOUND").messageRes(),
        )
        assertEquals(R.string.error_excel_structure, error("EXCEL_SHEET_NOT_FOUND").messageRes())
        assertEquals(R.string.error_excel_structure, error("EXCEL_HEADER_NOT_FOUND").messageRes())
    }

    @Test
    fun `excel file problems name the file instead of the generic bad request`() {
        assertEquals(
            R.string.error_invalid_file_type,
            error("EXCEL_FILE_UNSUPPORTED", ApiErrorType.BAD_REQUEST).messageRes(),
        )
        assertEquals(
            R.string.error_invalid_file_type,
            error("EXCEL_FILE_REQUIRED", ApiErrorType.BAD_REQUEST).messageRes(),
        )
        assertEquals(
            R.string.error_excel_invalid,
            error("EXCEL_FILE_INVALID", ApiErrorType.BAD_REQUEST).messageRes(),
        )
        assertEquals(
            R.string.error_excel_invalid,
            error("EXCEL_FILE_UNREADABLE", ApiErrorType.BAD_REQUEST).messageRes(),
        )
        assertEquals(
            R.string.error_file_too_large,
            error("EXCEL_FILE_TOO_LARGE", ApiErrorType.FILE_TOO_LARGE).messageRes(),
        )
    }

    @Test
    fun `errors without a code fall back to the type message`() {
        assertEquals(ApiErrorType.NOT_FOUND.messageRes(), error(null).messageRes())
        // The framework's unmatched-route code keeps today's generic text.
        assertEquals(ApiErrorType.NOT_FOUND.messageRes(), error("NOT_FOUND").messageRes())
        assertEquals(
            ApiErrorType.NETWORK.messageRes(),
            error(null, ApiErrorType.NETWORK).messageRes(),
        )
        assertEquals(
            ApiErrorType.TIMEOUT.messageRes(),
            error(null, ApiErrorType.TIMEOUT).messageRes(),
        )
    }

    @Test
    fun `unknown codes degrade to the type message instead of failing`() {
        assertEquals(
            ApiErrorType.BAD_REQUEST.messageRes(),
            error("SOMETHING_NEW", ApiErrorType.BAD_REQUEST).messageRes(),
        )
        assertEquals(
            ApiErrorType.CONFLICT.messageRes(),
            error(null, ApiErrorType.CONFLICT).messageRes(),
        )
    }
}
