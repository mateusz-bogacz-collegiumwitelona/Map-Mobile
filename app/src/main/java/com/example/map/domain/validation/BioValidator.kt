package com.example.map.domain.validation

enum class BioError {
    TOO_LONG
}

object BioValidator {
    const val MAX_LENGTH = 500

    fun validate(bio: String?): BioError? {
        if (bio.isNullOrBlank()) return null
        return if (bio.trim().length > MAX_LENGTH) {
            BioError.TOO_LONG
        } else {
            null
        }
    }
}