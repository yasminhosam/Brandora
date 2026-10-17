package com.yasminhosam.brandora.core.ui

import androidx.annotation.StringRes
import com.yasminhosam.brandora.R
import com.yasminhosam.brandora.core.network.ApiErrorType
import com.yasminhosam.brandora.core.network.ExecuteErrorCodes
import com.yasminhosam.brandora.core.network.ImportErrorCodes
import com.yasminhosam.brandora.core.network.StandardTargets
import com.yasminhosam.brandora.core.network.ValidationErrorCodes

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

@StringRes
fun String?.mappingErrorRes(): Int = when (this) {
    ImportErrorCodes.MAPPING_NAME_REQUIRED,
    ImportErrorCodes.MAPPING_TYPE_INVALID,
    ImportErrorCodes.MAPPING_TARGET_MUST_BE_NULL,
    ImportErrorCodes.VALIDATION_ERROR -> R.string.mapping_error_invalid
    ImportErrorCodes.MAPPING_SOURCE_INVALID,
    ImportErrorCodes.MAPPING_SOURCE_DUPLICATE -> R.string.mapping_error_source
    ImportErrorCodes.MAPPING_STANDARD_TARGET_INVALID,
    ImportErrorCodes.MAPPING_STANDARD_TARGET_DUPLICATE -> R.string.mapping_error_target
    ImportErrorCodes.MAPPING_ATTRIBUTE_TARGET_INVALID,
    ImportErrorCodes.MAPPING_ATTRIBUTE_TARGET_DUPLICATE -> R.string.mapping_error_key
    ImportErrorCodes.IMPORT_NOT_AWAITING_MAPPING,
    ImportErrorCodes.IMPORT_MAPPING_CHANGED -> R.string.mapping_error_stale
    ImportErrorCodes.IMPORT_JOB_NOT_FOUND -> R.string.mapping_error_job_gone
    ImportErrorCodes.IMPORT_WORKBOOK_UNAVAILABLE,
    ImportErrorCodes.IMPORT_WORKBOOK_READ_FAILED -> R.string.mapping_error_file_gone
    else -> R.string.mapping_error_unknown
}

/**
 * Per-field validation message, keyed by the backend's stable code. Known codes are explicit so
 * a renamed code is caught here; the fallback keeps an unrecognized backend code displayable.
 */
@StringRes
fun String.validationErrorRes(): Int = when (this) {
    ValidationErrorCodes.REQUIRED -> R.string.validation_error_required
    ValidationErrorCodes.TEXT_TOO_LONG -> R.string.validation_error_too_long
    ValidationErrorCodes.INVALID_DECIMAL -> R.string.validation_error_decimal
    ValidationErrorCodes.INVALID_INTEGER -> R.string.validation_error_integer
    ValidationErrorCodes.DUPLICATE_EXTERNAL_KEY -> R.string.validation_error_duplicate_sku
    else -> R.string.validation_error_field_generic
}

/** Human label for a raw field key (e.g. "basePrice"), reusing the mapping target labels. */
@StringRes
fun String.fieldLabelRes(): Int = when (this) {
    StandardTargets.NAME -> R.string.mapping_target_name
    StandardTargets.EXTERNAL_KEY -> R.string.mapping_target_external_key
    StandardTargets.DESCRIPTION -> R.string.mapping_target_description
    StandardTargets.CATEGORY -> R.string.mapping_target_category
    StandardTargets.BASE_PRICE -> R.string.mapping_target_base_price
    StandardTargets.STOCK_QUANTITY -> R.string.mapping_target_stock_quantity
    else -> R.string.validation_field_generic
}

/** Message for a valid row that failed during execution, keyed by its job-level code. */
@StringRes
fun String.executeErrorRes(): Int = when (this) {
    ExecuteErrorCodes.EXECUTION_CONFLICT -> R.string.result_error_execution_conflict
    else -> R.string.result_error_execution_generic
}
