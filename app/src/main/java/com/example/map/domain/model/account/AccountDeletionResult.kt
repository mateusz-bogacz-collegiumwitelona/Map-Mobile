package com.example.map.domain.model.account

sealed interface AccountDeletionResult {
    data object Success : AccountDeletionResult

    data class Failure(
        val error: AccountDeletionError,
        val message: String? = null
    ) : AccountDeletionResult
}

enum class AccountDeletionError {
    UNAUTHORIZED,
    NOT_FOUND,
    RATE_LIMITED,
    NETWORK,
    UNKNOWN
}
