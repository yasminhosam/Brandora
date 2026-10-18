package com.yasminhosam.brandora.feature.catalogimport.data.remote

import com.yasminhosam.brandora.core.network.ApiEndpoints
import com.yasminhosam.brandora.core.network.ApiParams
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Internal: built with retrofit.create() inside the data source, because an internal type in a
 * Hilt provider signature breaks Dagger's generated callers through Kotlin name mangling.
 */
internal interface CatalogImportApiService {

    /**
     * The form key comes from createFormData(), NOT from @Part: Retrofit ignores the
     * annotation's value on a MultipartBody.Part parameter. A null sheetName part is omitted,
     * so the backend falls back to the first sheet.
     */
    @Multipart
    @POST(ApiEndpoints.IMPORT_ANALYZE)
    suspend fun analyze(
        @Path(ApiParams.PATH_BUSINESS_ID) businessId: String,
        @Part file: MultipartBody.Part,
        @Part(ApiParams.PART_SHEET_NAME) sheetName: RequestBody? = null
    ): AnalyzeExcelResponseDto

    @GET(ApiEndpoints.IMPORT_MAPPING)
    suspend fun getMapping(
        @Path(ApiParams.PATH_IMPORT_ID) importId: String
    ): MappingResponseDto

    @PATCH(ApiEndpoints.IMPORT_MAPPING)
    suspend fun confirmMapping(
        @Path(ApiParams.PATH_IMPORT_ID) importId: String,
        @Body body: ConfirmMappingRequestDto
    ): ConfirmMappingResponseDto

    @POST(ApiEndpoints.IMPORT_VALIDATE)
    suspend fun validate(
        @Path(ApiParams.PATH_IMPORT_ID) importId: String
    ): ValidateResponseDto

    @GET(ApiEndpoints.IMPORT_VALIDATE_RESULTS)
    suspend fun getValidationResults(
        @Path(ApiParams.PATH_IMPORT_ID) importId: String,
        @Query(ApiParams.QUERY_LIMIT) limit: Int,
        @Query(ApiParams.QUERY_OFFSET) offset: Int
    ): ValidationResultsResponseDto

    @GET(ApiEndpoints.IMPORT_PREVIEW)
    suspend fun getPreview(
        @Path(ApiParams.PATH_IMPORT_ID) importId: String,
        @Query(ApiParams.QUERY_LIMIT) limit: Int,
        @Query(ApiParams.QUERY_OFFSET) offset: Int
    ): PreviewResponseDto

    @POST(ApiEndpoints.IMPORT_EXECUTE)
    suspend fun execute(
        @Path(ApiParams.PATH_IMPORT_ID) importId: String
    ): ExecuteResponseDto

    @GET(ApiEndpoints.IMPORT_STATUS)
    suspend fun getImportStatus(
        @Path(ApiParams.PATH_IMPORT_ID) importId: String
    ): JobStatusDto
}
