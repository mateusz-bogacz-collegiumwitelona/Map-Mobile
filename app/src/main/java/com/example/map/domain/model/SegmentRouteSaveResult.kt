package com.example.map.domain.model

sealed interface SegmentRouteSaveResult {
    data object Success : SegmentRouteSaveResult

    data class Failure(
        val error: SegmentRouteSaveError,
        val message: String? = null
    ) : SegmentRouteSaveResult
}

enum class SegmentRouteSaveError {
    INVALID_ROUTE,
    NOT_FOUND,
    UNAUTHORIZED,
    RATE_LIMITED,
    NETWORK,
    UNKNOWN
}
