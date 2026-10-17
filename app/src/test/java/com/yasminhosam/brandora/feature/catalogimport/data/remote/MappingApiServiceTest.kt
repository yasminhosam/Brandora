package com.yasminhosam.brandora.feature.catalogimport.data.remote

import com.yasminhosam.brandora.core.network.ApiEndpoints
import com.yasminhosam.brandora.core.network.ApiParams
import java.lang.reflect.Method
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

class MappingApiServiceTest {

    private fun getMethod(): Method =
        CatalogImportApiService::class.java.declaredMethods.single { it.name == "getMapping" }

    private fun patchMethod(): Method =
        CatalogImportApiService::class.java.declaredMethods.single { it.name == "confirmMapping" }

    @Test
    fun `get mapping uses the shared route and importId path`() {
        val method = getMethod()
        val get = method.getAnnotation(GET::class.java)

        assertNotNull("getMapping must carry @GET", get)
        assertEquals(ApiEndpoints.IMPORT_MAPPING, get?.value)
        val path = method.parameterAnnotations[0].filterIsInstance<Path>().single()
        assertEquals(ApiParams.PATH_IMPORT_ID, path.value)
        assertEquals(String::class.java, method.parameterTypes[0])
    }

    @Test
    fun `confirm mapping patches the same route with a body`() {
        val method = patchMethod()
        val patch = method.getAnnotation(PATCH::class.java)

        assertNotNull("confirmMapping must carry @PATCH", patch)
        assertEquals(ApiEndpoints.IMPORT_MAPPING, patch?.value)
        val path = method.parameterAnnotations[0].filterIsInstance<Path>().single()
        assertEquals(ApiParams.PATH_IMPORT_ID, path.value)
        val body = method.parameterAnnotations[1].filterIsInstance<Body>()
        assertTrue(body.isNotEmpty())
        assertEquals(ConfirmMappingRequestDto::class.java, method.parameterTypes[1])
    }
}
