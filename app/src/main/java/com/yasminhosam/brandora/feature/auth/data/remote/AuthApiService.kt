package com.yasminhosam.brandora.feature.auth.data.remote

import com.yasminhosam.brandora.core.network.ApiEndpoints
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Internal: built with retrofit.create() inside the data source, because an internal type in
 * a Hilt provider signature breaks Dagger's generated callers through Kotlin name mangling.
 *
 * All auth routes here are public: they use the short-timeout auth Retrofit instance and
 * never carry an Authorization header.
 *
 * forgot (200 with a generic message we deliberately ignore) and reset (204 No Content)
 * return Unit: Retrofit's built-in Unit converter closes a 200 body without parsing it, and
 * the suspend Unit path accepts the null body of a 204. Backend message text is never shown.
 */
internal interface AuthApiService {

    @POST(ApiEndpoints.AUTH_REGISTER)
    suspend fun register(@Body body: CredentialsRequestDto): SessionDto

    @POST(ApiEndpoints.AUTH_LOGIN)
    suspend fun login(@Body body: CredentialsRequestDto): SessionDto

    @POST(ApiEndpoints.AUTH_REFRESH)
    suspend fun refresh(@Body body: RefreshRequestDto): SessionDto

    @POST(ApiEndpoints.AUTH_FORGOT_PASSWORD)
    suspend fun forgotPassword(@Body body: ForgotPasswordRequestDto)

    @POST(ApiEndpoints.AUTH_RESET_PASSWORD)
    suspend fun resetPassword(@Body body: ResetPasswordRequestDto)
}
