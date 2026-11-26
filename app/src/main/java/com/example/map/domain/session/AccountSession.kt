package com.example.map.domain.session

import com.example.map.domain.model.AccountDeletionResult
import com.example.map.domain.model.EmailChangeResult
import com.example.map.domain.model.ProfileUpdateResult
import com.example.map.domain.model.UserProfile
import com.example.map.domain.repository.AccountRepository
import com.example.map.domain.repository.ProfileRepository
import com.example.map.domain.repository.ProfileStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

class AccountSession(
    private val accountRepository: AccountRepository,
    private val profileRepository: ProfileRepository,
    private val profileStorage: ProfileStorage
) {
    private val _email = MutableStateFlow(profileStorage.getEmail().orEmpty())
    val email: StateFlow<String> = _email.asStateFlow()

    private val _profile = MutableStateFlow(
        profileStorage.getProfile() ?: UserProfile()
    )
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    private val sessionGeneration = AtomicLong(0)

    fun rememberEmail(email: String) {
        val normalized = email.trim()
        if (normalized.isEmpty()) return

        profileStorage.saveEmail(normalized)
        _email.value = normalized
    }

    suspend fun refreshProfile() {
        val generation = sessionGeneration.get()
        val remoteProfile = profileRepository.getProfile() ?: return
        persistIfSessionActive(remoteProfile, generation)
    }

    suspend fun updateNames(firstName: String, lastName: String): ProfileUpdateResult {
        val generation = sessionGeneration.get()
        val normalizedFirstName = firstName.trim()
        val normalizedLastName = lastName.trim()

        val result = profileRepository.updateNames(normalizedFirstName, normalizedLastName)

        if (result is ProfileUpdateResult.Success) {
            persistIfSessionActive(
                _profile.value.copy(
                    firstName = normalizedFirstName,
                    lastName = normalizedLastName
                ),
                generation
            )
        }

        return result
    }

    suspend fun updateVisibility(isPublic: Boolean): ProfileUpdateResult {
        val generation = sessionGeneration.get()
        val result = profileRepository.setVisibility(isPublic)

        if (result is ProfileUpdateResult.Success) {
            persistIfSessionActive(_profile.value.copy(isPublic = isPublic), generation)
        }

        return result
    }

    suspend fun requestEmailChange(newEmail: String): EmailChangeResult =
        accountRepository.requestEmailChange(newEmail.trim())

    suspend fun confirmEmailChange(code: String, newEmail: String): EmailChangeResult {
        val generation = sessionGeneration.get()
        val result = accountRepository.confirmEmailChange(code)

        if (result is EmailChangeResult.Success && sessionGeneration.get() == generation) {
            val normalized = newEmail.trim()
            profileStorage.saveEmail(normalized)
            _email.value = normalized
        }

        return result
    }

    suspend fun deleteAccount(): AccountDeletionResult =
        accountRepository.deleteAccount()

    fun clear() {
        sessionGeneration.incrementAndGet()
        profileStorage.clear()
        _email.value = ""
        _profile.value = UserProfile()
    }


    private fun persistIfSessionActive(profile: UserProfile, generation: Long) {
        if (sessionGeneration.get() != generation) return
        persist(profile)
    }

    private fun persist(profile: UserProfile) {
        val normalized = profile.copy(
            firstName = profile.firstName.trim(),
            lastName = profile.lastName.trim()
        )

        profileStorage.saveProfile(normalized)
        _profile.value = normalized
    }
}
