package com.yasminhosam.brandora.feature.business.data.remote

import com.yasminhosam.brandora.core.network.ApiEndpoints
import retrofit2.http.Body
import retrofit2.http.POST

/** Create-only: the app has no read or update need for businesses yet. */
internal interface BusinessApiService {

    @POST(ApiEndpoints.BUSINESSES)
    suspend fun createBusiness(@Body body: CreateBusinessRequestDto): BusinessProfileDto
}
