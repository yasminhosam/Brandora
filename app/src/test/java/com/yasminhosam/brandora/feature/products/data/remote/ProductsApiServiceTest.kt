package com.yasminhosam.brandora.feature.products.data.remote

import com.yasminhosam.brandora.core.network.*
import com.yasminhosam.brandora.feature.products.data.repository.ProductsRepositoryImpl
import com.yasminhosam.brandora.feature.products.domain.model.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class ProductsApiServiceTest {
    private val requests = mutableListOf<Request>()
    private val bodies = mutableListOf<String>()

    // Keep in sync with NetworkModule.provideJson().
    private val responseJson =
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            explicitNulls = false
        }

    private fun retrofit(response: String = "{}", status: Int = 200): Retrofit {
        val client =
            OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request()
                    requests += request
                    val buffer = Buffer()
                    request.body?.writeTo(buffer)
                    bodies += buffer.readUtf8()
                    Response.Builder()
                        .request(request)
                        .protocol(Protocol.HTTP_1_1)
                        .code(status)
                        .message("OK")
                        .body(response.toResponseBody("application/json".toMediaType()))
                        .build()
                }
                .build()
        return Retrofit.Builder()
            .baseUrl("https://api.test/")
            .client(client)
            .addConverterFactory(responseJson.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    private fun repository(response: String = "{}", status: Int = 200) =
        ProductsRepositoryImpl(retrofit(response, status), ApiErrorHandler(Json))

    private fun api(response: String = "{}", status: Int = 200): ProductsApiService =
        retrofit(response, status).create(ProductsApiService::class.java)

    // ---- repository level: bodies built by the typed data layer ----

    @Test
    fun `adjust without quantity disables tracking with an explicit null`() = runBlocking {
        val result = repository().adjust("business-a", "prod", "var", null)
        assertTrue(result is ApiResult.Success)
        val request = requests.single()
        assertEquals(
            "/businesses/business-a/products/prod/variants/var/inventory/adjust",
            request.url.encodedPath,
        )
        val sent = Json.parseToJsonElement(bodies.single()).jsonObject
        assertEquals(JsonNull, sent["quantity"])
        assertEquals(false, sent["trackInventory"]?.jsonPrimitive?.boolean)
    }

    @Test
    fun `adjust with quantity tracks inventory`() = runBlocking {
        repository().adjust("business-a", "prod", "var", 0)
        val sent = Json.parseToJsonElement(bodies.single()).jsonObject
        assertEquals(0, sent["quantity"]?.jsonPrimitive?.int)
        assertEquals(true, sent["trackInventory"]?.jsonPrimitive?.boolean)
    }

    @Test
    fun `upload sends image bytes as files parts`() = runBlocking {
        val files = listOf(ImageUpload(byteArrayOf(1, 2, 3), "image/jpeg"))
        val result = repository().upload("business-a", "prod", files)
        assertTrue(result is ApiResult.Success)
        val request = requests.single()
        assertEquals("/businesses/business-a/products/prod/images", request.url.encodedPath)
        assertTrue(request.body!!.contentType()!!.toString().startsWith("multipart/form-data"))
        assertTrue(bodies.single().contains("name=\"files\""))
        assertTrue(bodies.single().contains("filename=\"image-0.jpg\""))
    }

    @Test
    fun `image reorder sends imageIds in the displayed order`() = runBlocking {
        repository().order("business-a", "prod", listOf("i2", "i1"))
        assertEquals("/businesses/business-a/products/prod/images", requests.single().url.encodedPath)
        val sent = Json.parseToJsonElement(bodies.single()).jsonObject
        assertEquals(setOf("imageIds"), sent.keys)
        assertEquals(
            listOf("i2", "i1"),
            sent["imageIds"]?.jsonArray?.map { it.jsonPrimitive.content },
        )
    }

    @Test
    fun `deleteImage reports cleanupPending`() = runBlocking {
        val result =
            repository("""{"deleted":true,"cleanupPending":true}""")
                .deleteImage("business-a", "prod", "img")
        assertTrue(result is ApiResult.Success)
        assertTrue((result as ApiResult.Success).data.cleanupPending)
        assertTrue(result.data.deleted)
    }

    @Test
    fun `main image body is exactly isMain true`() = runBlocking {
        repository().main("business-a", "prod", "img")
        assertEquals(
            "/businesses/business-a/products/prod/images/img",
            requests.single().url.encodedPath,
        )
        assertEquals("""{"isMain":true}""", bodies.single())
    }

    // ---- service level: routes, query params, exact payloads ----

    @Test
    fun `manual create uses the canonical route and sends the full typed body`() = runBlocking {
        val result =
            api("""{"id":"p1","name":"Manual"}""", 201)
                .create(
                    "business-a",
                    writeBody(
                        ProductWriteDto(
                            name = "Manual",
                            externalKey = "ع-001",
                            description = null,
                            basePrice = "0.00",
                            categoryId = null,
                            options = emptyMap(),
                        )
                    ),
                )
        assertEquals("p1", result.id)
        val request = requests.single()
        assertEquals("POST", request.method)
        assertEquals("/businesses/business-a/products", request.url.encodedPath)
        val sent = Json.parseToJsonElement(bodies.single()).jsonObject
        assertEquals("Manual", sent["name"]?.jsonPrimitive?.content)
        assertEquals("ع-001", sent["externalKey"]?.jsonPrimitive?.content)
        assertEquals(JsonNull, sent["description"])
        assertEquals(JsonNull, sent["categoryId"])
        assertEquals(JsonObject(emptyMap()), sent["options"])
    }

    @Test
    fun `explicit null survives serialization to clear nullable fields`() = runBlocking {
        api("""{"id":"p1","name":"Manual"}""")
            .update(
                "business-a",
                "p1",
                writeBody(
                    ProductWriteDto(
                        name = "Manual",
                        externalKey = null,
                        description = null,
                        basePrice = null,
                        categoryId = null,
                        options = emptyMap(),
                    )
                ),
            )
        val request = requests.single()
        assertEquals("PATCH", request.method)
        assertEquals("/businesses/business-a/products/p1", request.url.encodedPath)
        val sent = Json.parseToJsonElement(bodies.single()).jsonObject
        assertEquals(JsonNull, sent["externalKey"])
        assertEquals(JsonNull, sent["basePrice"])
        assertEquals(JsonNull, sent["categoryId"])
    }

    @Test
    fun `reactivate sends only isActive on the product route`() = runBlocking {
        api("""{"id":"p1","name":"Product"}""")
            .updateActive("business-a", "p1", writeBody(ActiveWriteDto(true)))
        val request = requests.single()
        assertEquals("PATCH", request.method)
        assertEquals("/businesses/business-a/products/p1", request.url.encodedPath)
        assertEquals(setOf("isActive"), Json.parseToJsonElement(bodies.single()).jsonObject.keys)
    }

    @Test
    fun `variant state patch sends only isActive on the variant route`() = runBlocking {
        repository("""{"id":"v1","productId":"p1"}""")
            .setVariantActive("business-a", "p1", "v1", active = false)
        val request = requests.single()
        assertEquals("PATCH", request.method)
        assertEquals("/businesses/business-a/products/p1/variants/v1", request.url.encodedPath)
        assertEquals(setOf("isActive"), Json.parseToJsonElement(bodies.single()).jsonObject.keys)
    }

    @Test
    fun `variant create carries only sku, optionValues and unitPrice`() = runBlocking {
        api("""{"id":"v1"}""", 201)
            .createVariant(
                "business-a",
                "prod",
                writeBody(
                    VariantCreateDto(
                        sku = null,
                        optionValues = mapOf("الخامة" to "قطن"),
                        unitPrice = null,
                    )
                ),
            )
        val request = requests.single()
        assertEquals("/businesses/business-a/products/prod/variants", request.url.encodedPath)
        val sent = Json.parseToJsonElement(bodies.single()).jsonObject
        assertEquals(setOf("sku", "optionValues", "unitPrice"), sent.keys)
        assertFalse(sent.containsKey("isActive"))
        assertFalse(sent.containsKey("stockQuantity"))
        assertEquals(JsonNull, sent["sku"])
        assertEquals(JsonObject(mapOf("الخامة" to JsonPrimitive("قطن"))), sent["optionValues"])
    }

    @Test
    fun `category rename and delete use the item routes with a name only body`() = runBlocking {
        api("""{"id":"c1","name":"General Goods"}""")
            .renameCategory("business-a", "c1", writeBody(CategoryWriteDto("General Goods")))
        var request = requests.last()
        assertEquals("PATCH", request.method)
        assertEquals("/businesses/business-a/categories/c1", request.url.encodedPath)
        assertEquals(setOf("name"), Json.parseToJsonElement(bodies.last()).jsonObject.keys)

        api("", 204).deleteCategory("business-a", "c1")
        request = requests.last()
        assertEquals("DELETE", request.method)
        assertEquals("/businesses/business-a/categories/c1", request.url.encodedPath)
    }

    @Test
    fun `list pages by offset and limit`() = runBlocking {
        api().products("business-a", limit = 20, offset = 40)
        val url = requests.single().url
        assertEquals("/businesses/business-a/products", url.encodedPath)
        assertEquals("20", url.queryParameter("limit"))
        assertEquals("40", url.queryParameter("offset"))
    }

    @Test
    fun `categoryId query param is emitted only when server side filtering is used`() =
        runBlocking {
            val service = api("""{"items":[],"pagination":{}}""")
            service.products("business-a", offset = 0, categoryId = null)
            assertNull(requests.last().url.queryParameter("categoryId"))

            service.products("business-a", offset = 0, categoryId = "cat-9")
            assertEquals("cat-9", requests.last().url.queryParameter("categoryId"))
        }

    @Test
    fun `movements stay scoped to the variant and page by offset`() = runBlocking {
        api("""{"items":[],"pagination":{}}""")
            .movements("business-a", "prod", "var", offset = 20)
        val url = requests.single().url
        assertEquals(
            "/businesses/business-a/products/prod/variants/var/inventory/movements",
            url.encodedPath,
        )
        assertEquals("20", url.queryParameter("offset"))
    }
}
