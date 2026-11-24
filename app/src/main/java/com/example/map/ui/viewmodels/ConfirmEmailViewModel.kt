package com.example.map.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.map.domain.model.EmailVerificationError
import com.example.map.domain.model.EmailVerificationResult
import com.example.map.domain.session.AuthSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConfirmEmailUiState(
    val email: String = "",
    val code: String = "",
    val codeError: String? = null,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val isSubmitting: Boolean = false,
    val isResending: Boolean = false,
    val isVerified: Boolean = false
)

@HiltViewModel
class ConfirmEmailViewModel @Inject constructor(
    private val authSession: AuthSession
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ConfirmEmailUiState(email = authSession.pendingVerificationEmail.value.orEmpty())
    )
    val uiState: StateFlow<ConfirmEmailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authSession.pendingVerificationEmail.collect { pendingEmail ->
                if (!pendingEmail.isNullOrBlank()) {
                    _uiState.update { state ->
                        if (state.email == pendingEmail) state else state.copy(email = pendingEmail)
                    }
                }
            }
        }
    }

    fun onCodeChange(newCode: String) {
        val filtered = newCode.filter { it.isDigit() }.take(6)
        _uiState.update {
            it.copy(
                code = filtered,
                codeError = null,
                errorMessage = null
            )
        }
    }

    fun confirmEmail() {
        if (_uiState.value.isSubmitting) return

        val currentCode = _uiState.value.code.trim()
        if (currentCode.length != 6) {
            _uiState.update {
                it.copy(codeError = "Wpisz 6-cyfrowy kod weryfikacyjny.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSubmitting = true,
                    codeError = null,
                    errorMessage = null,
                    infoMessage = null
                )
            }

            when (val result = authSession.confirmEmail(currentCode)) {
                EmailVerificationResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            isVerified = true,
                            infoMessage = "Adres e-mail potwierdzony. Konto zostało zarejestrowane."
                        )
                    }
                }

                is EmailVerificationResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = mapVerificationError(result.error)
                        )
                    }
                }
            }
        }
    }

    fun resendCode() {
        if (_uiState.value.isResending) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isResending = true,
                    errorMessage = null,
                    infoMessage = null
                )
            }

            when (val result = authSession.resendVerificationCode()) {
                EmailVerificationResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isResending = false,
                            infoMessage = "Wysłano nowy kod weryfikacyjny."
                        )
                    }
                }

                is EmailVerificationResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isResending = false,
                            errorMessage = mapVerificationError(result.error)
                        )
                    }
                }
            }
        }
    }

    fun leaveScreen() {
        authSession.clearPendingVerificationEmail()
    }

    private fun mapVerificationError(error: EmailVerificationError): String = when (error) {
        EmailVerificationError.INVALID_CODE -> "Nieprawidłowy lub wygasły kod weryfikacyjny."
        EmailVerificationError.RATE_LIMITED -> "Zbyt wiele prób. Odczekaj chwilę i spróbuj ponownie."
        EmailVerificationError.NETWORK -> "Brak połączenia z serwerem."
        EmailVerificationError.VALIDATION -> "Brak adresu e-mail do potwierdzenia."
        EmailVerificationError.UNKNOWN -> "Nie udało się potwierdzić adresu e-mail."
    }
}