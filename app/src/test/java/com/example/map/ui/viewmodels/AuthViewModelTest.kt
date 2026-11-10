package com.example.map.ui.viewmodels

import com.example.map.domain.model.AuthTokens
import com.example.map.domain.usecase.AccountSession
import com.example.map.domain.usecase.AuthSession
import com.example.map.testing.FakeAccountRepository
import com.example.map.testing.FakeProfileStorage
import com.example.map.testing.FakeTokenStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthViewModelTest {

    @Test
    fun accountDeletionTeardownClearsTokensProfileAndAuthentication() {
        val tokenStorage = FakeTokenStorage(
            AuthTokens(
                accessToken = "access-test-token",
                refreshToken = "refresh-test-token"
            )
        )
        val profileStorage = FakeProfileStorage("user@example.com")
        val accountSession = AccountSession(FakeAccountRepository(), profileStorage)
        val viewModel = AuthViewModel(
            authSession = AuthSession(tokenStorage),
            accountSession = accountSession
        )

        assertTrue(viewModel.uiState.value.isAuthenticated)

        viewModel.onAccountDeleted()

        assertFalse(viewModel.uiState.value.isAuthenticated)
        assertNull(tokenStorage.getAccessToken())
        assertNull(tokenStorage.getRefreshToken())
        assertNull(profileStorage.getEmail())
        assertEquals("", accountSession.email.value)
    }
}
