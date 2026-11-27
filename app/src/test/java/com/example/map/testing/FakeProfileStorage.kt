package com.example.map.testing

import com.example.map.domain.model.UserProfile
import com.example.map.domain.repository.ProfileStorage

class FakeProfileStorage(
    initialEmail: String? = null,
    initialProfile: UserProfile? = null
) : ProfileStorage {

    private var email: String? = initialEmail
    private var profile: UserProfile? = initialProfile

    override fun getEmail(): String? = email

    override fun saveEmail(email: String) {
        this.email = email
    }

    override fun getProfile(): UserProfile? = profile

    override fun saveProfile(profile: UserProfile) {
        this.profile = profile
    }

    override fun clear() {
        email = null
        profile = null
    }
}