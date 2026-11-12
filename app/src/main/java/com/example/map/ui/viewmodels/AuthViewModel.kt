package com.example.map.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.map.domain.model.AuthException
import com.example.map.domain.model.AuthFailure
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

            try {
                authSession.login(normalizedEmail, password)
                _uiState.value = AuthUiState(isAuthenticated = true)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: AuthException) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = messageFor(error.failure)
                    )
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Nie udało się zalogować. Spróbuj ponownie."
                    )
                }
            }
        }
    }

    fun clearErrors() {
        _uiState.update {
            it.copy(
                emailError = null,
                passwordError = null,
                errorMessage = null
            )
        }
    }

    fun logout() {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            var logoutError: String? = null

            try {
                authSession.logout()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                logoutError =
                    "Usunięto sesję z urządzenia, ale serwer nie potwierdził wylogowania."
            }

            _uiState.value = AuthUiState(
                isAuthenticated = false,
                errorMessage = logoutError
            )
        }
    }

    private fun messageFor(failure: AuthFailure): String =
        when (failure) {
            AuthFailure.INVALID_CREDENTIALS ->
                "Nieprawidłowy e-mail lub hasło. Konto może też wymagać potwierdzenia adresu e-mail."
            AuthFailure.EMAIL_NOT_CONFIRMED ->
                "Potwierdź adres e-mail przed zalogowaniem."
            AuthFailure.ACCOUNT_LOCKED ->
                "Konto jest tymczasowo zablokowane. Spróbuj ponownie później."
            AuthFailure.ACCOUNT_BANNED ->
                "To konto nie może się zalogować."
            AuthFailure.ROLE_NOT_ALLOWED ->
                "Logowanie jest dostępne wyłącznie dla kont z rolą USER."
            AuthFailure.INVALID_SESSION ->
                "Sesja wygasła. Zaloguj się ponownie."
            AuthFailure.NETWORK_ERROR ->
                "Brak połączenia z serwerem. Sprawdź internet i spróbuj ponownie."
            AuthFailure.SERVER_ERROR,
            AuthFailure.INVALID_RESPONSE ->
                "Serwer nie mógł obsłużyć logowania. Spróbuj ponownie."
        }

    private companion object {
        val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}