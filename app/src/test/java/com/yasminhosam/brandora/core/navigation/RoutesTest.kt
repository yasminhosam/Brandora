package com.yasminhosam.brandora.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class RoutesTest {

    @Test
    fun `reset password encodes reserved characters in the email`() {
        assertEquals(
            "reset_password?email=owner%2Btag%40example.com",
            Routes.resetPassword("owner+tag@example.com")
        )
        assertEquals(
            "reset_password?email=a%26b%23c%40example.com",
            Routes.resetPassword("a&b#c@example.com")
        )
    }
}
