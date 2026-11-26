package com.example.map.domain.repository

import com.example.map.domain.model.UserProfile

interface ProfileStorage {
    fun getEmail(): String?
    fun saveEmail(email: String)
    fun getProfile(): UserProfile?
    fun saveProfile(profile: UserProfile)
    fun clear()
}
