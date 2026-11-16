package com.example.map.domain.validation

object EmailValidator {
    private const val MAX_LENGTH = 256
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun isValid(email: String): Boolean {
        val trimmed = email.trim()
        return trimmed.isNotEmpty() &&
            trimmed.length <= MAX_LENGTH &&
            EMAIL_REGEX.matches(trimmed)
    }
}
