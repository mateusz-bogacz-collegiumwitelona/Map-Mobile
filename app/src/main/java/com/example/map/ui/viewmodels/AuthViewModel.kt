package com.example.map.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.example.map.domain.model.AuthTokens
import com.example.map.domain.usecase.AccountSession
import com.example.map.domain.usecase.AuthSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class AuthUiState(
    val isAuthenticated: Boolean
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authSession: AuthSession,
    private val accountSession: AccountSession
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuthUiState(isAuthenticated = authSession.hasSavedSession())
    )
    val uiState = _uiState.asStateFlow()

    fun onDemoLogin(email: String) {
        accountSession.rememberEmail(email)
        _uiState.update { it.copy(isAuthenticated = true) }
    }

    fun onLoginSucceeded(tokens: AuthTokens, rememberMe: Boolean, email: String? = null) {
        authSession.saveAfterLogin(tokens, rememberMe)
        email?.let(accountSession::rememberEmail)
        _uiState.update { it.copy(isAuthenticated = true) }
    }

    fun logout() {
        authSession.logout()
        accountSession.clear()
        _uiState.update { it.copy(isAuthenticated = false) }
    }
}
