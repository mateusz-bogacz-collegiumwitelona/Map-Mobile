package com.example.map.domain.repository

import com.example.map.domain.model.ProfileUpdateResult
import com.example.map.domain.model.UserProfile

interface ProfileRepository {
    suspend fun getProfile(): UserProfile?
    suspend fun updateNames(firstName: String, lastName: String): ProfileUpdateResult
    suspend fun setVisibility(isPublic: Boolean): ProfileUpdateResult
}
