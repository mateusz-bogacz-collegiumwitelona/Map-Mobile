package com.example.map.domain.model.account

enum class EmailVerificationError {
    INVALID_CODE,
    RATE_LIMITED,
    NETWORK,
    VALIDATION,
    UNKNOWN
}

sealed interface EmailVerificationResult {
    data object Success : EmailVerificationResult
    data class Failure(
        val error: EmailVerificationError,
        val message: String? = null
    ) : EmailVerificationResult
}