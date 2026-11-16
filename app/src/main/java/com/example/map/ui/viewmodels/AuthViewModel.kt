package com.example.map.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.map.domain.model.AuthException
import com.example.map.domain.model.AuthFailure
import com.example.map.domain.usecase.AccountSession
import com.example.map.domain.usecase.AuthSession
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.CancellationException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authSession: AuthSession
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuthUiState(isAuthenticated = authSession.hasSavedSession())
    )
    val uiState = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        if (_uiState.value.isLoading) return

        val normalizedEmail = email.trim()
        val emailError = if (EMAIL_REGEX.matches(normalizedEmail)) {
            null
        } else {
            "Wpisz poprawny adres e-mail."
        }
        val passwordError = if (password.isNotBlank()) {
            null
        } else {
            "Wpisz hasło."
        }

        if (emailError != null || passwordError != null) {
            _uiState.update {
                it.copy(
                    emailError = emailError,
                    passwordError = passwordError,
                    errorMessage = null
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    emailError = null,
                    passwordError = null,
                    errorMessage = null
                )
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