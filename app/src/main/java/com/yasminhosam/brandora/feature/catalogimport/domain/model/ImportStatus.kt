package com.yasminhosam.brandora.feature.catalogimport.domain.model

import com.yasminhosam.brandora.core.network.ImportStatusValues

enum class ImportStatus {
    UPLOADED,
    ANALYZING,
    MAPPING_REQUIRED,
    VALIDATING,
    READY,
    IMPORTING,
    COMPLETED,
    COMPLETED_WITH_ERRORS,
    FAILED,
    CANCELLED,
    UNKNOWN;

    companion object {

        /** Unknown or missing values become [UNKNOWN] so a new backend status degrades gracefully. */
        fun from(raw: String?): ImportStatus = when (raw) {
            ImportStatusValues.UPLOADED -> UPLOADED
            ImportStatusValues.ANALYZING -> ANALYZING
            ImportStatusValues.MAPPING_REQUIRED -> MAPPING_REQUIRED
            ImportStatusValues.VALIDATING -> VALIDATING
            ImportStatusValues.READY -> READY
            ImportStatusValues.IMPORTING -> IMPORTING
            ImportStatusValues.COMPLETED -> COMPLETED
            ImportStatusValues.COMPLETED_WITH_ERRORS -> COMPLETED_WITH_ERRORS
            ImportStatusValues.FAILED -> FAILED
            ImportStatusValues.CANCELLED -> CANCELLED
            else -> UNKNOWN
        }
    }
}
