package com.yasminhosam.brandora.feature.catalogimport.domain.file

import com.yasminhosam.brandora.feature.catalogimport.domain.model.ExcelFile

/**
 * The reference is a String, not an `android.net.Uri`, so this port can live in the domain
 * layer; the data layer parses it back. Implementations never throw: failure returns null and
 * becomes a validation error instead of a crash.
 */
interface ExcelFileFactory {

    fun create(uri: String): ExcelFile?
}
