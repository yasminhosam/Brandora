package com.yasminhosam.brandora.core.network
sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Error(
        val type: ApiErrorType,
        val code: Int? = null, // HTTP status
        val message: String?=null,
        val errorCode: String? = null // backend's stable code, e.g. IMPORT_MAPPING_CHANGED
    ) : ApiResult<Nothing>
}
enum class ApiErrorType {
    NETWORK, TIMEOUT, UNAUTHORIZED, BAD_REQUEST, NOT_FOUND,
    CONFLICT, FILE_TOO_LARGE, INVALID_FILE_TYPE, SERVER, UNKNOWN
}

inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(data))
    is ApiResult.Error -> this
}