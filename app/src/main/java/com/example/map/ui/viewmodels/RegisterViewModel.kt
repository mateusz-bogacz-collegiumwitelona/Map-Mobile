package com.example.map.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.map.domain.model.RegistrationError
import com.example.map.domain.model.RegistrationResult
import com.example.map.domain.session.AuthSession
import com.example.map.domain.validation.BioError
import com.example.map.domain.validation.PasswordError
import com.example.map.domain.validation.ProfileNameError
import com.example.map.domain.validation.RegistrationValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val bio: String = "",
    val isPublic: Boolean = false,
    val firstNameError: String? = null,
    val lastNameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val bioError: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isRegistered: Boolean = false
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authSession: AuthSession
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onFirstNameChange(value: String) = _uiState.update {
        it.copy(firstName = value, firstNameError = null, errorMessage = null)
    }

    fun onLastNameChange(value: String) = _uiState.update {
        it.copy(lastName = value, lastNameError = null, errorMessage = null)
    }

    fun onEmailChange(value: String) = _uiState.update {
        it.copy(email = value, emailError = null, errorMessage = null)
    }

    fun onPasswordChange(value: String) = _uiState.update {
        it.copy(
            password = value,
            passwordError = null,
            confirmPasswordError = null,
            errorMessage = null
        )
    }

    fun onConfirmPasswordChange(value: String) = _uiState.update {
        it.copy(confirmPassword = value, confirmPasswordError = null, errorMessage = null)
    }

    fun onBioChange(value: String) = _uiState.update {
        it.copy(bio = value, bioError = null, errorMessage = null)
    }

    fun onIsPublicChange(value: Boolean) = _uiState.update {
        it.copy(isPublic = value)
    }

    fun register() {
        val state = _uiState.value
        if (state.isLoading) return

        val validation = RegistrationValidator.validate(
            firstName = state.firstName,
            lastName = state.lastName,
            email = state.email,
            password = state.password,
            confirmPassword = state.confirmPassword,
            bio = state.bio
        )

        if (!validation.isValid) {
            _uiState.update {
                it.copy(
                    firstNameError = validation.firstNameError?.toMessage("Imię"),
                    lastNameError = validation.lastNameError?.toMessage("Nazwisko"),
                    emailError = if (!validation.isEmailValid) "Podaj poprawny adres e-mail." else null,
                    passwordError = validation.passwordError?.toMessage(),
                    confirmPasswordError = validation.confirmPasswordError?.toMessage(),
                    bioError = validation.bioError?.toMessage(),
                    errorMessage = null
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (
                val result = authSession.register(
                    firstName = state.firstName,
                    lastName = state.lastName,
                    email = state.email,
                    password = state.password,
                    confirmPassword = state.confirmPassword,
                    bio = state.bio.ifBlank { null },
                    isPublic = state.isPublic
                )
            ) {
                RegistrationResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, isRegistered = true) }
                }

                is RegistrationResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = mapApiErrorToMessage(result.error)
                        )
                    }
                }
            }
        }
    }

    private fun ProfileNameError.toMessage(fieldLabel: String): String = when (this) {
        ProfileNameError.BLANK -> "Pole \"$fieldLabel\" jest wymagane."
        ProfileNameError.TOO_SHORT -> "Pole \"$fieldLabel\" jest za krótkie."
        ProfileNameError.TOO_LONG -> "Pole \"$fieldLabel\" jest za długie."
    }

    private fun PasswordError.toMessage(): String = when (this) {
        PasswordError.BLANK -> "Hasło jest wymagane."
        PasswordError.TOO_SHORT -> "Hasło jest za krótkie."
        PasswordError.WEAK -> "Hasło musi zawierać wielką literę (A-Z), cyfrę oraz znak specjalny."
        PasswordError.MISMATCH -> "Hasła nie są takie same."
    }

    private fun BioError.toMessage(): String = when (this) {
        BioError.TOO_LONG -> "Opis (bio) jest za długi."
    }

    private fun mapApiErrorToMessage(error: RegistrationError): String =
        when (error) {
            RegistrationError.FIRST_NAME_INVALID -> "Nieprawidłowe imię."
            RegistrationError.LAST_NAME_INVALID -> "Nieprawidłowe nazwisko."
            RegistrationError.BIO_INVALID -> "Nieprawidłowy opis (bio)."
            RegistrationError.EMAIL_INVALID -> "Nieprawidłowy adres e-mail."
            RegistrationError.VALIDATION_ERROR -> "Sprawdź poprawność danych."
            RegistrationError.RATE_LIMITED -> "Zbyt wiele prób. Spróbuj ponownie za chwilę."
            RegistrationError.NETWORK -> "Brak połączenia z internetem."
            RegistrationError.UNKNOWN -> "Nie udało się utworzyć konta."
        }
}