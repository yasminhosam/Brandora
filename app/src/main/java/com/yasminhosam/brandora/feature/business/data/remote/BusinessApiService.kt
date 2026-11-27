package com.yasminhosam.brandora.feature.business.data.remote

import com.yasminhosam.brandora.core.network.ApiEndpoints
import com.yasminhosam.brandora.core.network.ApiParams
import com.yasminhosam.brandora.feature.products.data.remote.PageDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/** Business contract: create, list owned businesses. */
internal interface BusinessApiService {

    @POST(ApiEndpoints.BUSINESSES)
    suspend fun createBusiness(@Body body: CreateBusinessRequestDto): BusinessProfileDto

    @GET(ApiEndpoints.BUSINESSES)
    suspend fun getBusinesses(
        @Query(ApiParams.QUERY_LIMIT) limit: Int = 20,
        @Query(ApiParams.QUERY_OFFSET) offset: Int = 0,
    ): PageDto<BusinessProfileDto>
}
