package com.yasminhosam.brandora.feature.products.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Typed request bodies.
 *
 * [productsWriteJson] encodes request bodies with explicit nulls — the shared NetworkModule codec
 * drops every null while encoding (`explicitNulls = false`) — because PATCH clears a nullable field
 * with an explicit `field: null` (backend: null clears, omission preserves). Only request bodies
 * use this codec; responses keep the shared one.
 */
internal val productsWriteJson =
    Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = true
    }

/** Encodes [value] as the JSON request body for the products API. */
internal inline fun <reified T> writeBody(value: T): RequestBody =
    productsWriteJson.encodeToString(value).toRequestBody("application/json".toMediaType())

@Serializable
data class ProductWriteDto(
    val name: String,
    val externalKey: String?,
    val description: String?,
    val basePrice: String?,
    val categoryId: String?,
    val options: Map<String, List<String>>,
)

@Serializable data class ActiveWriteDto(val isActive: Boolean)

@Serializable data class CategoryWriteDto(val name: String)

/** POST /variants accepts only these three fields — never isActive or inventory. */
@Serializable
data class VariantCreateDto(
    val sku: String?,
    val optionValues: Map<String, String>,
    val unitPrice: String?,
)

/** PATCH /variants always carries isActive when editing. */
@Serializable
data class VariantUpdateDto(
    val sku: String?,
    val optionValues: Map<String, String>,
    val unitPrice: String?,
    val isActive: Boolean,
)

@Serializable data class IsMainWriteDto(val isMain: Boolean)

@Serializable data class ImageOrderWriteDto(val imageIds: List<String>)

@Serializable data class StockAdjustWriteDto(val quantity: Int?, val trackInventory: Boolean)
