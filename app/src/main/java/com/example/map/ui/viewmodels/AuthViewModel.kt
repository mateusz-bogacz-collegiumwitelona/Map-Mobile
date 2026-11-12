package com.example.map.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.map.domain.model.AuthTokens
import com.example.map.domain.model.UserProfile
import com.example.map.domain.usecase.AuthSession
import com.example.map.domain.usecase.ProfileSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isAuthenticated: Boolean,
    val profile: UserProfile = UserProfile()
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authSession: AuthSession,
    private val profileSession: ProfileSession
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuthUiState(
            isAuthenticated = authSession.hasSavedSession(),
            profile = profileSession.profile.value
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            profileSession.profile.collect { profile ->
                _uiState.update { it.copy(profile = profile) }
            }
        }
    }

    fun onDemoLogin(email: String) {
        profileSession.rememberEmail(email)
        _uiState.update { it.copy(isAuthenticated = true) }
    }

    fun onLoginSucceeded(tokens: AuthTokens, rememberMe: Boolean, email: String? = null) {
        authSession.saveAfterLogin(tokens, rememberMe)
        email?.let(profileSession::rememberEmail)
        _uiState.update { it.copy(isAuthenticated = true) }
    }

    fun logout() {
        authSession.logout()
        profileSession.clear()
        _uiState.update {
            it.copy(
                isAuthenticated = false,
                profile = UserProfile()
            )
        }
    }
}
