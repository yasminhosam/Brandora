package com.yasminhosam.brandora.feature.business.data.remote

import com.yasminhosam.brandora.feature.business.data.mapper.toDomain
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessDtoJsonTest {

    // Mirrors the app's shared Json (see NetworkModule).
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Test
    fun `create request omits null optional fields and keeps the exact text`() {
        val encoded = json.encodeToString(
            CreateBusinessRequestDto.serializer(),
            CreateBusinessRequestDto(
                name = "متجر التمر",
                description = "",
                address = "الرياض"
            )
        )

        assertTrue(encoded.contains("\"name\":\"متجر التمر\""))
        assertTrue(encoded.contains("\"description\":\"\""))
        assertTrue(encoded.contains("\"address\":\"الرياض\""))
        assertFalse(encoded.contains("category"))
        assertFalse(encoded.contains("phone"))
    }

    @Test
    fun `profile response parses nulls and ignores timestamps`() {
        val domain = json.decodeFromString<BusinessProfileDto>(
            """
            {
              "id": "f6bc7287-3285-4e19-a819-459c41f0a138",
              "name": "متجر التمر",
              "description": null,
              "category": "تمور",
              "phone": null,
              "address": null,
              "createdAt": "2026-10-19T09:00:00Z",
              "updatedAt": "2026-10-19T09:00:00Z"
            }
            """.trimIndent()
        ).toDomain()

        assertEquals("f6bc7287-3285-4e19-a819-459c41f0a138", domain.id)
        assertEquals("متجر التمر", domain.name)
        assertNull(domain.description)
        assertEquals("تمور", domain.category)
        assertNull(domain.phone)
        assertNull(domain.address)
    }

    @Test
    fun `profile response tolerates missing optional keys`() {
        val domain = json.decodeFromString<BusinessProfileDto>(
            """{ "id": "f6bc7287-3285-4e19-a819-459c41f0a138", "name": "متجر التمر" }"""
        ).toDomain()

        assertNull(domain.description)
        assertNull(domain.category)
        assertNull(domain.phone)
        assertNull(domain.address)
    }
}
