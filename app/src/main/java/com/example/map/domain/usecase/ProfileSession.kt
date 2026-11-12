package com.example.map.domain.usecase

import com.example.map.domain.model.ProfileUpdateResult
import com.example.map.domain.model.UserProfile
import com.example.map.domain.repository.ProfileRepository
import com.example.map.domain.repository.ProfileStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProfileSession(
    private val profileRepository: ProfileRepository,
    private val profileStorage: ProfileStorage
) {
    private val _profile = MutableStateFlow(loadInitialProfile())
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    fun rememberEmail(email: String) {
        val normalized = email.trim()
        if (normalized.isEmpty()) return

        profileStorage.saveEmail(normalized)
        persist(_profile.value.copy(email = normalized))
    }

    suspend fun refresh() {
        val remoteProfile = profileRepository.getProfile() ?: return
        val email = _profile.value.email.ifBlank { profileStorage.getEmail().orEmpty() }
        persist(remoteProfile.copy(email = email))
    }

    suspend fun updateNames(firstName: String, lastName: String): ProfileUpdateResult {
        val normalizedFirstName = firstName.trim()
        val normalizedLastName = lastName.trim()

        val result = profileRepository.updateNames(normalizedFirstName, normalizedLastName)

        if (result is ProfileUpdateResult.Success) {
            persist(
                _profile.value.copy(
                    firstName = normalizedFirstName,
                    lastName = normalizedLastName
                )
            )
        }

        return result
    }

    suspend fun updateVisibility(isPublic: Boolean): ProfileUpdateResult {
        val result = profileRepository.setVisibility(isPublic)

        if (result is ProfileUpdateResult.Success) {
            persist(_profile.value.copy(isPublic = isPublic))
        }

        return result
    }

    fun clear() {
        profileStorage.clear()
        _profile.value = UserProfile()
    }

    private fun loadInitialProfile(): UserProfile =
        profileStorage.getProfile()
            ?: UserProfile(email = profileStorage.getEmail().orEmpty())

    private fun persist(profile: UserProfile) {
        profileStorage.saveProfile(profile)
        _profile.value = profile
    }
}
