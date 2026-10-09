package com.yasminhosam.brandora.core.network

import com.yasminhosam.brandora.BuildConfig

object ApiConfig {
    // LAN backend: the test device must reach this host (same Wi-Fi for hardware).
    const val BASE_URL = "http://192.168.1.104:3000/"

    // Single read point; the value comes from brandora.businessId in local.properties.
    val CURRENT_BUSINESS_ID: String get() = BuildConfig.CURRENT_BUSINESS_ID

    const val MAX_UPLOAD_BYTES = 10L * 1024 * 1024 // Excel limit: 10 MiB (backend returns 413 above it)
    const val DEFAULT_LIMIT = 20
    const val DEFAULT_OFFSET = 0
}

object ApiEndpoints {
    const val CATALOG_ITEMS = "catalog-items"                                    // POST (create), GET (list)

    // Excel import flow: analyze -> mapping -> validate -> preview -> execute
    const val IMPORT_ANALYZE = "businesses/{businessId}/imports/excel/analyze"   // POST multipart
    const val IMPORT_MAPPING = "imports/{importId}/mapping"                      // GET suggestions, PATCH confirm
    const val IMPORT_VALIDATE = "imports/{importId}/validate"                    // POST
    const val IMPORT_PREVIEW = "imports/{importId}/preview"                      // GET
    const val IMPORT_EXECUTE = "imports/{importId}/execute"                      // POST
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
    const val MAPPING_REQUIRED = "MAPPING_REQUIRED"
    const val READY = "READY"
    const val COMPLETED = "COMPLETED"
    const val COMPLETED_WITH_ERRORS = "COMPLETED_WITH_ERRORS"
    const val FAILED = "FAILED"
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