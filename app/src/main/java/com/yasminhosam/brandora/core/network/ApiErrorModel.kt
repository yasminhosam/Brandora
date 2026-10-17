package com.yasminhosam.brandora.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

@Serializable
data class ApiErrorModel(
    val statusCode: Int?,
    val code: String?,//stable identifier (e.g. MAPPING_NAME_REQUIRED) used for localization
    val message: JsonElement? = null,
    val error: String?
){
    fun readableMessage(): String? = when (message) {
        is JsonArray -> message.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            .joinToString("\n")
            .ifBlank { null }
        is JsonPrimitive -> message.contentOrNull
        else -> null
    }
}