package com.yasminhosam.brandora.feature.catalogimport.presentation

import com.yasminhosam.brandora.feature.catalogimport.domain.model.ExcelFile
import com.yasminhosam.brandora.feature.catalogimport.domain.model.ImportAnalysis
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The route carries only the importJobId; the full analysis travels through here. Does NOT
 * survive process death: after a kill the user re-picks and gets a fresh importJobId.
 */
@Singleton
class ImportSessionStore @Inject constructor() {

    data class Session(
        val file: ExcelFile,
        val analysis: ImportAnalysis
    )

    var session: Session? = null
        private set

    fun write(file: ExcelFile, analysis: ImportAnalysis) {
        session = Session(file, analysis)
    }

    fun clear() {
        session = null
    }
}
