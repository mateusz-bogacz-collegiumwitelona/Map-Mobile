package com.example.map.domain.validation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmailValidatorTest {

    @Test
    fun rejectsBlankEmail() {
        assertFalse(EmailValidator.isValid(""))
        assertFalse(EmailValidator.isValid("   "))
    }

    @Test
    fun rejectsMalformedEmail() {
        assertFalse(EmailValidator.isValid("not-an-email"))
        assertFalse(EmailValidator.isValid("user@"))
        assertFalse(EmailValidator.isValid("@example.com"))
        assertFalse(EmailValidator.isValid("user@example"))
    }

    @Test
    fun rejectsEmailLongerThan256Characters() {
        val longLocalPart = "a".repeat(250)
        val email = "$longLocalPart@example.com"

        assertTrue(email.length > 256)
        assertFalse(EmailValidator.isValid(email))
    }

    @Test
    fun acceptsValidEmailAndTrimsWhitespace() {
        assertTrue(EmailValidator.isValid("user@example.com"))
        assertTrue(EmailValidator.isValid("  user.name+tag@example.co.uk  "))
    }
}
