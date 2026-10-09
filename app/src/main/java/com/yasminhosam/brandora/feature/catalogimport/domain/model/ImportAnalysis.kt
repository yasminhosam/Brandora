package com.yasminhosam.brandora.feature.catalogimport.domain.model

/** [headerRow] is decided by the backend and displayed read-only; the app never edits it. */
data class ImportAnalysis(
    val importJobId: String,
    val status: ImportStatus,
    val sheetNames: List<String>,
    val selectedSheetName: String?,
    val headerRow: Int,
    val headers: List<String>,
    val sampleRows: List<SampleRow>,
    val totalRows: Int
)
