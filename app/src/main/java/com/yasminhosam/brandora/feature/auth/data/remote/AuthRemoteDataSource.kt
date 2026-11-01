package com.yasminhosam.brandora.feature.auth.data.remote

import com.yasminhosam.brandora.core.di.AuthRetrofit
import com.yasminhosam.brandora.core.network.ApiErrorHandler
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.core.network.map
import com.yasminhosam.brandora.feature.auth.data.mapper.toDomain
import com.yasminhosam.brandora.feature.auth.domain.model.AuthSession
import javax.inject.Inject
import retrofit2.Retrofit

/**
 * Uses the dedicated auth Retrofit instance (short timeouts): a hung refresh would otherwise
 * keep an OkHttp authenticator thread blocked for the upload-length timeouts.
 */
class AuthRemoteDataSource @Inject constructor(
    @AuthRetrofit retrofit: Retrofit,
    private val errorHandler: ApiErrorHandler
) {

    private val api: AuthApiService = retrofit.create(AuthApiService::class.java)

    suspend fun register(email: String, password: String): ApiResult<AuthSession> =
        errorHandler.safeApiCall {
            api.register(CredentialsRequestDto(email = email, password = password))
        }.map { it.toDomain() }

    suspend fun login(email: String, password: String): ApiResult<AuthSession> =
        errorHandler.safeApiCall { api.login(CredentialsRequestDto(email = email, password = password)) }
            .map { it.toDomain() }

    suspend fun refresh(refreshToken: String): ApiResult<AuthSession> =
        errorHandler.safeApiCall { api.refresh(RefreshRequestDto(refreshToken = refreshToken)) }
            .map { it.toDomain() }
}
