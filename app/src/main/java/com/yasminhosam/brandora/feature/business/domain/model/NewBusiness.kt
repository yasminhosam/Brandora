package com.yasminhosam.brandora.feature.business.domain.model

/**
 * The create-business payload. Only these five fields are accepted; the backend rejects ids,
 * timestamps, settings, and workingHours on this endpoint.
 */
data class NewBusiness(
    val name: String,
    val description: String? = null,
    val category: String? = null,
    val phone: String? = null,
    val address: String? = null
)
