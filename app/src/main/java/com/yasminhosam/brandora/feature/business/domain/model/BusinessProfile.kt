package com.yasminhosam.brandora.feature.business.domain.model

/** The business profile as created on the backend. Timestamps are ignored: nothing displays them. */
data class BusinessProfile(
    val id: String,
    val name: String,
    val description: String? = null,
    val category: String? = null,
    val phone: String? = null,
    val address: String? = null
)
