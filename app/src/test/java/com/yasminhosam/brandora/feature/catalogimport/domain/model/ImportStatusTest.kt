package com.yasminhosam.brandora.feature.catalogimport.domain.model

import com.yasminhosam.brandora.core.network.ImportStatusValues
import org.junit.Assert.assertEquals
import org.junit.Test

class ImportStatusTest {

    @Test
    fun `every known backend value maps to its enum entry`() {
        assertEquals(ImportStatus.MAPPING_REQUIRED, ImportStatus.from(ImportStatusValues.MAPPING_REQUIRED))
        assertEquals(ImportStatus.READY, ImportStatus.from(ImportStatusValues.READY))
        assertEquals(ImportStatus.COMPLETED, ImportStatus.from(ImportStatusValues.COMPLETED))
        assertEquals(
            ImportStatus.COMPLETED_WITH_ERRORS,
            ImportStatus.from(ImportStatusValues.COMPLETED_WITH_ERRORS)
        )
        assertEquals(ImportStatus.FAILED, ImportStatus.from(ImportStatusValues.FAILED))
    }

    @Test
    fun `an unknown value becomes UNKNOWN instead of throwing`() {
        assertEquals(ImportStatus.UNKNOWN, ImportStatus.from("SOMETHING_NEW"))
    }

    @Test
    fun `a missing value becomes UNKNOWN instead of throwing`() {
        assertEquals(ImportStatus.UNKNOWN, ImportStatus.from(null))
    }

    @Test
    fun `matching is case sensitive so backend typos are not silently accepted`() {
        assertEquals(ImportStatus.UNKNOWN, ImportStatus.from("mapping_required"))
    }
}
