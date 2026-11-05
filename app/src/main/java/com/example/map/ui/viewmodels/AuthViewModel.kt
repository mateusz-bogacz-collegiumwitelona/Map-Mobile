package com.example.map.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.example.map.domain.model.AuthTokens
import com.example.map.domain.usecase.AuthSession
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AuthUiState(
    val isAuthenticated: Boolean
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authSession: AuthSession
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuthUiState(isAuthenticated = authSession.hasSavedSession())
    )
    val uiState = _uiState.asStateFlow()

    fun onDemoLogin() {
        _uiState.update { it.copy(isAuthenticated = true) }
    }

    fun onLoginSucceeded(tokens: AuthTokens, rememberMe: Boolean) {
        authSession.saveAfterLogin(tokens, rememberMe)
        _uiState.update { it.copy(isAuthenticated = true) }
    }

    fun logout() {
        authSession.logout()
        _uiState.update { it.copy(isAuthenticated = false) }
    }
}