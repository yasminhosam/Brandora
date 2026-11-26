package com.yasminhosam.brandora.core.network

import com.yasminhosam.brandora.BuildConfig
import com.yasminhosam.brandora.feature.inbox.domain.model.Customer

object ApiConfig {
    // Comes from brandora.baseUrl in local.properties (always ends with '/').
    val BASE_URL: String
        get() = BuildConfig.BASE_URL

    const val MAX_UPLOAD_BYTES =
        10L * 1024 * 1024 // Excel limit: 10 MiB (backend returns 413 above it)
    const val DEFAULT_LIMIT = 20
    const val DEFAULT_OFFSET = 0

    // Cooldown assumed when a 429/503 arrives without a usable Retry-After (numeric seconds).
    const val RATE_LIMIT_FALLBACK_SECONDS = 30L
}

object ApiEndpoints {
    const val BUSINESSES = "businesses" // POST (create)

    // Auth routes are public and use the dedicated auth client. Recovery is OTP-only.
    const val AUTH_REGISTER = "auth/register" // POST
    const val AUTH_LOGIN = "auth/login" // POST
    const val AUTH_REFRESH = "auth/refresh" // POST (single-use refresh token)
    const val AUTH_LOGOUT = "auth/logout" // POST
    const val AUTH_FORGOT_PASSWORD = "auth/forgot-password" // POST
    const val AUTH_RESET_PASSWORD = "auth/reset-password" // POST

    // Excel import flow: analyze -> mapping -> validate -> preview -> execute
    const val IMPORT_ANALYZE = "businesses/{businessId}/imports/excel/analyze" // POST multipart
    const val IMPORT_MAPPING = "imports/{importId}/mapping" // GET suggestions, PATCH confirm
    const val IMPORT_VALIDATE = "imports/{importId}/validate" // POST
    const val IMPORT_VALIDATE_RESULTS = "imports/{importId}/validation-results" // GET
    const val IMPORT_PREVIEW = "imports/{importId}/preview" // GET
    const val IMPORT_EXECUTE = "imports/{importId}/execute" // POST
    const val IMPORT_STATUS = "imports/{importId}" // GET (job status)

    //Conversation
    const val CONVERSATION_LIST = "businesses/{businessId}/conversations" // GET
    const val CONVERSATION = "businesses/{businessId}/conversations/{conversationId}" //GET
    const val CONVERSATION_MESSAGES ="businesses/{businessId}/conversations/{conversationId}/messages" //GET
    const val CUSTOMER_LIST = "businesses/{businessId}/customers"
    const val CUSTOMER= "businesses/{businessId}/customers/{customerId}"
    const val MESSAGES= "businesses/{businessId}/conversations/{conversationId}/messages"

    // Working hours: GET returns [] until the first PUT; PUT replaces all seven days at once
    const val WORKING_HOURS = "businesses/{businessId}/working-hours" // GET, PUT
}

// Names used inside @Path / @Query / @Part annotations
object ApiParams {
    const val PATH_BUSINESS_ID = "businessId"
    const val PATH_IMPORT_ID = "importId"

    const val QUERY_BUSINESS_ID = "businessId"
    const val QUERY_LIMIT = "limit"
    const val QUERY_OFFSET = "offset"

    const val PART_FILE = "file" // required .xlsx
    const val PART_SHEET_NAME = "sheetName" // optional, falls back to the first sheet

    const val CONVERSATION_ID="conversationId"
    const val CUSTOMER_ID="customerId"
    const val QUERY_STATUS = "status"
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
    const val SKU = "sku"
    const val UNIT_PRICE = "unitPrice"
    const val NAME = "name" // required before confirming the mapping
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

object ConversationStatusValues{
    const val OPEN ="OPEN"
    const val CLOSED="CLOSED"
}

/** Values of `conversation.handoffReason` (why the AI stopped answering). */
object HandoffReasonValues {
    const val AI_REVIEW_REQUIRED = "AI_REVIEW_REQUIRED"
    const val AI_UNAVAILABLE = "AI_UNAVAILABLE"
    const val MESSAGE_TOO_LONG = "MESSAGE_TOO_LONG"
    /** The owner switched the AI off or replied herself. */
    const val OWNER_TOOK_OVER = "OWNER_TOOK_OVER"
}

// Stable conversation error codes from conversations-api.md.
object ConversationErrorCodes {
    /** 409 on POST .../messages: WhatsApp allows free-form replies only within 24 hours. */
    const val WHATSAPP_WINDOW_CLOSED = "WHATSAPP_WINDOW_CLOSED"
    const val CONVERSATION_NOT_FOUND = "CONVERSATION_NOT_FOUND"
    const val CUSTOMER_NOT_FOUND = "CUSTOMER_NOT_FOUND"
}

object MessageTypeValues{
    const val TEXT="TEXT"
    const val IMAGE="IMAGE"
    const val AUDIO="AUDIO"
    const val VIDEO="VIDEO"
    const val DOCUMENT="DOCUMENT"
    const val LOCATION ="LOCATION"
    const val OTHER="OTHER"
}
object MessageStatusValues{
    const val RECEIVED="RECEIVED"
    const val PENDING="PENDING"
    const val SENT="SENT"
    const val DELIVERED="DELIVERED"
    const val READ="READ"
    const val FAILED="FAILED"
}
object SenderTypeValues{
    const val AI="AI"
    const val OWNER="OWNER"
    const val CUSTOMER="CUSTOMER"
}
// Stable backend identifiers for client-side localization. Never show or parse "message".
object AuthErrorCodes {
    // 400 on POST /auth/register (and /auth/login): the backend rejected the field values.
    const val VALIDATION_ERROR = "VALIDATION_ERROR"
    // 409 on POST /auth/register: the address is already taken.
    const val EMAIL_UNAVAILABLE = "EMAIL_UNAVAILABLE"
    // 401 on POST /auth/login with wrong email/password.
    const val INVALID_CREDENTIALS = "INVALID_CREDENTIALS"
    // 401 on POST /auth/refresh: refresh token expired/used/revoked -> clear session.
    const val AUTH_REFRESH_INVALID = "AUTH_REFRESH_INVALID"
    // 429 on any auth route: surface the typed error, honoring Retry-After (numeric seconds).
    const val AUTH_RATE_LIMITED = "AUTH_RATE_LIMITED"
    // 503 on auth routes: same cooldown contract as the 429, keyed by this code.
    const val AUTH_RATE_LIMIT_UNAVAILABLE = "AUTH_RATE_LIMIT_UNAVAILABLE"
    // 503 on POST /auth/forgot-password: the backend has no mail provider configured.
    const val AUTH_EMAIL_UNAVAILABLE = "AUTH_EMAIL_UNAVAILABLE"
    // 400 on POST /auth/reset-password: the OTP is expired/used/superseded/invalid.
    const val AUTH_RESET_INVALID = "AUTH_RESET_INVALID"
}

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
