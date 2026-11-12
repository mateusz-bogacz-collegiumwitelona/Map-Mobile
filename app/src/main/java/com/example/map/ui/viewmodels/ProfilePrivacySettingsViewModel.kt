package com.example.map.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.map.domain.model.ProfileUpdateError
import com.example.map.domain.model.ProfileUpdateResult
import com.example.map.domain.model.UserProfile
import com.example.map.domain.usecase.AuthSession
import com.example.map.domain.usecase.ProfileSession
import com.example.map.domain.validation.ProfileNameError
import com.example.map.domain.validation.ProfileNameValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfilePrivacySettingsUiState(
    val email: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val isPublic: Boolean = false,
    val savedFirstName: String = "",
    val savedLastName: String = "",
    val savedIsPublic: Boolean = false,
    val firstNameTouched: Boolean = false,
    val lastNameTouched: Boolean = false,
    val submitAttempted: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    private val firstNameValidation: ProfileNameError?
        get() = ProfileNameValidator.validate(firstName)

    private val lastNameValidation: ProfileNameError?
        get() = ProfileNameValidator.validate(lastName)

    val firstNameError: String?
        get() = if (firstNameTouched || submitAttempted) {
            nameErrorMessage("Imię", firstNameValidation)
        } else {
            null
        }

    val lastNameError: String?
        get() = if (lastNameTouched || submitAttempted) {
            nameErrorMessage("Nazwisko", lastNameValidation)
        } else {
            null
        }

    val isValid: Boolean
        get() = firstNameValidation == null && lastNameValidation == null

    val hasChanges: Boolean
        get() = firstName.trim() != savedFirstName ||
            lastName.trim() != savedLastName ||
            isPublic != savedIsPublic

    val canSave: Boolean
        get() = !isSaving && isValid && hasChanges
}

@HiltViewModel
class ProfilePrivacySettingsViewModel @Inject constructor(
    private val profileSession: ProfileSession,
    private val authSession: AuthSession
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        profileSession.profile.value.toUiState()
    )
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            profileSession.profile.collect { profile ->
                _uiState.update { state -> state.syncWith(profile) }
            }
        }

        if (authSession.hasSavedSession()) {
            viewModelScope.launch {
                profileSession.refresh()
            }
        }
    }

    fun onFirstNameChange(value: String) {
        _uiState.update {
            it.copy(
                firstName = value,
                firstNameTouched = true,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun onLastNameChange(value: String) {
        _uiState.update {
            it.copy(
                lastName = value,
                lastNameTouched = true,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun onVisibilityChange(isPublic: Boolean) {
        _uiState.update {
            it.copy(
                isPublic = isPublic,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun save() {
        if (_uiState.value.isSaving) return

        _uiState.update {
            it.copy(
                submitAttempted = true,
                errorMessage = null,
                successMessage = null
            )
        }

        val state = _uiState.value
        if (!state.isValid || !state.hasChanges) return

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val firstName = state.firstName.trim()
            val lastName = state.lastName.trim()
            val namesChanged = firstName != state.savedFirstName ||
                lastName != state.savedLastName
            val visibilityChanged = state.isPublic != state.savedIsPublic

            if (namesChanged) {
                when (val result = profileSession.updateNames(firstName, lastName)) {
                    is ProfileUpdateResult.Failure -> {
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                errorMessage = result.toMessage(
                                    "Nie udało się zapisać danych profilu."
                                )
                            )
                        }
                        return@launch
                    }

                    ProfileUpdateResult.Success -> Unit
                }
            }

            if (visibilityChanged) {
                when (val result = profileSession.updateVisibility(state.isPublic)) {
                    is ProfileUpdateResult.Failure -> {
                        val message = result.toMessage(
                            "Nie udało się zmienić widoczności profilu."
                        )
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                errorMessage = if (namesChanged) {
                                    "Dane zostały zapisane. $message"
                                } else {
                                    message
                                }
                            )
                        }
                        return@launch
                    }

                    ProfileUpdateResult.Success -> Unit
                }
            }

            _uiState.update {
                it.copy(
                    firstName = it.firstName.trim(),
                    lastName = it.lastName.trim(),
                    isSaving = false,
                    successMessage = "Zapisano zmiany."
                )
            }
        }
    }

    private fun ProfileUpdateError.toMessage(fallback: String, message: String?): String =
        when (this) {
            ProfileUpdateError.FIRST_NAME_INVALID -> "Imię musi mieć od 2 do 50 znaków."
            ProfileUpdateError.LAST_NAME_INVALID -> "Nazwisko musi mieć od 2 do 50 znaków."
            ProfileUpdateError.UNAUTHORIZED -> "Sesja wygasła. Zaloguj się ponownie."
            ProfileUpdateError.NOT_FOUND -> "Konto nie istnieje lub zostało usunięte."
            ProfileUpdateError.RATE_LIMITED ->
                "Zbyt wiele żądań. Odczekaj chwilę i spróbuj ponownie."
            ProfileUpdateError.NETWORK -> "Brak połączenia z internetem. Spróbuj ponownie."
            ProfileUpdateError.UNKNOWN -> message ?: fallback
        }

    private fun ProfileUpdateResult.Failure.toMessage(fallback: String): String =
        error.toMessage(fallback, message)
}

private fun UserProfile.toUiState(): ProfilePrivacySettingsUiState =
    ProfilePrivacySettingsUiState(
        email = email,
        firstName = firstName,
        lastName = lastName,
        isPublic = isPublic,
        savedFirstName = firstName,
        savedLastName = lastName,
        savedIsPublic = isPublic
    )

private fun ProfilePrivacySettingsUiState.syncWith(profile: UserProfile): ProfilePrivacySettingsUiState {
    val firstNameDirty = firstName.trim() != savedFirstName
    val lastNameDirty = lastName.trim() != savedLastName
    val visibilityDirty = isPublic != savedIsPublic

    return copy(
        email = profile.email,
        firstName = if (firstNameDirty) firstName else profile.firstName,
        lastName = if (lastNameDirty) lastName else profile.lastName,
        isPublic = if (visibilityDirty) isPublic else profile.isPublic,
        savedFirstName = profile.firstName,
        savedLastName = profile.lastName,
        savedIsPublic = profile.isPublic
    )
}

private fun nameErrorMessage(field: String, error: ProfileNameError?): String? =
    when (error) {
        ProfileNameError.BLANK -> "$field jest wymagane."
        ProfileNameError.TOO_SHORT ->
            "$field musi mieć co najmniej ${ProfileNameValidator.MIN_LENGTH} znaki."
        ProfileNameError.TOO_LONG ->
            "$field może mieć maksymalnie ${ProfileNameValidator.MAX_LENGTH} znaków."
        null -> null
    }
