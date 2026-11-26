package com.yasminhosam.brandora.feature.products.domain.model

/**
 * Everything the manual product editor collects. Screens build drafts and the data layer maps them
 * to typed request bodies, so no hand-built JSON ever reaches the API. The editor always sends
 * every field: null clears it, matching the backend's "null clears / omission preserves" PATCH
 * semantics.
 */
data class ProductDraft(
    val name: String,
    val externalKey: String? = null,
    val description: String? = null,
    val basePrice: String? = null,
    val categoryId: String? = null,
    val options: Map<String, List<String>> = emptyMap(),
)

/**
 * Variant editor payload. Creation and edit have different contracts: POST /variants only accepts
 * sku/optionValues/unitPrice (VariantCreateDto never carries isActive), while PATCH always sends
 * isActive when editing (VariantUpdateDto requires it).
 */
data class VariantDraft(
    val sku: String? = null,
    val unitPrice: String? = null,
    val optionValues: Map<String, String> = emptyMap(),
    val isActive: Boolean? = null,
)
