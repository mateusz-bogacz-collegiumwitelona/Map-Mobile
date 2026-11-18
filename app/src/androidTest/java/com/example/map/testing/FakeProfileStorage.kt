package com.example.map.testing

import com.example.map.domain.repository.ProfileStorage

class FakeProfileStorage(initialEmail: String? = null) : ProfileStorage {

    private var email: String? = initialEmail

    override fun getEmail(): String? = email

    override fun saveEmail(email: String) {
        this.email = email
    }

    override fun clear() {
        email = null
    }
}