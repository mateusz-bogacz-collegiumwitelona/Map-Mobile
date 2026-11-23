package com.example.map.domain

import com.example.map.domain.model.AuthException
import com.example.map.domain.model.AuthFailure
import com.example.map.domain.model.AuthTokens
import com.example.map.domain.model.AuthenticatedAccount
import com.example.map.domain.repository.AuthRepository
import com.example.map.domain.repository.TokenStorage
import com.example.map.domain.session.AuthSession
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class AuthSessionTest {

    @Test
    fun userLoginStoresBothTokens() = runBlocking {
        var storedTokens: AuthTokens? = null
        val storage = testStorage(
            readTokens = { storedTokens },
            writeTokens = { storedTokens = it }
        )
        val repository = testRepository(roles = listOf("User"))

        AuthSession(storage, repository).login("user@example.com", "password")

        assertEquals("access-test", storedTokens?.accessToken)
        assertEquals("refresh-test", storedTokens?.refreshToken)
    }

    @Test
    fun adminLoginIsRejectedAndReturnedTokensAreRevoked() = runBlocking {
        var storedTokens: AuthTokens? = null
        var revokedTokens: AuthTokens? = null

        val storage = testStorage(
            readTokens = { storedTokens },
            writeTokens = { storedTokens = it }
        )
        val repository = testRepository(
            roles = listOf("Admin"),
            onLogout = { access, refresh ->
                revokedTokens = AuthTokens(access, refresh)
            }
        )

        val error = try {
            AuthSession(storage, repository).login("admin@example.com", "password")
            null
        } catch (caught: AuthException) {
            caught
        }

        assertEquals(AuthFailure.ROLE_NOT_ALLOWED, error?.failure)
        assertNull(storedTokens)
        assertEquals("access-test", revokedTokens?.accessToken)
        assertEquals("refresh-test", revokedTokens?.refreshToken)
    }

    @Test
    fun accountWithUserAndAdminRolesIsRejected() = runBlocking {
        var storedTokens: AuthTokens? = null
        val storage = testStorage(
            readTokens = { storedTokens },
            writeTokens = { storedTokens = it }
        )
        val repository = testRepository(roles = listOf("User", "Admin"))

        val error = try {
            AuthSession(storage, repository).login("admin@example.com", "password")
            null
        } catch (caught: AuthException) {
            caught
        }

        assertEquals(AuthFailure.ROLE_NOT_ALLOWED, error?.failure)
        assertNull(storedTokens)
    }

    @Test
    fun logoutUsesCurrentTokensWithoutRefreshingAndClearsLocalTokens() = runBlocking {
        var storedTokens: AuthTokens? = AuthTokens("access-test", "refresh-test")
        var revokedTokens: AuthTokens? = null
        var refreshCalled = false

        val storage = testStorage(
            readTokens = { storedTokens },
            writeTokens = { storedTokens = it }
        )
        val repository = testRepository(
            roles = listOf("User"),
            onLogout = { access, refresh ->
                revokedTokens = AuthTokens(access, refresh)
            },
            onRefresh = {
                refreshCalled = true
                AuthenticatedAccount(
                    tokens = AuthTokens("access-refreshed", "refresh-refreshed"),
                    roles = listOf("User")
                )
            }
        )

        AuthSession(storage, repository).logout()

        assertEquals(AuthTokens("access-test", "refresh-test"), revokedTokens)
        assertFalse(refreshCalled)
        assertNull(storedTokens)
    }

    @Test
    fun logoutRefreshesAndRetriesWhenCurrentAccessTokenIsInvalid() = runBlocking {
        var storedTokens: AuthTokens? = AuthTokens("access-test", "refresh-test")
        var refreshArgument: String? = null
        val logoutCalls = mutableListOf<AuthTokens>()

        val storage = testStorage(
            readTokens = { storedTokens },
            writeTokens = { storedTokens = it }
        )
        val repository = testRepository(
            roles = listOf("User"),
            onLogout = { access, refresh ->
                logoutCalls += AuthTokens(access, refresh)
                if (logoutCalls.size == 1) {
                    throw AuthException(AuthFailure.INVALID_SESSION)
                }
            },
            onRefresh = { refresh ->
                refreshArgument = refresh
                AuthenticatedAccount(
                    tokens = AuthTokens("access-refreshed", "refresh-refreshed"),
                    roles = listOf("User")
                )
            }
        )

        AuthSession(storage, repository).logout()

        assertEquals("refresh-test", refreshArgument)
        assertEquals(
            listOf(
                AuthTokens("access-test", "refresh-test"),
                AuthTokens("access-refreshed", "refresh-refreshed")
            ),
            logoutCalls
        )
        assertNull(storedTokens)
    }

    @Test
    fun logoutClearsLocalTokensEvenWhenBackendCallFails() = runBlocking {
        var storedTokens: AuthTokens? = AuthTokens("access-test", "refresh-test")
        val storage = testStorage(
            readTokens = { storedTokens },
            writeTokens = { storedTokens = it }
        )
        val repository = testRepository(
            roles = listOf("User"),
            onLogout = { _, _ -> throw IllegalStateException("network error") }
        )

        try {
            AuthSession(storage, repository).logout()
        } catch (_: IllegalStateException) {
            // Lokalna sesja musi zostać usunięta mimo błędu backendu.
        }

        assertNull(storedTokens)
    }

    @Test
    fun clearLocalSessionClearsTokens() {
        var storedTokens: AuthTokens? = AuthTokens("access-test", "refresh-test")
        val storage = testStorage(
            readTokens = { storedTokens },
            writeTokens = { storedTokens = it }
        )

        AuthSession(storage, testRepository(listOf("User"))).clearLocalSession()

        assertNull(storedTokens)
    }

    private fun testStorage(
        readTokens: () -> AuthTokens?,
        writeTokens: (AuthTokens?) -> Unit
    ): TokenStorage = object : TokenStorage {
        override fun saveTokens(tokens: AuthTokens) {
            writeTokens(tokens)
        }

        override fun getAccessToken(): String? =
            readTokens()?.accessToken

        override fun getRefreshToken(): String? =
            readTokens()?.refreshToken

        override fun clearTokens() {
            writeTokens(null)
        }
    }

    private fun testRepository(
        roles: List<String>,
        onLogout: suspend (String, String) -> Unit = { _, _ -> },
        onRefresh: suspend (String) -> AuthenticatedAccount = {
            AuthenticatedAccount(
                tokens = AuthTokens("access-refreshed", "refresh-refreshed"),
                roles = roles
            )
        }
    ): AuthRepository = object : AuthRepository {
        override suspend fun login(
            email: String,
            password: String
        ) = AuthenticatedAccount(
            tokens = AuthTokens("access-test", "refresh-test"),
            roles = roles
        )

        override suspend fun refresh(
            refreshToken: String
        ): AuthenticatedAccount = onRefresh(refreshToken)

        override suspend fun logout(
            accessToken: String,
            refreshToken: String
        ) {
            onLogout(accessToken, refreshToken)
        }
    }
}