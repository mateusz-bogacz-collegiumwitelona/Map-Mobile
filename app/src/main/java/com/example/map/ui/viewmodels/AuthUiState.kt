package com.example.map.ui.viewmodels

import com.example.map.domain.model.UserProfile

data class AuthUiState(
    val isAuthenticated: Boolean,
    val isLoading: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val errorMessage: String? = null,
    val profile: UserProfile = UserProfile()
)
