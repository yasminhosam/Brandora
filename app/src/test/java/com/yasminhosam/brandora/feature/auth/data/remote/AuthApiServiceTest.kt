package com.yasminhosam.brandora.feature.auth.data.remote

import com.yasminhosam.brandora.core.network.ApiEndpoints
import java.lang.reflect.Method
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

/** Contract assertions via reflection, mirroring CatalogImportApiServiceTest. */
class AuthApiServiceTest {

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

    private fun method(name: String): Method =
        AuthApiService::class.java.declaredMethods.single { it.name == name }

    @Test
    fun `retrofit accepts the service`() {
        assertNotNull(retrofit().create(AuthApiService::class.java))
    }

    @Test
    fun `login posts to the auth login route`() {
        val post = method("login").getAnnotation(POST::class.java)

        assertNotNull("login must carry @POST", post)
        assertEquals(ApiEndpoints.AUTH_LOGIN, post?.value)
    }

    @Test
    fun `register posts to the auth register route`() {
        val post = method("register").getAnnotation(POST::class.java)

        assertNotNull("register must carry @POST", post)
        assertEquals(ApiEndpoints.AUTH_REGISTER, post?.value)
    }

    @Test
    fun `refresh posts to the auth refresh route`() {
        val post = method("refresh").getAnnotation(POST::class.java)

        assertNotNull("refresh must carry @POST", post)
        assertEquals(ApiEndpoints.AUTH_REFRESH, post?.value)
    }

    @Test
    fun `forgot posts to the forgot-password route`() {
        val post = method("forgotPassword").getAnnotation(POST::class.java)

        assertNotNull("forgotPassword must carry @POST", post)
        assertEquals(ApiEndpoints.AUTH_FORGOT_PASSWORD, post?.value)
    }

    @Test
    fun `reset posts to the reset-password route`() {
        val post = method("resetPassword").getAnnotation(POST::class.java)

        assertNotNull("resetPassword must carry @POST", post)
        assertEquals(ApiEndpoints.AUTH_RESET_PASSWORD, post?.value)
    }

    @Test
    fun `all five methods send a JSON body`() {
        // Index 0, not single(): a suspend function's hidden Continuation parameter adds a
        // second entry to parameterAnnotations/parameterTypes.
        val registerBody = method("register").parameterAnnotations[0].filterIsInstance<Body>().single()
        val loginBody = method("login").parameterAnnotations[0].filterIsInstance<Body>().single()
        val refreshBody = method("refresh").parameterAnnotations[0].filterIsInstance<Body>().single()
        val forgotBody = method("forgotPassword").parameterAnnotations[0].filterIsInstance<Body>().single()
        val resetBody = method("resetPassword").parameterAnnotations[0].filterIsInstance<Body>().single()

        assertEquals(CredentialsRequestDto::class.java, method("register").parameterTypes[0])
        assertEquals(CredentialsRequestDto::class.java, method("login").parameterTypes[0])
        assertEquals(RefreshRequestDto::class.java, method("refresh").parameterTypes[0])
        assertEquals(ForgotPasswordRequestDto::class.java, method("forgotPassword").parameterTypes[0])
        assertEquals(ResetPasswordRequestDto::class.java, method("resetPassword").parameterTypes[0])
        assertNotNull(registerBody)
        assertNotNull(loginBody)
        assertNotNull(refreshBody)
        assertNotNull(forgotBody)
        assertNotNull(resetBody)
    }

    @Test
    fun `the public routes cover the whole contract (for the interceptor skip list)`() {
        assertEquals(
            setOf(
                ApiEndpoints.AUTH_REGISTER,
                ApiEndpoints.AUTH_LOGIN,
                ApiEndpoints.AUTH_REFRESH,
                ApiEndpoints.AUTH_LOGOUT,
                ApiEndpoints.AUTH_FORGOT_PASSWORD,
                ApiEndpoints.AUTH_RESET_PASSWORD
            ),
            com.yasminhosam.brandora.core.network.PUBLIC_AUTH_PATHS
        )
    }
}
