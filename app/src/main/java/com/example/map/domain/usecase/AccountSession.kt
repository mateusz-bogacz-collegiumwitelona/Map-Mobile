package com.example.map.domain.usecase

import com.example.map.domain.model.EmailChangeResult
import com.example.map.domain.repository.AccountRepository
import com.example.map.domain.repository.ProfileStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AccountSession(
    private val accountRepository: AccountRepository,
    private val profileStorage: ProfileStorage
) {
    private val _email = MutableStateFlow(profileStorage.getEmail().orEmpty())
    val email: StateFlow<String> = _email.asStateFlow()

    fun rememberEmail(email: String) {
        val normalized = email.trim()
        if (normalized.isEmpty()) return

        profileStorage.saveEmail(normalized)
        _email.value = normalized
    }

    suspend fun requestEmailChange(newEmail: String): EmailChangeResult =
        accountRepository.requestEmailChange(newEmail.trim())

    suspend fun confirmEmailChange(code: String, newEmail: String): EmailChangeResult {
        val result = accountRepository.confirmEmailChange(code)

        if (result is EmailChangeResult.Success) {
            val normalized = newEmail.trim()
            profileStorage.saveEmail(normalized)
            _email.value = normalized
        }

        return result
    }

    fun clear() {
        profileStorage.clear()
        _email.value = ""
    }
}
