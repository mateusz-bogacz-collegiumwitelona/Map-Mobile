package com.example.map.domain

import com.example.map.domain.model.AuthException
import com.example.map.domain.model.AuthFailure
import com.example.map.domain.model.AuthTokens
import com.example.map.domain.model.AuthenticatedAccount
import com.example.map.domain.repository.AuthRepository
import com.example.map.domain.repository.TokenStorage
import com.example.map.domain.usecase.AuthSession
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Assert.assertFalse

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
        val repository = testRepository(roles = listOf("Admin")) { access, refresh ->
            revokedTokens = AuthTokens(access, refresh)
        }

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
    fun logoutCallsBackendAndClearsLocalTokens() = runBlocking {
        var storedTokens: AuthTokens? = AuthTokens("access-test", "refresh-test")
        var revokedTokens: AuthTokens? = null
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
            }
        )

        AuthSession(storage, repository).logout()

        assertEquals("access-test", revokedTokens?.accessToken)
        assertEquals("refresh-test", revokedTokens?.refreshToken)
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

        val storage = testStorage(
            readTokens = { storedTokens },
            writeTokens = { storedTokens = it }
        )
        val repository = testRepository(roles = listOf("User")) { access, refresh ->
            revokedTokens = AuthTokens(access, refresh)
        }

        AuthSession(storage, repository).logout()

        assertEquals("access-refreshed", revokedTokens?.accessToken)
        assertEquals("refresh-refreshed", revokedTokens?.refreshToken)
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
        }

        assertNull(storedTokens)
    }

    @Test
    fun clearLocalSessionClearsTokensWithoutCallingRepository() = runBlocking {
        var storedTokens: AuthTokens? = AuthTokens("access-test", "refresh-test")
        var repositoryCalls = 0

        val storage = testStorage(
            readTokens = { storedTokens },
            writeTokens = { storedTokens = it }
        )
        val repository = object : AuthRepository {
            override suspend fun login(
                email: String,
                password: String
            ): AuthenticatedAccount = error("Login nie jest używany w tym teście.")

            override suspend fun refresh(
                refreshToken: String
            ): AuthenticatedAccount = error("Refresh nie jest używany w tym teście.")

            override suspend fun logout(accessToken: String, refreshToken: String) {
                repositoryCalls++
            }
        }

        AuthSession(storage, repository).clearLocalSession()

        assertNull(storedTokens)
        assertEquals(0, repositoryCalls)
    }

    private fun testStorage(
        readTokens: () -> AuthTokens?,
        writeTokens: (AuthTokens?) -> Unit
    ): TokenStorage = object : TokenStorage {
                if (logoutCalls.size == 1) {
                    throw AuthException(AuthFailure.INVALID_SESSION)
                }
            },
            onRefresh = { refresh ->
                refreshArgument = refresh
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
        }

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
        onLogout: (String, String) -> Unit = { _, _ -> },
        onRefresh: (String) -> Unit = {}
    ): AuthRepository = object : AuthRepository {
        override suspend fun login(
            email: String,
            password: String
        ) = AuthenticatedAccount(
            tokens = AuthTokens("access-test", "refresh-test"),
            roles = roles
        )

    private fun testRepository(
        roles: List<String>,
        onLogout: (String, String) -> Unit = { _, _ -> }
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
        ): AuthenticatedAccount {
            onRefresh(refreshToken)

            return AuthenticatedAccount(
                tokens = AuthTokens(
                    accessToken = "access-refreshed",
                    refreshToken = "refresh-refreshed"
                ),
                roles = roles
            )
        }

        override suspend fun logout(
            accessToken: String,
            refreshToken: String
        ) {
            onLogout(accessToken, refreshToken)
        }
    }
}