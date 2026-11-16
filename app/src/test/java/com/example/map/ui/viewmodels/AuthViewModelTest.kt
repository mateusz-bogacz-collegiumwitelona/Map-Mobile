package com.example.map.ui.viewmodels

import com.example.map.domain.model.AuthTokens
import com.example.map.domain.usecase.AccountSession
import com.example.map.domain.usecase.AuthSession
import com.example.map.testing.FakeAccountRepository
import com.example.map.testing.FakeAuthRepository
import com.example.map.testing.FakeProfileStorage
import com.example.map.testing.FakeTokenStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loginPersistsEmailAfterSuccessfulAuthentication() = runTest(dispatcher) {
        val tokenStorage = FakeTokenStorage()
        val profileStorage = FakeProfileStorage()
        val authRepository = FakeAuthRepository()
        val accountSession = AccountSession(FakeAccountRepository(), profileStorage)
        val viewModel = AuthViewModel(
            authSession = AuthSession(tokenStorage, authRepository),
            accountSession = accountSession
        )

        viewModel.login(" user@example.com ", "Password123!")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isAuthenticated)
        assertEquals("access-test", tokenStorage.getAccessToken())
        assertEquals("refresh-test", tokenStorage.getRefreshToken())
        assertEquals("user@example.com", profileStorage.getEmail())
        assertEquals("user@example.com", accountSession.email.value)
    }

    @Test
    fun failedLoginDoesNotPersistEmail() = runTest(dispatcher) {
        val profileStorage = FakeProfileStorage()
        val authRepository = FakeAuthRepository().apply {
            loginError = IllegalStateException("network error")
        }
        val viewModel = AuthViewModel(
            authSession = AuthSession(FakeTokenStorage(), authRepository),
            accountSession = AccountSession(FakeAccountRepository(), profileStorage)
        )

        viewModel.login("user@example.com", "Password123!")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isAuthenticated)
        assertNull(profileStorage.getEmail())
    }

    @Test
    fun accountDeletionTeardownClearsLocalStateWithoutNetworkCalls() = runTest(dispatcher) {
        val tokenStorage = FakeTokenStorage(
            AuthTokens(
                accessToken = "access-test-token",
                refreshToken = "refresh-test-token"
            )
        )
        val profileStorage = FakeProfileStorage("user@example.com")
        val authRepository = FakeAuthRepository()
        val accountSession = AccountSession(FakeAccountRepository(), profileStorage)
        val viewModel = AuthViewModel(
            authSession = AuthSession(tokenStorage, authRepository),
            accountSession = accountSession
        )

        assertTrue(viewModel.uiState.value.isAuthenticated)

        viewModel.onAccountDeleted()

        assertFalse(viewModel.uiState.value.isAuthenticated)
        assertNull(tokenStorage.getAccessToken())
        assertNull(tokenStorage.getRefreshToken())
        assertNull(profileStorage.getEmail())
        assertEquals("", accountSession.email.value)
        assertEquals(0, authRepository.refreshCalls)
        assertEquals(0, authRepository.logoutCalls)
    }
}
