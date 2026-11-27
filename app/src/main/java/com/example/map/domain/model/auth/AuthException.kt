package com.example.map.domain.model.auth

class AuthException(
    val failure: AuthFailure,
    cause: Throwable? = null
) : Exception(failure.name, cause)