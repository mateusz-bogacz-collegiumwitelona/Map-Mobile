package com.example.map.testing

import com.example.map.domain.model.user.ProfileUpdateResult
import com.example.map.domain.model.user.UserProfile
import com.example.map.domain.repository.ProfileRepository
import kotlinx.coroutines.CompletableDeferred

class FakeUserProfileRepository : ProfileRepository {

    var remoteProfile: UserProfile? =
        UserProfile(firstName = "Jan", lastName = "Kowalski", isPublic = false)
    var updateNamesResult: ProfileUpdateResult = ProfileUpdateResult.Success
    var setVisibilityResult: ProfileUpdateResult = ProfileUpdateResult.Success

    var profileError: Exception? = null

    var updateNamesGate: CompletableDeferred<Unit>? = null
    var setVisibilityGate: CompletableDeferred<Unit>? = null
    var getProfileGate: CompletableDeferred<Unit>? = null

    var getProfileCalls = 0
    var updateNamesCalls = 0
    var setVisibilityCalls = 0

    var lastNames: Pair<String, String>? = null
    var lastVisibility: Boolean? = null

    override suspend fun getProfile(): UserProfile? {
        getProfileCalls++
        profileError?.let { throw it }
        getProfileGate?.await()
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
