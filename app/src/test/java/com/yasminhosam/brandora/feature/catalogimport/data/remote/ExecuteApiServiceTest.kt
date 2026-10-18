package com.yasminhosam.brandora.feature.catalogimport.data.remote

import com.yasminhosam.brandora.core.network.ApiEndpoints
import com.yasminhosam.brandora.core.network.ApiParams
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.lang.reflect.Method

class ExecuteApiServiceTest {

    private fun methodNamed(name: String): Method =
        CatalogImportApiService::class.java.declaredMethods.single { it.name == name }

    @Test
    fun `execute posts to the execute route with an importId path`() {
        val method = methodNamed("execute")
        val post = method.getAnnotation(POST::class.java)

        assertNotNull("execute must carry @POST", post)
        assertEquals(ApiEndpoints.IMPORT_EXECUTE, post?.value)
        val path = method.parameterAnnotations[0].filterIsInstance<Path>().single()
        assertEquals(ApiParams.PATH_IMPORT_ID, path.value)
    }

    @Test
    fun `getImportStatus gets the job route with an importId path`() {
        val method = methodNamed("getImportStatus")
        val get = method.getAnnotation(GET::class.java)

        assertNotNull("getImportStatus must carry @GET", get)
        assertEquals(ApiEndpoints.IMPORT_STATUS, get?.value)
        val path = method.parameterAnnotations[0].filterIsInstance<Path>().single()
        assertEquals(ApiParams.PATH_IMPORT_ID, path.value)
    }

    @Test
    fun `preview gets the preview route with limit and offset`() {
        val method = methodNamed("getPreview")
        val get = method.getAnnotation(GET::class.java)

        assertNotNull("getPreview must carry @GET", get)
        assertEquals(ApiEndpoints.IMPORT_PREVIEW, get?.value)
    }
}
