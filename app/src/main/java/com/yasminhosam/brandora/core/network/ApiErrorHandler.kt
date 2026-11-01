package com.yasminhosam.brandora.core.network

import android.util.Log
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class ApiErrorHandler @Inject constructor(
    private val json: Json
) {
    // Wraps any Retrofit call so the ViewModel never sees raw exceptions
    suspend fun <T> safeApiCall(block: suspend () -> T): ApiResult<T> =
        try {
            ApiResult.Success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handle(e)
        }

    fun handle(error: Throwable): ApiResult.Error {

        return when (error) {
            is HttpException ->{
                val statusCode =error.code()
                val apiError= parseErrorBody(
                    error.response()?.errorBody()?.string()
                )
                // The backend's stable code outranks the status: a 503 carrying
                // AUTH_RATE_LIMIT_UNAVAILABLE gets the cooldown contract, while any
                // other 503 stays the generic server error.
                val type = if (apiError?.code == AuthErrorCodes.AUTH_RATE_LIMIT_UNAVAILABLE) {
                    ApiErrorType.AUTH_RATE_LIMIT_UNAVAILABLE
                } else {
                    typeFor(statusCode)
                }
                ApiResult.Error(
                    type = type,
                    code = statusCode,
                    message = apiError?.readableMessage(),
                    errorCode = apiError?.code,
                    // Retry-After is only actionable for the rate-limit contract: the 429
                    // and the 503 with its stable code. Any other status sending the header
                    // must not make the field look actionable.
                    retryAfterSeconds = if (
                        statusCode == 429 || type == ApiErrorType.AUTH_RATE_LIMIT_UNAVAILABLE
                    ) retryAfterSeconds(error) else null
                )

            }

            is SocketTimeoutException -> ApiResult.Error(type = ApiErrorType.TIMEOUT)

            is IOException -> ApiResult.Error(type = ApiErrorType.NETWORK)
            is SerializationException -> {
                Log.e("ApiErrorHandler", "Unexpected response shape", error)
                ApiResult.Error(type = ApiErrorType.UNKNOWN)
            }
            else -> ApiResult.Error(type = ApiErrorType.UNKNOWN, message = error.message)

        }
    }
    private fun parseErrorBody(errorBody: String?): ApiErrorModel? {
        if (errorBody.isNullOrBlank()) return null
        return try {
            json.decodeFromString<ApiErrorModel>(errorBody)   // injected Json, not the default one
        } catch (e: SerializationException) {
            null
        }
    }

    private fun typeFor(code: Int): ApiErrorType = when (code) {
        400 -> ApiErrorType.BAD_REQUEST
        401 -> ApiErrorType.UNAUTHORIZED
        404 -> ApiErrorType.NOT_FOUND
        409 -> ApiErrorType.CONFLICT
        413 -> ApiErrorType.FILE_TOO_LARGE
        429 -> ApiErrorType.AUTH_RATE_LIMITED
        in 500..599 -> ApiErrorType.SERVER
        else -> ApiErrorType.UNKNOWN
    }

    /**
     * The backend sends Retry-After as numeric seconds. An HTTP-date or an unparseable
     * value is treated as absent - the caller shows a generic rate-limit message.
     */
    private fun retryAfterSeconds(error: HttpException): Long? =
        error.response()?.headers()?.get("Retry-After")?.trim()?.toLongOrNull()
}