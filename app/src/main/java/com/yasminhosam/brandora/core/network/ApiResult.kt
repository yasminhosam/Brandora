package com.yasminhosam.brandora.core.network

import kotlinx.coroutines.CancellationException
sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Error(
        val type: ApiErrorType,
        val code: Int? = null,
        val message: String?=null
    ) : ApiResult<Nothing>
}
enum class ApiErrorType {
    NETWORK, TIMEOUT, UNAUTHORIZED, BAD_REQUEST, NOT_FOUND,
    CONFLICT, FILE_TOO_LARGE, SERVER, UNKNOWN
}

inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(data))
    is ApiResult.Error -> this
}