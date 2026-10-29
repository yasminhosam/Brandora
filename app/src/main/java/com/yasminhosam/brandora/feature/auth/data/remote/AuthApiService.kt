package com.yasminhosam.brandora.feature.auth.data.remote

import com.yasminhosam.brandora.core.network.ApiEndpoints
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Internal: built with retrofit.create() inside the data source, because an internal type in
 * a Hilt provider signature breaks Dagger's generated callers through Kotlin name mangling.
 *
 * Only login and refresh exist for now; register/forgot/reset/logout arrive with their
 * screens. All routes here are public: they use the short-timeout auth Retrofit instance and
 * never carry an Authorization header.
 */
internal interface AuthApiService {

    @POST(ApiEndpoints.AUTH_LOGIN)
    suspend fun login(@Body body: LoginRequestDto): SessionDto

    @POST(ApiEndpoints.AUTH_REFRESH)
    suspend fun refresh(@Body body: RefreshRequestDto): SessionDto
}
