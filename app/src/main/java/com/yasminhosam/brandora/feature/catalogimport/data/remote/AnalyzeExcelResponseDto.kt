package com.yasminhosam.brandora.feature.catalogimport.data.remote

import kotlinx.serialization.Serializable

@Serializable
internal data class AnalyzeExcelResponseDto(
    val importJobId: String,
    val status: String,
    val sheetNames: List<String> = emptyList(),
    val selectedSheetName: String? = null,
    val headerRow: Int = 0,
    val headers: List<String> = emptyList(),
    val sampleRows: List<SampleRowDto> = emptyList(),
    val totalRows: Int = 0
)
