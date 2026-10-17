package com.yasminhosam.brandora.feature.catalogimport.data.remote

import com.yasminhosam.brandora.core.network.ApiErrorHandler
import com.yasminhosam.brandora.core.network.ApiParams
import com.yasminhosam.brandora.core.network.ApiResult
import com.yasminhosam.brandora.core.network.map
import com.yasminhosam.brandora.feature.catalogimport.data.file.UriRequestBody
import com.yasminhosam.brandora.feature.catalogimport.data.mapper.toDomain
import com.yasminhosam.brandora.feature.catalogimport.data.mapper.toRequestDto
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.MappedColumn
import com.yasminhosam.brandora.feature.catalogimport.domain.mapping.MappingPayload
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ExcelFile
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ImportAnalysis
import okhttp3.MultipartBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import javax.inject.Inject

class CatalogImportRemoteDataSource @Inject constructor(
    retrofit: Retrofit,
    private val errorHandler: ApiErrorHandler
) {

    private val api: CatalogImportApiService = retrofit.create(CatalogImportApiService::class.java)

    suspend fun analyze(
        file: ExcelFile,
        businessId: String,
        sheetName: String?
    ): ApiResult<ImportAnalysis> =
        errorHandler.safeApiCall {
            api.analyze(
                businessId = businessId,
                file = buildFilePart(file),
                sheetName = sheetName?.toRequestBody(TEXT_PLAIN_MEDIA_TYPE)
            )
        }.map { it.toDomain() }

    suspend fun getMapping(importId: String): ApiResult<MappingPayload> =
        errorHandler.safeApiCall { api.getMapping(importId) }.map { it.toDomain() }

    suspend fun confirmMapping(importId: String, columns: List<MappedColumn>): ApiResult<Unit> =
        errorHandler.safeApiCall { api.confirmMapping(importId, columns.toRequestDto()) }.map { }
}

/**
 * The form key comes from createFormData(), NOT from the @Part annotation (see service).
 * The backend expects exactly "file".
 */
internal fun buildFilePart(file: ExcelFile): MultipartBody.Part =
    MultipartBody.Part.createFormData(
        ApiParams.PART_FILE,
        file.displayName,
        UriRequestBody(file)
    )

private val TEXT_PLAIN_MEDIA_TYPE = "text/plain".toMediaType()
