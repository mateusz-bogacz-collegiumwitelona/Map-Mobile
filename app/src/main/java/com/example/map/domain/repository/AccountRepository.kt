package com.example.map.domain.repository

import com.example.map.domain.model.EmailChangeResult

interface AccountRepository {
    suspend fun requestEmailChange(newEmail: String): EmailChangeResult
    suspend fun confirmEmailChange(code: String): EmailChangeResult
}
