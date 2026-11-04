package com.yasminhosam.brandora.feature.auth.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Pure-function coverage for the rules both auth screens rely on before any request. */
class AuthValidatorsTest {

    // --- email normalization ---

    @Test
    fun `normalize trims and lowercases the identity sent to the backend`() {
        assertEquals("shop@dates.example", normalizeEmail("  Shop@Dates.Example  "))
    }

    // --- email validation ---

    @Test
    fun `a blank email is required`() {
        assertEquals(AuthEmailError.REQUIRED, validateEmail(normalizeEmail("   ")))
    }

    @Test
    fun `a simple address validates`() {
        assertNull(validateEmail(normalizeEmail("A.B+Tag@Sub.Example.COM")))
    }

    @Test
    fun `a one-character last label still validates (syntax, not registry policy)`() {
        assertNull(validateEmail(normalizeEmail("a@b.c")))
    }

    @Test
    fun `254 characters pass and 255 fail`() {
        val suffix = "@b.co"
        val atLimit = "a".repeat(254 - suffix.length) + suffix
        val overLimit = "a".repeat(255 - suffix.length) + suffix

        assertNull(validateEmail(atLimit))
        assertEquals(AuthEmailError.TOO_LONG, validateEmail(overLimit))
    }

    @Test
    fun `a non-ASCII email is invalid`() {
        assertEquals(AuthEmailError.INVALID, validateEmail(normalizeEmail("user@exämple.com")))
        assertEquals(AuthEmailError.INVALID, validateEmail(normalizeEmail("مستخدم@example.com")))
    }

    @Test
    fun `missing at sign or domain dot is invalid`() {
        assertEquals(AuthEmailError.INVALID, validateEmail(normalizeEmail("nope")))
        assertEquals(AuthEmailError.INVALID, validateEmail(normalizeEmail("a@b")))
        assertEquals(AuthEmailError.INVALID, validateEmail(normalizeEmail("a@b@c.com")))
    }

    @Test
    fun `an email with spaces is invalid`() {
        assertEquals(AuthEmailError.INVALID, validateEmail(normalizeEmail("a b@c.com")))
    }

    // --- register password ---

    @Test
    fun `a blank register password is required`() {
        assertEquals(AuthPasswordError.REQUIRED, validateRegisterPassword("   "))
    }

    @Test
    fun `11 characters is too short and 12 passes`() {
        val twelve = "Aa1" + ".".repeat(9)

        assertEquals(AuthPasswordError.TOO_SHORT, validateRegisterPassword(twelve.dropLast(1)))
        assertNull(validateRegisterPassword(twelve))
    }

    @Test
    fun `128 characters passes and 129 fails`() {
        val valid = "Aa1" + "x".repeat(PASSWORD_MAX_LENGTH - 3)

        assertNull(validateRegisterPassword(valid))
        assertEquals(AuthPasswordError.TOO_LONG, validateRegisterPassword(valid + "x"))
    }

    @Test
    fun `each character class must appear at least once`() {
        assertEquals(AuthPasswordError.MISSING_COMPLEXITY, validateRegisterPassword("aaaaaaaa1111"))
        assertEquals(AuthPasswordError.MISSING_COMPLEXITY, validateRegisterPassword("aaaaaaaaAAAA"))
        assertEquals(AuthPasswordError.MISSING_COMPLEXITY, validateRegisterPassword("AAAAAAA11111"))
    }

    @Test
    fun `symbols and spaces are allowed in a register password`() {
        assertNull(validateRegisterPassword("Ab 1!defghij"))
    }

    @Test
    fun `the register password is never trimmed`() {
        // 12 characters INCLUDING the leading space: trimming would drop it to 11 (too short).
        assertNull(validateRegisterPassword(" Abcdefghij1"))
    }

    // --- login password ---

    @Test
    fun `a blank login password is required`() {
        assertEquals(AuthPasswordError.REQUIRED, validateLoginPassword(""))
    }

    @Test
    fun `the login password only has a length rule`() {
        assertNull(validateLoginPassword("x"))
        assertNull(validateLoginPassword("p".repeat(PASSWORD_MAX_LENGTH)))
        assertEquals(
            AuthPasswordError.TOO_LONG,
            validateLoginPassword("p".repeat(PASSWORD_MAX_LENGTH + 1))
        )
    }
}
