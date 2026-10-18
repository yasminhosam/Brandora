package com.yasminhosam.brandora.core.ui

import com.yasminhosam.brandora.R
import com.yasminhosam.brandora.core.network.ValidationErrorCodes
import org.junit.Assert.assertEquals
import org.junit.Test

class ValidationErrorMappingTest {

    @Test
    fun `every known row-error code maps to its own message`() {
        val expected = mapOf(
            ValidationErrorCodes.REQUIRED to R.string.validation_error_required,
            ValidationErrorCodes.TEXT_TOO_LONG to R.string.validation_error_too_long,
            ValidationErrorCodes.INVALID_DECIMAL to R.string.validation_error_decimal,
            ValidationErrorCodes.INVALID_INTEGER to R.string.validation_error_integer,
            ValidationErrorCodes.DUPLICATE_EXTERNAL_KEY to R.string.validation_error_duplicate_sku
        )

        expected.forEach { (code, res) ->
            assertEquals("code $code", res, code.validationErrorRes())
        }
    }

    @Test
    fun `an unknown code falls back to the generic message`() {
        assertEquals(R.string.validation_error_field_generic, "SOME_NEW_CODE".validationErrorRes())
    }

    @Test
    fun `mapped field keys reuse the mapping target labels`() {
        assertEquals(R.string.mapping_target_base_price, "basePrice".fieldLabelRes())
        assertEquals(R.string.mapping_target_name, "name".fieldLabelRes())
        assertEquals(R.string.validation_field_generic, "mysteryField".fieldLabelRes())
    }
}
