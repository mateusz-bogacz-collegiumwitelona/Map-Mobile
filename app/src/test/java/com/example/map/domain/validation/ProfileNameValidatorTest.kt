package com.example.map.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileNameValidatorTest {

    @Test
    fun emptyNameIsBlank() {
        assertEquals(ProfileNameError.BLANK, ProfileNameValidator.validate(""))
    }

    @Test
    fun whitespaceOnlyNameIsBlank() {
        assertEquals(ProfileNameError.BLANK, ProfileNameValidator.validate("   "))
    }

    @Test
    fun singleCharacterNameIsTooShort() {
        assertEquals(ProfileNameError.TOO_SHORT, ProfileNameValidator.validate("A"))
    }

    @Test
    fun singleCharacterWithSurroundingWhitespaceIsTooShort() {
        assertEquals(ProfileNameError.TOO_SHORT, ProfileNameValidator.validate("  A  "))
    }

    @Test
    fun twoCharacterNameIsValid() {
        assertNull(ProfileNameValidator.validate("Al"))
    }

    @Test
    fun fiftyCharacterNameIsValid() {
        assertNull(ProfileNameValidator.validate("a".repeat(ProfileNameValidator.MAX_LENGTH)))
    }

    @Test
    fun fiftyOneCharacterNameIsTooLong() {
        assertEquals(
            ProfileNameError.TOO_LONG,
            ProfileNameValidator.validate("a".repeat(ProfileNameValidator.MAX_LENGTH + 1))
        )
    }

    @Test
    fun lengthIsMeasuredAfterTrimming() {
        assertNull(
            ProfileNameValidator.validate(
                "  " + "a".repeat(ProfileNameValidator.MAX_LENGTH) + "  "
            )
        )
    }
}
