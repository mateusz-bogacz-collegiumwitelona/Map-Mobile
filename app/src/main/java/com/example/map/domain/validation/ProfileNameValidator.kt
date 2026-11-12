package com.example.map.domain.validation

enum class ProfileNameError {
    BLANK,
    TOO_SHORT,
    TOO_LONG
}

object ProfileNameValidator {
    const val MIN_LENGTH = 2
    const val MAX_LENGTH = 50

    fun validate(value: String): ProfileNameError? {
        val trimmed = value.trim()

        return when {
            trimmed.isEmpty() -> ProfileNameError.BLANK
            trimmed.length < MIN_LENGTH -> ProfileNameError.TOO_SHORT
            trimmed.length > MAX_LENGTH -> ProfileNameError.TOO_LONG
            else -> null
        }
    }
}
