package com.example.map.domain.validation

enum class PasswordError {
    BLANK,
    TOO_SHORT,
    WEAK,
    MISMATCH
}

object PasswordValidator {
    const val MIN_LENGHT = 8

    fun validatePassword(password: String): PasswordError? {
        if (password.isEmpty()) return PasswordError.BLANK
        if (password.length < MIN_LENGHT) return PasswordError.TOO_SHORT

        val hasUpper = password.any { it.isUpperCase() }
        val hasLower = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecial = password.any { !it.isLetterOrDigit() }

        return if (hasUpper && hasLower && hasDigit && hasSpecial) {
            null
        } else {
            PasswordError.WEAK
        }
    }

    fun validateConfirmPassword(
        password: String,
        confirmPassword: String
    ): PasswordError? = when {
        confirmPassword.isEmpty() -> PasswordError.BLANK
        confirmPassword != password -> PasswordError.MISMATCH
        else -> null
    }
}