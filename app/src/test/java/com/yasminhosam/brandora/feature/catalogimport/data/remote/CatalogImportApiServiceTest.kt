package com.yasminhosam.brandora.feature.catalogimport.data.remote

import com.yasminhosam.brandora.core.network.ApiEndpoints
import com.yasminhosam.brandora.core.network.ApiParams
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ExcelFile
import java.lang.reflect.Method
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

/** Contract assertions via reflection, so no MockWebServer is needed for these. */
class CatalogImportApiServiceTest {

    // Keep in sync with NetworkModule.provideJson().
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    private fun retrofit(): Retrofit = Retrofit.Builder()
        .baseUrl("http://localhost:3000/")
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private fun analyzeMethod(): Method =
        CatalogImportApiService::class.java.declaredMethods.single { it.name == "analyze" }

    @Test
    fun `retrofit accepts the service`() {
        assertNotNull(retrofit().create(CatalogImportApiService::class.java))
    }

    @Test
    fun `posts to the analyze route`() {
        val method = analyzeMethod()
        val post = method.getAnnotation(POST::class.java)

        assertNotNull("analyze must carry @POST", post)
        assertEquals(ApiEndpoints.IMPORT_ANALYZE, post?.value)
        assertTrue(method.isAnnotationPresent(Multipart::class.java))
    }

    @Test
    fun `businessId is a path parameter using the shared constant`() {
        val path = analyzeMethod().parameterAnnotations[0].filterIsInstance<Path>().single()

        assertEquals(ApiParams.PATH_BUSINESS_ID, path.value)
        assertEquals(String::class.java, analyzeMethod().parameterTypes[0])
    }

    @Test
    fun `the workbook part carries no name in the annotation`() {
        val part = analyzeMethod().parameterAnnotations[1].filterIsInstance<Part>()

        assertTrue(part.isNotEmpty())
        assertTrue("expected an empty @Part value", part.single().value.isEmpty())
        assertEquals(MultipartBody.Part::class.java, analyzeMethod().parameterTypes[1])
    }

    @Test
    fun `sheetName is an optional part using the shared constant`() {
        val part = analyzeMethod().parameterAnnotations[2].filterIsInstance<Part>().single()

        assertEquals(ApiParams.PART_SHEET_NAME, part.value)
    }

    @Test
    fun `the multipart form key is exactly what the backend expects`() {
        assertEquals("file", ApiParams.PART_FILE)

        val part = buildFilePart(
            ExcelFile("Book.xlsx", 4L) { "test".byteInputStream() }
        )
        val disposition = part.headers.toString()

        assertTrue("missing name=file in $disposition", disposition.contains("name=\"file\""))
        assertTrue("missing filename in $disposition", disposition.contains("filename=\"Book.xlsx\""))
        assertEquals(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            part.body.contentType().toString()
        )
    }
}
