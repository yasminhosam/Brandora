package com.yasminhosam.brandora.feature.catalogimport.domain.mapping

data class MappedColumn(
    val sourceColumn: String,
    val sourceColumnIndex: Int,
    val samples: List<String>,
    val choice: MappingChoice
)
