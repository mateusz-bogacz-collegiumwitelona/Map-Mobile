package com.example.map.ui.auth

import com.example.map.domain.model.AuthenticatedAccount
import com.example.map.domain.model.AuthTokens
import com.example.map.domain.model.EmailVerificationResult
import com.example.map.domain.model.RegistrationError
import com.example.map.domain.model.RegistrationResult
import com.example.map.domain.repository.AuthRepository
import com.example.map.domain.repository.TokenStorage
import com.example.map.domain.session.AuthSession
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeRegisterAuthRepository
    private lateinit var viewModel: RegisterViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeRegisterAuthRepository()
        val session = AuthSession(
            tokenStorage = FakeTokenStorage(),
            authRepository = repository
        )
        viewModel = RegisterViewModel(session)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `invalid input sets field errors and does not call repository`() = runTest {
        viewModel.onFirstNameChange("Jan")
        viewModel.onLastNameChange("Kowalski")
        viewModel.onEmailChange("jan@example.com")
        viewModel.onPasswordChange("slabe")
        viewModel.onConfirmPasswordChange("inne")

        viewModel.register()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.passwordError)
        assertNotNull(state.confirmPasswordError)
        assertFalse(state.isRegistered)
        assertEquals(0, repository.registerCalls)
    }

    @Test
    fun `editing field clears its previous validation error`() {
        viewModel.register()
        assertNotNull(viewModel.uiState.value.firstNameError)

        viewModel.onFirstNameChange("Jan")
        assertNull(viewModel.uiState.value.firstNameError)
    }

    @Test
    fun `successful registration sets isRegistered and normalizes blank bio to null`() = runTest {
        fillValidForm(bio = "   ")

        viewModel.register()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isRegistered)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(1, repository.registerCalls)
        assertNull(repository.lastBio)
    }

    @Test
    fun `api network error sets friendly errorMessage`() = runTest {
        repository.nextResult = RegistrationResult.Failure(RegistrationError.NETWORK)
        fillValidForm()

        viewModel.register()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isRegistered)
        assertFalse(state.isLoading)
        assertEquals("Brak połączenia z internetem.", state.errorMessage)
    }

    private fun fillValidForm(bio: String = "Opis") {
        viewModel.onFirstNameChange("Jan")
        viewModel.onLastNameChange("Kowalski")
        viewModel.onEmailChange("jan@example.com")
        viewModel.onPasswordChange("Password1!")
        viewModel.onConfirmPasswordChange("Password1!")
        viewModel.onBioChange(bio)
    }

    private class FakeTokenStorage : TokenStorage {
        private var access: String? = null
        private var refresh: String? = null
        override fun saveTokens(tokens: AuthTokens) {
            access = tokens.accessToken
            refresh = tokens.refreshToken
        }
        override fun getAccessToken(): String? = access
        override fun getRefreshToken(): String? = refresh
        override fun clearTokens() {
            access = null
            refresh = null
        }
    }

    private class FakeRegisterAuthRepository : AuthRepository {
        var nextResult: RegistrationResult = RegistrationResult.Success
        var registerCalls = 0
        var lastBio: String? = null

        override suspend fun login(email: String, password: String): AuthenticatedAccount {
            error("Not used in test")
        }

        override suspend fun refresh(refreshToken: String): AuthenticatedAccount {
            error("Not used in test")
        }

        override suspend fun logout(accessToken: String, refreshToken: String) = Unit

        override suspend fun register(
            firstName: String,
            lastName: String,
            email: String,
            password: String,
            confirmPassword: String,
            bio: String?,
            isPublic: Boolean
        ): RegistrationResult {
            registerCalls++
            lastBio = bio
            return nextResult
        }

        override suspend fun confirmEmail(
            email: String,
            code: String
        ): EmailVerificationResult = EmailVerificationResult.Success

        override suspend fun resendVerificationCode(
            email: String
        ): EmailVerificationResult = EmailVerificationResult.Success
    }
}