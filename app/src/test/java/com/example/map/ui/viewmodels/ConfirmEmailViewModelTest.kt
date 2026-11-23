package com.example.map.ui.viewmodels

import com.example.map.domain.model.AuthTokens
import com.example.map.domain.model.AuthenticatedAccount
import com.example.map.domain.model.EmailVerificationError
import com.example.map.domain.model.EmailVerificationResult
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
class ConfirmEmailViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeConfirmAuthRepository
    private lateinit var session: AuthSession

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeConfirmAuthRepository()
        session = AuthSession(
            tokenStorage = FakeConfirmTokenStorage(),
            authRepository = repository
        ).apply {
            setPendingVerificationEmail("  jan@example.com  ")
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialStateLoadsPendingEmailFromSession() {
        val viewModel = ConfirmEmailViewModel(session)

        assertEquals("jan@example.com", viewModel.uiState.value.email)
        assertEquals("", viewModel.uiState.value.code)
        assertFalse(viewModel.uiState.value.isVerified)
    }

    @Test
    fun onCodeChangeKeepsOnlyDigitsAndMaxSixChars() {
        val viewModel = ConfirmEmailViewModel(session)

        viewModel.onCodeChange("12a34b56789")

        assertEquals("123456", viewModel.uiState.value.code)
        assertNull(viewModel.uiState.value.codeError)
    }

    @Test
    fun confirmEmailWithShortCodeShowsErrorWithoutCallingRepository() = runTest(dispatcher) {
        val viewModel = ConfirmEmailViewModel(session)
        viewModel.onCodeChange("123")

        viewModel.confirmEmail()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.codeError)
        assertEquals(0, repository.confirmEmailCalls)
        assertFalse(viewModel.uiState.value.isVerified)
    }

    @Test
    fun confirmEmailSuccessMarksVerifiedAndClearsPendingEmail() = runTest(dispatcher) {
        val viewModel = ConfirmEmailViewModel(session)
        viewModel.onCodeChange("654321")

        viewModel.confirmEmail()
        advanceUntilIdle()

        assertEquals("jan@example.com" to "654321", repository.lastConfirmedArgs)
        assertTrue(viewModel.uiState.value.isVerified)
        assertFalse(viewModel.uiState.value.isSubmitting)
        assertNull(session.pendingVerificationEmail.value)
    }

    @Test
    fun confirmEmailFailureShowsErrorAndKeepsPendingEmail() = runTest(dispatcher) {
        repository.confirmResult = EmailVerificationResult.Failure(
            error = EmailVerificationError.INVALID_CODE,
            message = "Kod wygasł."
        )
        val viewModel = ConfirmEmailViewModel(session)
        viewModel.onCodeChange("111111")

        viewModel.confirmEmail()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isVerified)
        assertEquals("Kod wygasł.", viewModel.uiState.value.errorMessage)
        assertEquals("jan@example.com", session.pendingVerificationEmail.value)
    }

    @Test
    fun resendCodeSuccessShowsInfoMessage() = runTest(dispatcher) {
        val viewModel = ConfirmEmailViewModel(session)

        viewModel.resendCode()
        advanceUntilIdle()

        assertEquals("jan@example.com", repository.lastResendEmail)
        assertEquals(1, repository.resendCalls)
        assertNotNull(viewModel.uiState.value.infoMessage)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun leaveScreenClearsPendingVerificationEmail() {
        val viewModel = ConfirmEmailViewModel(session)

        viewModel.leaveScreen()

        assertNull(session.pendingVerificationEmail.value)
    }

    private class FakeConfirmTokenStorage : TokenStorage {
        override fun saveTokens(tokens: AuthTokens) = Unit
        override fun getAccessToken(): String? = null
        override fun getRefreshToken(): String? = null
        override fun clearTokens() = Unit
    }

    private class FakeConfirmAuthRepository : AuthRepository {
        var confirmResult: EmailVerificationResult = EmailVerificationResult.Success
        var resendResult: EmailVerificationResult = EmailVerificationResult.Success
        var confirmEmailCalls = 0
        var resendCalls = 0
        var lastConfirmedArgs: Pair<String, String>? = null
        var lastResendEmail: String? = null

        override suspend fun login(email: String, password: String): AuthenticatedAccount =
            error("Not used")

        override suspend fun refresh(refreshToken: String): AuthenticatedAccount =
            error("Not used")

        override suspend fun logout(accessToken: String, refreshToken: String) = Unit

        override suspend fun register(
            firstName: String,
            lastName: String,
            email: String,
            password: String,
            confirmPassword: String,
            bio: String?,
            isPublic: Boolean
        ): RegistrationResult = RegistrationResult.Success

        override suspend fun confirmEmail(email: String, code: String): EmailVerificationResult {
            confirmEmailCalls++
            lastConfirmedArgs = email to code
            return confirmResult
        }

        override suspend fun resendVerificationCode(email: String): EmailVerificationResult {
            resendCalls++
            lastResendEmail = email
            return resendResult
        }
    }
}