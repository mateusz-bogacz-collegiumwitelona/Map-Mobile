package com.example.map.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.map.domain.model.AccountDeletionError
import com.example.map.domain.model.AccountDeletionResult
import com.example.map.domain.usecase.AccountSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val email: String = "",
    val isDeleteDialogVisible: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val deleteErrorMessage: String? = null,
    val isAccountDeleted: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val accountSession: AccountSession
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(email = accountSession.email.value)
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            accountSession.email.collect { email ->
                _uiState.update { it.copy(email = email) }
            }
        }
    }

    fun showDeleteDialog() {
        _uiState.update {
            it.copy(
                isDeleteDialogVisible = true,
                deleteErrorMessage = null
            )
        }
    }

    fun dismissDeleteDialog() {
        if (_uiState.value.isDeletingAccount) return

        _uiState.update {
            it.copy(
                isDeleteDialogVisible = false,
                deleteErrorMessage = null
            )
        }
    }

    fun deleteAccount() {
        if (_uiState.value.isDeletingAccount) return

        _uiState.update {
            it.copy(
                isDeletingAccount = true,
                deleteErrorMessage = null
            )
        }

        viewModelScope.launch {
            when (val result = accountSession.deleteAccount()) {
                is AccountDeletionResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isDeletingAccount = false,
                            isDeleteDialogVisible = false,
                            isAccountDeleted = true
                        )
                    }
                }

                is AccountDeletionResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isDeletingAccount = false,
                            deleteErrorMessage = result.toMessage()
                        )
                    }
                }
            }
        }
    }

    private fun AccountDeletionResult.Failure.toMessage(): String =
        when (error) {
            AccountDeletionError.UNAUTHORIZED -> "Sesja wygasła. Zaloguj się ponownie."
            AccountDeletionError.NOT_FOUND -> "Konto nie istnieje lub zostało już usunięte."
            AccountDeletionError.RATE_LIMITED ->
                "Zbyt wiele żądań. Odczekaj chwilę i spróbuj ponownie."
            AccountDeletionError.NETWORK -> "Brak połączenia z internetem. Spróbuj ponownie."
            AccountDeletionError.UNKNOWN ->
                message ?: "Nie udało się usunąć konta. Spróbuj ponownie."
        }
}
