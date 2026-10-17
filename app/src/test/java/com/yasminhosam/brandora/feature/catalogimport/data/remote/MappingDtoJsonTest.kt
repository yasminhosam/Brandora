package com.yasminhosam.brandora.feature.catalogimport.data.remote

import com.yasminhosam.brandora.core.network.ApiErrorHandler
import com.yasminhosam.brandora.core.network.ApiErrorType
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class MappingDtoJsonTest {

    // Keep in sync with NetworkModule.provideJson().
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Test
    fun `get response with null mapping parses, keeping mixed sample types`() {
        val parsed = json.decodeFromString<MappingResponseDto>(GET_SUGGESTIONS)

        assertEquals("job-1", parsed.importId)
        assertEquals("MAPPING_REQUIRED", parsed.status)
        assertNull(parsed.mapping)
        assertEquals(2, parsed.columns.size)

        val first = parsed.columns[0]
        assertEquals("Item Name", first.sourceColumn)
        assertEquals(1, first.sourceColumnIndex)
        assertEquals("STANDARD", first.suggestedType)
        assertEquals("name", first.suggestedTarget)
        assertEquals(3, first.samples.size)

        val blank = parsed.columns[1]
        assertEquals("", blank.sourceColumn)
        assertEquals("UNMAPPED", blank.suggestedType)
        assertNull(blank.suggestedTarget)
    }

    @Test
    fun `get response with a saved mapping parses every entry`() {
        val parsed = json.decodeFromString<MappingResponseDto>(GET_SAVED)

        val mapping = requireNotNull(parsed.mapping)
        assertEquals(3, mapping.size)
        assertEquals("STANDARD", mapping[0].type)
        assertEquals("name", mapping[0].target)
        assertEquals("VARIANT_ATTRIBUTE", mapping[1].type)
        assertEquals("SOMETHING_NEW", mapping[2].type)
    }

    @Test
    fun `patch body serializes indices and drops null targets`() {
        val body = ConfirmMappingRequestDto(
            mapping = listOf(
                MappingEntryDto("Item Name", 1, "STANDARD", "name"),
                MappingEntryDto("Fabric", 2, "ATTRIBUTE", "fabric"),
                MappingEntryDto("Notes", 3, "IGNORE", null)
            )
        )

        val serialized = json.encodeToString(body)

        assertTrue(serialized.contains("\"sourceColumnIndex\":1"))
        assertTrue(serialized.contains("\"sourceColumnIndex\":2"))
        assertTrue(serialized.contains("\"target\":\"name\""))
        assertTrue(!serialized.contains("\"target\":null"))
    }

    @Test
    fun `error envelope with string message yields its code`() {
        val error = handlerError(
            400,
            """{"statusCode":400,"code":"MAPPING_NAME_REQUIRED","message":"name is required","error":"Bad Request"}"""
        )

        assertEquals(ApiErrorType.BAD_REQUEST, error.type)
        assertEquals("MAPPING_NAME_REQUIRED", error.errorCode)
    }

    @Test
    fun `error envelope with array message still yields its code`() {
        val error = handlerError(
            400,
            """{"statusCode":400,"code":"VALIDATION_ERROR","message":["mapping must be an array"],"error":"Bad Request"}"""
        )

        assertEquals(ApiErrorType.BAD_REQUEST, error.type)
        assertEquals("VALIDATION_ERROR", error.errorCode)
    }

    @Test
    fun `409 envelope yields conflict type with its code`() {
        val error = handlerError(
            409,
            """{"statusCode":409,"code":"IMPORT_NOT_AWAITING_MAPPING","message":"stale","error":"Conflict"}"""
        )

        assertEquals(ApiErrorType.CONFLICT, error.type)
        assertEquals("IMPORT_NOT_AWAITING_MAPPING", error.errorCode)
    }

    private fun handlerError(status: Int, body: String) =
        ApiErrorHandler(json).handle(
            HttpException(Response.error<Any>(status, body.toResponseBody(JSON_MEDIA_TYPE)))
        )

    private companion object {
        val JSON_MEDIA_TYPE = "application/json".toMediaType()

        val GET_SUGGESTIONS = """
            {
              "importId": "job-1",
              "status": "MAPPING_REQUIRED",
              "columns": [
                {
                  "sourceColumn": "Item Name",
                  "sourceColumnIndex": 1,
                  "samples": ["Shirt", 10, true],
                  "suggestedType": "STANDARD",
                  "suggestedTarget": "name",
                  "displayLabel": "Product Name"
                },
                {
                  "sourceColumn": "",
                  "sourceColumnIndex": 2,
                  "samples": [],
                  "suggestedType": "UNMAPPED",
                  "suggestedTarget": null,
                  "displayLabel": null
                }
              ],
              "mapping": null
            }
        """.trimIndent()

        val GET_SAVED = """
            {
              "importId": "job-1",
              "status": "MAPPING_REQUIRED",
              "columns": [],
              "mapping": [
                {"sourceColumn": "Item Name", "sourceColumnIndex": 1, "type": "STANDARD", "target": "name"},
                {"sourceColumn": "Size", "sourceColumnIndex": 2, "type": "VARIANT_ATTRIBUTE", "target": "size"},
                {"sourceColumn": "Old", "sourceColumnIndex": 3, "type": "SOMETHING_NEW", "target": null}
              ]
            }
        """.trimIndent()
    }
}
