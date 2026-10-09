package com.yasminhosam.brandora.core.ui

import androidx.annotation.StringRes
import com.yasminhosam.brandora.R
import com.yasminhosam.brandora.core.network.ApiErrorType

/**
 * Single ApiErrorType-to-message mapping. The `when` is exhaustive without `else`, so a new
 * error type fails compilation until it gets a message in both languages.
 */
@StringRes
fun ApiErrorType.messageRes(): Int = when (this) {
    ApiErrorType.NETWORK -> R.string.error_network
    ApiErrorType.TIMEOUT -> R.string.error_timeout
    ApiErrorType.UNAUTHORIZED -> R.string.error_unauthorized
    ApiErrorType.BAD_REQUEST -> R.string.error_bad_request
    ApiErrorType.NOT_FOUND -> R.string.error_not_found
    ApiErrorType.CONFLICT -> R.string.error_conflict
    ApiErrorType.FILE_TOO_LARGE -> R.string.error_file_too_large
    ApiErrorType.INVALID_FILE_TYPE -> R.string.error_invalid_file_type
    ApiErrorType.SERVER -> R.string.error_server
    ApiErrorType.UNKNOWN -> R.string.error_unknown
}
