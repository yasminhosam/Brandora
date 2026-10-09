package com.yasminhosam.brandora.feature.catalogimport.domain.model

/** Null cells keep their position so values stay aligned with the headers. */
data class SampleRow(
    val rowNumber: Int,
    val values: List<String?>
)
