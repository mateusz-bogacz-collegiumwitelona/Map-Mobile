package com.example.map.domain.session

import com.example.map.domain.model.auth.AuthException
import com.example.map.domain.model.auth.AuthFailure
import com.example.map.domain.model.auth.AuthTokens
import com.example.map.domain.model.auth.AuthenticatedAccount
import com.example.map.domain.model.account.EmailVerificationError
import com.example.map.domain.model.account.EmailVerificationResult
import com.example.map.domain.model.auth.RegistrationError
import com.example.map.domain.model.auth.RegistrationResult
import com.example.map.testing.FakeAuthRepository
import com.example.map.testing.FakeTokenStorage
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class AuthSessionTest {

    private fun createSession(
        repository: FakeAuthRepository = FakeAuthRepository(),
        storage: FakeTokenStorage = FakeTokenStorage()
    ): Triple<AuthSession, FakeAuthRepository, FakeTokenStorage> =
        Triple(
            AuthSession(tokenStorage = storage, authRepository = repository),
            repository,
            storage
        )

    @Test
    fun loginWithUserRolePersistsTokens() = runTest {
        val (session, _, storage) = createSession()

        session.login("jan@example.com", "Password1!")

        assertEquals("access-token", storage.getAccessToken())
        assertEquals("refresh-token", storage.getRefreshToken())
        assertTrue(session.hasSavedSession())
    }

    @Test
    fun loginWithDisallowedRoleRevokesTokensAndThrows() = runTest {
        val repo = FakeAuthRepository().apply {
            loginAccount = AuthenticatedAccount(
                tokens = AuthTokens("admin-access", "admin-refresh"),
                roles = listOf("ADMIN")
            )
        }
        val (session, _, storage) = createSession(repository = repo)

        try {
            session.login("admin@example.com", "Password1!")
            fail("Oczekiwano AuthException(ROLE_NOT_ALLOWED)")
        } catch (ex: AuthException) {
            assertEquals(AuthFailure.ROLE_NOT_ALLOWED, ex.failure)
        }

        assertEquals("admin-access" to "admin-refresh", repo.lastLogoutTokens)
        assertNull(storage.getAccessToken())
        assertNull(storage.getRefreshToken())
        assertFalse(session.hasSavedSession())
    }

    @Test
    fun logoutClearsLocalTokensEvenWhenRemoteFails() = runTest {
        val storage = FakeTokenStorage().apply {
            saveTokens(AuthTokens("access-1", "refresh-1"))
        }
        val repo = FakeAuthRepository().apply {
            logoutError = AuthException(AuthFailure.SERVER_ERROR)
        }
        val (session, _, _) = createSession(repository = repo, storage = storage)

        try {
            session.logout()
            fail("Oczekiwano AuthException(SERVER_ERROR)")
        } catch (ex: AuthException) {
            assertEquals(AuthFailure.SERVER_ERROR, ex.failure)
        }

        assertNull(storage.getAccessToken())
        assertNull(storage.getRefreshToken())
        assertFalse(session.hasSavedSession())
    }

    @Test
    fun logoutRefreshesSessionAndRetriesOnInvalidSession() = runTest {
        val storage = FakeTokenStorage().apply {
            saveTokens(AuthTokens("expired-access", "valid-refresh"))
        }
        val repo = FakeAuthRepository().apply {
            failFirstLogoutWithInvalidSession = true
            refreshedAccount = AuthenticatedAccount(
                tokens = AuthTokens("new-access", "new-refresh"),
                roles = listOf("USER")
            )
        }
        val (session, _, _) = createSession(repository = repo, storage = storage)

        session.logout()

        assertEquals(2, repo.logoutCalls)
        assertEquals("new-access" to "new-refresh", repo.lastLogoutTokens)
        assertNull(storage.getAccessToken())
        assertNull(storage.getRefreshToken())
    }

    @Test
    fun clearLocalSessionRemovesSavedTokens() {
        val storage = FakeTokenStorage().apply {
            saveTokens(AuthTokens("access-1", "refresh-1"))
        }
        val (session, _, _) = createSession(storage = storage)

        session.clearLocalSession()

        assertFalse(session.hasSavedSession())
        assertNull(storage.getAccessToken())
        assertNull(storage.getRefreshToken())
    }

    @Test
    fun successfulRegisterNormalizesAndStoresPendingEmailInMemory() = runTest {
        val (session, repo, _) = createSession()

        val result = session.register(
            firstName = "  Jan ",
            lastName = " Kowalski  ",
            email = "  jan@example.com  ",
            password = "Password1!",
            confirmPassword = "Password1!",
            bio = "  Opis  ",
            isPublic = true
        )

        assertEquals(RegistrationResult.Success, result)
        assertEquals("Jan", repo.lastRegisteredFirstName)
        assertEquals("Kowalski", repo.lastRegisteredLastName)
        assertEquals("jan@example.com", repo.lastRegisteredEmail)
        assertEquals("Opis", repo.lastRegisteredBio)
        assertEquals("jan@example.com", session.pendingVerificationEmail.value)
    }

    @Test
    fun failedRegisterDoesNotStorePendingEmail() = runTest {
        val repo = FakeAuthRepository().apply {
            registerResult = RegistrationResult.Failure(RegistrationError.NETWORK)
        }
        val (session, _, _) = createSession(repository = repo)

        val result = session.register(
            firstName = "Jan",
            lastName = "Kowalski",
            email = "jan@example.com",
            password = "Password1!",
            confirmPassword = "Password1!",
            bio = null,
            isPublic = false
        )

        assertEquals(RegistrationResult.Failure(RegistrationError.NETWORK), result)
        assertNull(session.pendingVerificationEmail.value)
    }

    @Test
    fun setPendingVerificationEmailNormalizesAndIgnoresBlank() {
        val (session, _, _) = createSession()

        session.setPendingVerificationEmail("   ")
        assertNull(session.pendingVerificationEmail.value)

        session.setPendingVerificationEmail("  jan@example.com  ")
        assertEquals("jan@example.com", session.pendingVerificationEmail.value)
    }

    @Test
    fun clearPendingVerificationEmailResetsState() {
        val (session, _, _) = createSession()
        session.setPendingVerificationEmail("jan@example.com")

        session.clearPendingVerificationEmail()

        assertNull(session.pendingVerificationEmail.value)
    }

    @Test
    fun confirmEmailWithoutPendingEmailReturnsValidationError() = runTest {
        val (session, repo, _) = createSession()

        val result = session.confirmEmail("123456")

        assertEquals(
            EmailVerificationResult.Failure(
                error = EmailVerificationError.VALIDATION
            ),
            result
        )
        assertEquals(0, repo.confirmEmailCalls)
    }

    @Test
    fun successfulConfirmEmailTrimsCodeAndClearsPendingEmail() = runTest {
        val (session, repo, _) = createSession()
        session.setPendingVerificationEmail("  jan@example.com ")

        val result = session.confirmEmail("  654321  ")

        assertEquals(EmailVerificationResult.Success, result)
        assertEquals("jan@example.com", repo.lastConfirmedEmail)
        assertEquals("654321", repo.lastConfirmedCode)
        assertEquals(1, repo.confirmEmailCalls)
        assertNull(session.pendingVerificationEmail.value)
    }

    @Test
    fun failedConfirmEmailKeepsPendingEmailInMemory() = runTest {
        val failure = EmailVerificationResult.Failure(
            error = EmailVerificationError.INVALID_CODE,
            message = "Nieprawidłowy lub wygasły kod weryfikacyjny."
        )
        val repo = FakeAuthRepository().apply {
            confirmEmailResult = failure
        }
        val (session, _, _) = createSession(repository = repo)
        session.setPendingVerificationEmail("jan@example.com")

        val result = session.confirmEmail("000000")

        assertEquals(failure, result)
        assertEquals("jan@example.com", session.pendingVerificationEmail.value)
    }

    @Test
    fun resendVerificationCodeWithoutPendingEmailReturnsValidationError() = runTest {
        val (session, repo, _) = createSession()

        val result = session.resendVerificationCode()

        assertEquals(
            EmailVerificationResult.Failure(
                error = EmailVerificationError.VALIDATION
            ),
            result
        )
        assertEquals(0, repo.resendVerificationCalls)
    }

    @Test
    fun resendVerificationCodeSendsForPendingEmail() = runTest {
        val (session, repo, _) = createSession()
        session.setPendingVerificationEmail("  jan@example.com ")

        val result = session.resendVerificationCode()

        assertEquals(EmailVerificationResult.Success, result)
        assertEquals("jan@example.com", repo.lastResendVerificationEmail)
        assertEquals(1, repo.resendVerificationCalls)
        assertEquals("jan@example.com", session.pendingVerificationEmail.value)
    }
}