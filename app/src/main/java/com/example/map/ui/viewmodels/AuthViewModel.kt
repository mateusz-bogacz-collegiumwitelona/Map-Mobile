package com.example.map.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.map.domain.model.AuthException
import com.example.map.domain.model.AuthFailure
import com.example.map.domain.session.AccountSession
import com.example.map.domain.session.AuthSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authSession: AuthSession,
    private val accountSession: AccountSession
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuthUiState(
            isAuthenticated = authSession.hasSavedSession(),
            profile = accountSession.profile.value
        )
    )
    val uiState = _uiState.asStateFlow()

    private var hydrateProfileJob: Job? = null

    init {
        viewModelScope.launch {
            accountSession.profile.collect { profile ->
                _uiState.update { it.copy(profile = profile) }
            }
        }
    }

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
                accountSession.rememberEmail(normalizedEmail)
                _uiState.update {
                    it.copy(
                        isAuthenticated = true,
                        isLoading = false,
                        errorMessage = null,
                        profile = accountSession.profile.value
                    )
                }

                hydrateProfile()
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

    private fun hydrateProfile() {
        hydrateProfileJob?.cancel()
        hydrateProfileJob = viewModelScope.launch {
            try {
                accountSession.refreshProfile()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Nieudane pobranie profilu nie może blokować logowania ani nawigacji.
            }
        }
    }

    private suspend fun cancelProfileHydrationAndWait() {
        hydrateProfileJob?.cancelAndJoin()
        hydrateProfileJob = null
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
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null)
            }

            // Zatrzymaj pobieranie profilu, zanim sesja zostanie wyczyszczona,
            // żeby spóźniona odpowiedź nie nadpisała wylogowanego stanu.
            cancelProfileHydrationAndWait()

            var logoutError: String? = null

            try {
                authSession.logout()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: AuthException) {
                if (error.failure != AuthFailure.INVALID_SESSION) {
                    logoutError =
                        "Usunięto sesję z urządzenia, ale serwer nie potwierdził wylogowania."
                }
            } catch (_: Exception) {
                logoutError =
                    "Usunięto sesję z urządzenia, ale serwer nie potwierdził wylogowania."
            } finally {
                accountSession.clear()
            }

            _uiState.value = AuthUiState(
                isAuthenticated = false,
                errorMessage = logoutError
            )
        }
    }

    fun onAccountDeleted() {
        // Anulowanie jest kooperatywne; gdyby odpowiedź zdążyła wrócić przed anulowaniem,
        // AccountSession.clear() unieważnia ją przez licznik generacji sesji.
        hydrateProfileJob?.cancel()
        hydrateProfileJob = null
        authSession.clearLocalSession()
        accountSession.clear()
        _uiState.value = AuthUiState(isAuthenticated = false)
    }

    private fun messageFor(failure: AuthFailure): String =
        when (failure) {
            AuthFailure.INVALID_CREDENTIALS ->
                "Nieprawidłowy e-mail lub hasło."

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

            AuthFailure.TOO_MANY_ATTEMPTS ->
                "Zbyt wiele prób logowania. Spróbuj ponownie później."

            AuthFailure.SERVER_ERROR,
            AuthFailure.INVALID_RESPONSE ->
                "Serwer nie mógł obsłużyć logowania. Spróbuj ponownie."
        }

    private companion object {
        val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}