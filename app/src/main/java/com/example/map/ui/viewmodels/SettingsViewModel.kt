package com.example.map.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.example.map.domain.usecase.AccountSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    accountSession: AccountSession
) : ViewModel() {

    val email: StateFlow<String> = accountSession.email
}
