package com.example.map.domain

import com.example.map.data.AuthTokens
import com.example.map.data.TokenStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthSessionTest {

    private class FakeTokenStorage : TokenStorage {
        private var tokens: AuthTokens? = null

        override fun saveTokens(tokens: AuthTokens) {
            this.tokens = tokens
        }

        override fun getAccessToken(): String? =
            tokens?.accessToken

        override fun getRefreshToken(): String? =
            tokens?.refreshToken

        override fun clearTokens() {
            tokens = null
        }
    }

    @Test
    fun savesTokensWhenRememberMeIsEnabled() {
        val storage = FakeTokenStorage()
        val session = AuthSession(storage)
        val tokens = AuthTokens("access-test", "refresh-test")

        session.saveAfterLogin(tokens, rememberMe = true)

        assertEquals("access-test", storage.getAccessToken())
        assertEquals("refresh-test", storage.getRefreshToken())
        assertTrue(session.hasSavedSession())
    }

    @Test
    fun doesNotKeepTokensWhenRememberMeIsDisabled() {
        val storage = FakeTokenStorage()
        val session = AuthSession(storage)

        storage.saveTokens(AuthTokens("old-access", "old-refresh"))
        session.saveAfterLogin(
            AuthTokens("new-access", "new-refresh"),
            rememberMe = false
        )

        assertNull(storage.getAccessToken())
        assertNull(storage.getRefreshToken())
        assertEquals(false, session.hasSavedSession())
    }

    @Test
    fun logoutClearsSavedTokens() {
        val storage = FakeTokenStorage()
        val session = AuthSession(storage)

        storage.saveTokens(AuthTokens("access-test", "refresh-test"))
        session.logout()

        assertNull(storage.getAccessToken())
        assertNull(storage.getRefreshToken())
    }
}