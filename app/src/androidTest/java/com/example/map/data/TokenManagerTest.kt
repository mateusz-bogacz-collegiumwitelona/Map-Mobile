package com.example.map.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TokenManagerTest {

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var tokenManager: TokenManager

    @Before
    fun setUp() {
        tokenManager = TokenManager(context)
        tokenManager.clearTokens()
    }

    @After
    fun tearDown() {
        tokenManager.clearTokens()
    }

    @Test
    fun savesTokensAndReadsThemFromANewInstance() {
        val expected = AuthTokens(
            accessToken = "access-test-token",
            refreshToken = "refresh-test-token"
        )

        tokenManager.saveTokens(expected)

        val tokensFromNewInstance = TokenManager(context).getTokens()

        assertEquals(expected, tokensFromNewInstance)
    }

    @Test
    fun savingTokensReplacesThePreviousPair() {
        val first = AuthTokens("access-first", "refresh-first")
        val replacement = AuthTokens("access-second", "refresh-second")

        tokenManager.saveTokens(first)
        tokenManager.saveTokens(replacement)

        assertEquals(replacement, tokenManager.getTokens())
    }

    @Test
    fun clearTokensRemovesTheStoredPair() {
        tokenManager.saveTokens(
            AuthTokens("access-test-token", "refresh-test-token")
        )

        tokenManager.clearTokens()

        assertNull(tokenManager.getTokens())
    }
}