package com.yasminhosam.brandora.feature.catalogimport.domain.model

import com.yasminhosam.brandora.core.network.ImportStatusValues

enum class ImportStatus {
    MAPPING_REQUIRED,
    READY,
    COMPLETED,
    COMPLETED_WITH_ERRORS,
    FAILED,
    UNKNOWN;

    companion object {

        /** Unknown or missing values become [UNKNOWN] so a new backend status degrades gracefully. */
        fun from(raw: String?): ImportStatus = when (raw) {
            ImportStatusValues.MAPPING_REQUIRED -> MAPPING_REQUIRED
            ImportStatusValues.READY -> READY
            ImportStatusValues.COMPLETED -> COMPLETED
            ImportStatusValues.COMPLETED_WITH_ERRORS -> COMPLETED_WITH_ERRORS
            ImportStatusValues.FAILED -> FAILED
            else -> UNKNOWN
        }
    }
}
