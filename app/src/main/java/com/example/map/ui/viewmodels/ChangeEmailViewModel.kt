package com.example.map.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.map.domain.model.EmailChangeError
import com.example.map.domain.model.EmailChangeResult
import com.example.map.domain.usecase.AccountSession
import com.example.map.domain.validation.EmailValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ChangeEmailStep {
    EMAIL_ENTRY,
    CODE_VERIFICATION,
    SUCCESS
}

data class ChangeEmailUiState(
    val step: ChangeEmailStep = ChangeEmailStep.EMAIL_ENTRY,
    val currentEmail: String = "",
    val newEmail: String = "",
    val code: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
) {
    val normalizedNewEmail: String
        get() = newEmail.trim()

    val emailValidationError: String?
        get() = when {
            newEmail.isBlank() -> null
            !EmailValidator.isValid(newEmail) -> "Podaj prawidłowy adres e-mail."
            normalizedNewEmail.equals(currentEmail.trim(), ignoreCase = true) ->
                "Nowy adres e-mail musi różnić się od obecnego."
            else -> null
        }

    val canSubmitEmail: Boolean
        get() = !isSubmitting &&
            newEmail.isNotBlank() &&
            EmailValidator.isValid(newEmail) &&
            !normalizedNewEmail.equals(currentEmail.trim(), ignoreCase = true)

    val canSubmitCode: Boolean
        get() = !isSubmitting &&
            code.length == CODE_LENGTH &&
            code.all { it.isDigit() }

    private companion object {
        const val CODE_LENGTH = 6
    }
}

@HiltViewModel
class ChangeEmailViewModel @Inject constructor(
    private val accountSession: AccountSession
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ChangeEmailUiState(currentEmail = accountSession.email.value)
    )
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            accountSession.email.collect { email ->
                _uiState.update { it.copy(currentEmail = email) }
            }
        }
    }

    fun onNewEmailChange(value: String) {
        _uiState.update {
            it.copy(
                newEmail = value.take(MAX_EMAIL_LENGTH),
                errorMessage = null
            )
        }
    }

    fun onCodeChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(CODE_LENGTH)
        _uiState.update { it.copy(code = digits, errorMessage = null) }
    }

    fun submitNewEmail() {
        val state = _uiState.value
        if (!state.canSubmitEmail) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

            when (val result = accountSession.requestEmailChange(state.normalizedNewEmail)) {
                is EmailChangeResult.Success -> {
                    _uiState.update {
                        it.copy(
                            step = ChangeEmailStep.CODE_VERIFICATION,
                            isSubmitting = false
                        )
                    }
                }

                is EmailChangeResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = result.toMessage()
                        )
                    }
                }
            }
        }
    }

    fun submitCode() {
        val state = _uiState.value
        if (!state.canSubmitCode) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

            val result = accountSession.confirmEmailChange(
                code = state.code,
                newEmail = state.normalizedNewEmail
            )

            when (result) {
                is EmailChangeResult.Success -> {
                    _uiState.update {
                        it.copy(
                            step = ChangeEmailStep.SUCCESS,
                            isSubmitting = false
                        )
                    }
                }

                is EmailChangeResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = result.toMessage()
                        )
                    }
                }
            }
        }
    }

    fun backToEmailEntry() {
        _uiState.update {
            it.copy(
                step = ChangeEmailStep.EMAIL_ENTRY,
                code = "",
                errorMessage = null
            )
        }
    }

    private fun EmailChangeResult.Failure.toMessage(): String =
        when (error) {
            EmailChangeError.EMAIL_INVALID -> "Podaj prawidłowy adres e-mail."
            EmailChangeError.EMAIL_TAKEN -> "Ten adres e-mail jest już używany."
            EmailChangeError.EMAIL_UNCHANGED ->
                "Nowy adres e-mail musi różnić się od obecnego."
            EmailChangeError.INVALID_CODE -> "Nieprawidłowy kod weryfikacyjny."
            EmailChangeError.CODE_EXPIRED ->
                "Kod weryfikacyjny wygasł. Wróć i wyślij nowy kod."
            EmailChangeError.ATTEMPTS_EXCEEDED ->
                "Przekroczono liczbę prób. Wróć i wyślij nowy kod."
            EmailChangeError.RATE_LIMITED ->
                "Zbyt wiele żądań. Odczekaj chwilę i spróbuj ponownie."
            EmailChangeError.UNAUTHORIZED -> "Sesja wygasła. Zaloguj się ponownie."
            EmailChangeError.NETWORK -> "Brak połączenia z internetem. Spróbuj ponownie."
            EmailChangeError.UNKNOWN ->
                message ?: "Nie udało się zmienić adresu e-mail."
        }

    private companion object {
        const val MAX_EMAIL_LENGTH = 256
        const val CODE_LENGTH = 6
    }
}
