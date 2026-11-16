package com.example.map.domain.model

sealed interface EmailChangeResult {
    data object Success : EmailChangeResult

    data class Failure(
        val error: EmailChangeError,
        val message: String? = null
    ) : EmailChangeResult
}

enum class EmailChangeError {
    EMAIL_INVALID,
    EMAIL_TAKEN,
    EMAIL_UNCHANGED,
    INVALID_CODE,
    CODE_EXPIRED,
    ATTEMPTS_EXCEEDED,
    RATE_LIMITED,
    UNAUTHORIZED,
    NETWORK,
    UNKNOWN
}
