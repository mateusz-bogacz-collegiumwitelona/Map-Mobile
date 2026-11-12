package com.example.map.domain.model

enum class ProfileUpdateError {
    FIRST_NAME_INVALID,
    LAST_NAME_INVALID,
    UNAUTHORIZED,
    NOT_FOUND,
    RATE_LIMITED,
    NETWORK,
    UNKNOWN
}

sealed interface ProfileUpdateResult {
    data object Success : ProfileUpdateResult

    data class Failure(
        val error: ProfileUpdateError,
        val message: String? = null
    ) : ProfileUpdateResult
}
