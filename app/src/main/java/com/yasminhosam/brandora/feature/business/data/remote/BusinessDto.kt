package com.yasminhosam.brandora.feature.business.data.remote

import kotlinx.serialization.Serializable

@Serializable
internal data class CreateBusinessRequestDto(
    val name: String,
    val description: String? = null,
    val category: String? = null,
    val phone: String? = null,
    val address: String? = null
)

/** createdAt/updatedAt are left to ignoreUnknownKeys: nothing in the app displays them. */
@Serializable
internal data class BusinessProfileDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val category: String? = null,
    val phone: String? = null,
    val address: String? = null
)
