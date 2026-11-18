package com.example.map.ui.viewmodels

import com.example.map.domain.model.AuthTokens
import com.example.map.domain.model.UserProfile
import com.example.map.domain.usecase.AccountSession
import com.example.map.domain.usecase.AuthSession
import com.example.map.testing.FakeAccountRepository
import com.example.map.testing.FakeAuthRepository
import com.example.map.testing.FakeProfileStorage
import com.example.map.testing.FakeTokenStorage
import com.example.map.testing.FakeUserProfileRepository
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
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
        val accountSession = AccountSession(
            accountRepository = FakeAccountRepository(),
            profileRepository = FakeUserProfileRepository(),
            profileStorage = profileStorage
        )
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
    fun loginHydratesProfileSoHeaderShowsRealData() = runTest(dispatcher) {
        val profileRepository = FakeUserProfileRepository().apply {
            remoteProfile = UserProfile(firstName = "Anna", lastName = "Nowak")
        }
        val accountSession = AccountSession(
            accountRepository = FakeAccountRepository(),
            profileRepository = profileRepository,
            profileStorage = FakeProfileStorage()
        )
        val viewModel = AuthViewModel(
            authSession = AuthSession(FakeTokenStorage(), FakeAuthRepository()),
            accountSession = accountSession
        )

        viewModel.login("user@example.com", "Password123!")
        advanceUntilIdle()

        assertEquals(1, profileRepository.getProfileCalls)
        assertEquals(
            UserProfile(firstName = "Anna", lastName = "Nowak"),
            viewModel.uiState.value.profile
        )
        assertEquals(UserProfile(firstName = "Anna", lastName = "Nowak"), accountSession.profile.value)
    }

    @Test
    fun profileHydrationFailureDoesNotBlockLogin() = runTest(dispatcher) {
        val profileRepository = FakeUserProfileRepository().apply {
            profileError = IOException("offline")
        }
        val viewModel = AuthViewModel(
            authSession = AuthSession(FakeTokenStorage(), FakeAuthRepository()),
            accountSession = AccountSession(
                accountRepository = FakeAccountRepository(),
                profileRepository = profileRepository,
                profileStorage = FakeProfileStorage()
            )
        )

        viewModel.login("user@example.com", "Password123!")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isAuthenticated)
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(UserProfile(), viewModel.uiState.value.profile)
    }

    @Test
    fun failedLoginDoesNotPersistEmail() = runTest(dispatcher) {
        val profileStorage = FakeProfileStorage()
        val authRepository = FakeAuthRepository().apply {
            loginError = IllegalStateException("network error")
        }
        val viewModel = AuthViewModel(
            authSession = AuthSession(FakeTokenStorage(), authRepository),
            accountSession = AccountSession(
                accountRepository = FakeAccountRepository(),
                profileRepository = FakeUserProfileRepository(),
                profileStorage = profileStorage
            )
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
        val profileStorage = FakeProfileStorage(
            initialProfile = UserProfile(firstName = "Jan", lastName = "Kowalski"),
            initialEmail = "user@example.com"
        )
        val authRepository = FakeAuthRepository()
        val accountSession = AccountSession(
            accountRepository = FakeAccountRepository(),
            profileRepository = FakeUserProfileRepository(),
            profileStorage = profileStorage
        )
        val viewModel = AuthViewModel(
            authSession = AuthSession(tokenStorage, authRepository),
            accountSession = accountSession
        )

        assertTrue(viewModel.uiState.value.isAuthenticated)
        assertEquals("Jan", viewModel.uiState.value.profile.firstName)

        viewModel.onAccountDeleted()

        assertFalse(viewModel.uiState.value.isAuthenticated)
        assertNull(tokenStorage.getAccessToken())
        assertNull(tokenStorage.getRefreshToken())
        assertNull(profileStorage.getEmail())
        assertNull(profileStorage.getProfile())
        assertEquals("", accountSession.email.value)
        assertEquals(UserProfile(), accountSession.profile.value)
        assertEquals(UserProfile(), viewModel.uiState.value.profile)
        assertEquals(0, authRepository.refreshCalls)
        assertEquals(0, authRepository.logoutCalls)
    }

    @Test
    fun logoutClearsProfileAndEmail() = runTest(dispatcher) {
        val profileStorage = FakeProfileStorage(
            initialProfile = UserProfile(firstName = "Jan", lastName = "Kowalski"),
            initialEmail = "user@example.com"
        )
        val tokenStorage = FakeTokenStorage(
            AuthTokens(accessToken = "access-test", refreshToken = "refresh-test")
        )
        val accountSession = AccountSession(
            accountRepository = FakeAccountRepository(),
            profileRepository = FakeUserProfileRepository(),
            profileStorage = profileStorage
        )
        val viewModel = AuthViewModel(
            authSession = AuthSession(tokenStorage, FakeAuthRepository()),
            accountSession = accountSession
        )

        viewModel.logout()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isAuthenticated)
        assertNull(profileStorage.getEmail())
        assertNull(profileStorage.getProfile())
        assertEquals(UserProfile(), viewModel.uiState.value.profile)
    }

    @Test
    fun logoutDuringProfileHydrationDoesNotRestoreClearedProfile() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val profileRepository = FakeUserProfileRepository().apply {
            remoteProfile = UserProfile(firstName = "Anna", lastName = "Nowak")
            getProfileGate = gate
        }
        val profileStorage = FakeProfileStorage(
            initialProfile = UserProfile(firstName = "Jan", lastName = "Kowalski"),
            initialEmail = "user@example.com"
        )
        val tokenStorage = FakeTokenStorage(
            AuthTokens(accessToken = "access-test", refreshToken = "refresh-test")
        )
        val accountSession = AccountSession(
            accountRepository = FakeAccountRepository(),
            profileRepository = profileRepository,
            profileStorage = profileStorage
        )
        val viewModel = AuthViewModel(
            authSession = AuthSession(tokenStorage, FakeAuthRepository()),
            accountSession = accountSession
        )

        viewModel.login("user@example.com", "Password123!")
        advanceUntilIdle()
        assertEquals(1, profileRepository.getProfileCalls)

        viewModel.logout()
        advanceUntilIdle()

        // Spóźniona odpowiedź GET api/user wraca dopiero po wyczyszczeniu sesji.
        gate.complete(Unit)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isAuthenticated)
        assertNull(profileStorage.getProfile())
        assertNull(profileStorage.getEmail())
        assertEquals(UserProfile(), accountSession.profile.value)
        assertEquals(UserProfile(), viewModel.uiState.value.profile)
    }

    @Test
    fun accountDeletionDuringProfileHydrationDoesNotRestoreClearedProfile() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val profileRepository = FakeUserProfileRepository().apply {
            remoteProfile = UserProfile(firstName = "Anna", lastName = "Nowak")
            getProfileGate = gate
        }
        val profileStorage = FakeProfileStorage(
            initialProfile = UserProfile(firstName = "Jan", lastName = "Kowalski"),
            initialEmail = "user@example.com"
        )
        val accountSession = AccountSession(
            accountRepository = FakeAccountRepository(),
            profileRepository = profileRepository,
            profileStorage = profileStorage
        )
        val viewModel = AuthViewModel(
            authSession = AuthSession(
                FakeTokenStorage(
                    AuthTokens(accessToken = "access-test", refreshToken = "refresh-test")
                ),
                FakeAuthRepository()
            ),
            accountSession = accountSession
        )

        viewModel.login("user@example.com", "Password123!")
        advanceUntilIdle()
        assertEquals(1, profileRepository.getProfileCalls)

        viewModel.onAccountDeleted()

        gate.complete(Unit)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isAuthenticated)
        assertNull(profileStorage.getProfile())
        assertNull(profileStorage.getEmail())
        assertEquals(UserProfile(), accountSession.profile.value)
        assertEquals(UserProfile(), viewModel.uiState.value.profile)
    }
}
