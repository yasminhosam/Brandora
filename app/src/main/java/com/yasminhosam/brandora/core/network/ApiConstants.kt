package com.yasminhosam.brandora.core.network

import com.yasminhosam.brandora.BuildConfig

object ApiConfig {
    // Comes from brandora.baseUrl in local.properties (always ends with '/').
    val BASE_URL: String get() = BuildConfig.BASE_URL

    const val MAX_UPLOAD_BYTES = 10L * 1024 * 1024 // Excel limit: 10 MiB (backend returns 413 above it)
    const val DEFAULT_LIMIT = 20
    const val DEFAULT_OFFSET = 0
}

object ApiEndpoints {
    const val BUSINESSES = "businesses"                                          // POST (create)
    const val CATALOG_ITEMS = "catalog-items"                                    // POST (create), GET (list)

    // Excel import flow: analyze -> mapping -> validate -> preview -> execute
    const val IMPORT_ANALYZE = "businesses/{businessId}/imports/excel/analyze"   // POST multipart
    const val IMPORT_MAPPING = "imports/{importId}/mapping"                      // GET suggestions, PATCH confirm
    const val IMPORT_VALIDATE = "imports/{importId}/validate"                    // POST
    const val IMPORT_VALIDATE_RESULTS = "imports/{importId}/validation-results"  // GET
    const val IMPORT_PREVIEW = "imports/{importId}/preview"                      // GET
    const val IMPORT_EXECUTE = "imports/{importId}/execute"                      // POST
    const val IMPORT_STATUS = "imports/{importId}"                               // GET (job status)
}

// Names used inside @Path / @Query / @Part annotations
object ApiParams {
    const val PATH_BUSINESS_ID = "businessId"
    const val PATH_IMPORT_ID = "importId"

    const val QUERY_BUSINESS_ID = "businessId"
    const val QUERY_LIMIT = "limit"
    const val QUERY_OFFSET = "offset"

    const val PART_FILE = "file"              // required .xlsx
    const val PART_SHEET_NAME = "sheetName"   // optional, falls back to the first sheet
}

// Values the backend sends or expects. Keep exactly as written.
object ImportStatusValues {
    const val UPLOADED = "UPLOADED"
    const val ANALYZING = "ANALYZING"
    const val MAPPING_REQUIRED = "MAPPING_REQUIRED"
    const val VALIDATING = "VALIDATING"
    const val READY = "READY"
    const val IMPORTING = "IMPORTING"
    const val COMPLETED = "COMPLETED"
    const val COMPLETED_WITH_ERRORS = "COMPLETED_WITH_ERRORS"
    const val FAILED = "FAILED"
    const val CANCELLED = "CANCELLED"
}

object MappingTypeValues {
    const val STANDARD = "STANDARD"
    const val ATTRIBUTE = "ATTRIBUTE"
    const val VARIANT_ATTRIBUTE = "VARIANT_ATTRIBUTE"
    const val IGNORE = "IGNORE"
    const val UNMAPPED = "UNMAPPED"
}

// Allowed targets when type == STANDARD
object StandardTargets {
    const val NAME = "name"                    // required before confirming the mapping
    const val EXTERNAL_KEY = "externalKey"
    const val DESCRIPTION = "description"
    const val CATEGORY = "category"
    const val BASE_PRICE = "basePrice"
    const val STOCK_QUANTITY = "stockQuantity"
}

object ImportRowActions {
    const val INSERT = "INSERT"
    const val UPDATE = "UPDATE"
}

// Stable backend identifiers for client-side localization. Never show or parse "message".
object ImportErrorCodes {
    const val MAPPING_NAME_REQUIRED = "MAPPING_NAME_REQUIRED"
    const val MAPPING_SOURCE_INVALID = "MAPPING_SOURCE_INVALID"
    const val MAPPING_SOURCE_DUPLICATE = "MAPPING_SOURCE_DUPLICATE"
    const val MAPPING_STANDARD_TARGET_INVALID = "MAPPING_STANDARD_TARGET_INVALID"
    const val MAPPING_STANDARD_TARGET_DUPLICATE = "MAPPING_STANDARD_TARGET_DUPLICATE"
    const val MAPPING_ATTRIBUTE_TARGET_INVALID = "MAPPING_ATTRIBUTE_TARGET_INVALID"
    const val MAPPING_ATTRIBUTE_TARGET_DUPLICATE = "MAPPING_ATTRIBUTE_TARGET_DUPLICATE"
    const val MAPPING_TARGET_MUST_BE_NULL = "MAPPING_TARGET_MUST_BE_NULL"
    const val MAPPING_TYPE_INVALID = "MAPPING_TYPE_INVALID"
    const val VALIDATION_ERROR = "VALIDATION_ERROR"
    const val IMPORT_JOB_NOT_FOUND = "IMPORT_JOB_NOT_FOUND"
    const val IMPORT_NOT_AWAITING_MAPPING = "IMPORT_NOT_AWAITING_MAPPING"
    const val IMPORT_MAPPING_CHANGED = "IMPORT_MAPPING_CHANGED"
    const val IMPORT_WORKBOOK_UNAVAILABLE = "IMPORT_WORKBOOK_UNAVAILABLE"
    const val IMPORT_WORKBOOK_READ_FAILED = "IMPORT_WORKBOOK_READ_FAILED"
    const val IMPORT_VALIDATION_FAILED = "IMPORT_VALIDATION_FAILED"
    const val IMPORT_NOT_READY = "IMPORT_NOT_READY"
    const val IMPORT_EXECUTION_ALREADY_STARTED = "IMPORT_EXECUTION_ALREADY_STARTED"
    const val IMPORT_EXECUTION_FAILED = "IMPORT_EXECUTION_FAILED"
    const val INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR"
}

// Per-field row-validation codes (validation-results / preview invalid rows).
object ValidationErrorCodes {
    const val REQUIRED = "REQUIRED"
    const val TEXT_TOO_LONG = "TEXT_TOO_LONG"
    const val INVALID_DECIMAL = "INVALID_DECIMAL"
    const val INVALID_INTEGER = "INVALID_INTEGER"
    const val DUPLICATE_EXTERNAL_KEY = "DUPLICATE_EXTERNAL_KEY"
}

// Job-level codes on execute failedRows (a valid row that failed during execution).
object ExecuteErrorCodes {
    const val EXECUTION_CONFLICT = "EXECUTION_CONFLICT"
}
