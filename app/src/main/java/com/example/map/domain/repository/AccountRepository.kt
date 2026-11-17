package com.example.map.domain.repository

import com.example.map.domain.model.AccountDeletionResult
import com.example.map.domain.model.EmailChangeResult
import com.example.map.domain.model.UserProfile

interface AccountRepository {
    suspend fun requestEmailChange(newEmail: String): EmailChangeResult
    suspend fun confirmEmailChange(code: String): EmailChangeResult
    suspend fun deleteAccount(): AccountDeletionResult
    suspend fun getCurrentUserProfile(): UserProfile
}
