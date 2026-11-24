package com.example.map.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RegistrationValidatorTest {
    @Test
    fun `valid data passes all validations`() {
        // Arange & Act
        val result = RegistrationValidator.validate(
            firstName = " Jan ",
            lastName = " Kowalski ",
            email = "jan.kowalski@example.com",
            password = "Password1!",
            confirmPassword = "Password1!",
            bio = "Lubię góry i mapy"
        )

        //Assert
        assertTrue(result.isValid)
        assertNull(result.firstNameError)
        assertNull(result.lastNameError)
        assertTrue(result.isEmailValid)
        assertNull(result.passwordError)
        assertNull(result.confirmPasswordError)
        assertNull(result.bioError)
    }

    @Test
    fun `blank bio is allowed while too long bio returns TOO_LONG`() {
        // Assert
        assertNull(BioValidator.validate(null))
        assertNull(BioValidator.validate("   "))
        assertEquals(
            BioError.TOO_LONG,
            BioValidator.validate("a".repeat(BioValidator.MAX_LENGTH + 1))
        )
    }

    @Test
    fun `password validation detects blank too short weak and mismatch`() {
        // Assert
        assertEquals(PasswordError.BLANK, PasswordValidator.validatePassword(""))
        assertEquals(PasswordError.TOO_SHORT, PasswordValidator.validatePassword("Ab1!"))
        assertEquals(PasswordError.WEAK, PasswordValidator.validatePassword("Password1"))

        assertEquals(
            PasswordError.MISMATCH,
            PasswordValidator.validateConfirmPassword("Password1!", "Password2!")
        )
        assertNull(
            PasswordValidator.validateConfirmPassword("Password1!", "Password1!")
        )
    }

    @Test
    fun `invalid form reports field errors and blocks submission`() {
        // Arange & Act
        val result = RegistrationValidator.validate(
            firstName = "",
            lastName = "A",
            email = "zly-email",
            password = "weak",
            confirmPassword = "other",
            bio = "x".repeat(BioValidator.MAX_LENGTH + 5)
        )

        // Assert
        assertFalse(result.isValid)
        assertEquals(ProfileNameError.BLANK, result.firstNameError)
        assertEquals(ProfileNameError.TOO_SHORT, result.lastNameError)
        assertFalse(result.isEmailValid)
        assertEquals(PasswordError.TOO_SHORT, result.passwordError)
        assertEquals(PasswordError.MISMATCH, result.confirmPasswordError)
        assertEquals(BioError.TOO_LONG, result.bioError)
    }
}