package com.yasminhosam.brandora.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class PaginationDto(
    val limit: Int = 20,
    val offset: Int = 0,
    val totalCount: Int = 0,
    val hasMore: Boolean = false,
    val nextOffset: Int? = null,
)