package com.example.map.domain.model.auth

sealed interface RegistrationResult {
    data object Success : RegistrationResult

    data class Failure(
        val error: RegistrationError,
        val message: String? = null
    ) : RegistrationResult
}

enum class RegistrationError {
    FIRST_NAME_INVALID,
    LAST_NAME_INVALID,
    BIO_INVALID,
    EMAIL_INVALID,
    VALIDATION_ERROR,
    RATE_LIMITED,
    NETWORK,
    UNKNOWN
}