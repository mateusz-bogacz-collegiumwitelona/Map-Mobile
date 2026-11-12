package com.example.map.testing

import com.example.map.domain.model.AuthTokens
import com.example.map.domain.model.ProfileUpdateResult
import com.example.map.domain.model.UserProfile
import com.example.map.domain.repository.ProfileRepository
import com.example.map.domain.repository.ProfileStorage
import com.example.map.domain.repository.TokenStorage
import kotlinx.coroutines.CompletableDeferred

class FakeUserProfileRepository : ProfileRepository {

    var remoteProfile: UserProfile? =
        UserProfile(firstName = "Jan", lastName = "Kowalski", isPublic = false)
    var updateNamesResult: ProfileUpdateResult = ProfileUpdateResult.Success
    var setVisibilityResult: ProfileUpdateResult = ProfileUpdateResult.Success

    var updateNamesGate: CompletableDeferred<Unit>? = null
    var setVisibilityGate: CompletableDeferred<Unit>? = null

    var getProfileCalls = 0
    var updateNamesCalls = 0
    var setVisibilityCalls = 0

    var lastNames: Pair<String, String>? = null
    var lastVisibility: Boolean? = null

    override suspend fun getProfile(): UserProfile? {
        getProfileCalls++
        return remoteProfile
    }

    override suspend fun updateNames(
        firstName: String,
        lastName: String
    ): ProfileUpdateResult {
        updateNamesCalls++
        lastNames = firstName to lastName
        updateNamesGate?.await()
        return updateNamesResult
    }

    override suspend fun setVisibility(isPublic: Boolean): ProfileUpdateResult {
        setVisibilityCalls++
        lastVisibility = isPublic
        setVisibilityGate?.await()
        return setVisibilityResult
    }
}

class FakeProfileStorage(initialProfile: UserProfile? = null) : ProfileStorage {

    private var storedProfile: UserProfile? = initialProfile
    private var storedEmail: String? = initialProfile?.email

    override fun getEmail(): String? = storedEmail ?: storedProfile?.email

    override fun saveEmail(email: String) {
        storedEmail = email
        storedProfile = (storedProfile ?: UserProfile()).copy(email = email)
    }

    override fun getProfile(): UserProfile? = storedProfile

    override fun saveProfile(profile: UserProfile) {
        storedProfile = profile
        storedEmail = profile.email
    }

    override fun clear() {
        storedProfile = null
        storedEmail = null
    }
}

class FakeSessionTokenStorage(private var accessToken: String? = null) : TokenStorage {

    override fun saveTokens(tokens: AuthTokens) {
        accessToken = tokens.accessToken
    }

    override fun getAccessToken(): String? = accessToken

    override fun getRefreshToken(): String? = null

    override fun clearTokens() {
        accessToken = null
    }
}
