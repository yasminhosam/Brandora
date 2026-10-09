package com.yasminhosam.brandora.feature.catalogimport.domain.model

import java.io.InputStream

/**
 * The Android Uri never appears here: the data layer captures it inside [openStream], keeping
 * this type free of Android imports and unit-testable on the JVM.
 *
 * @property sizeBytes File size, or [UNKNOWN_SIZE] when the content provider does not report it.
 */
data class ExcelFile(
    val displayName: String,
    val sizeBytes: Long,
    val openStream: () -> InputStream
) {

    val isXlsx: Boolean
        get() = displayName.endsWith(XLSX_EXTENSION, ignoreCase = true)

    companion object {
        private const val XLSX_EXTENSION = ".xlsx"

        /** Size the local 10 MiB check cannot evaluate; the server's 413 becomes the guard. */
        const val UNKNOWN_SIZE = -1L
    }
}
