package com.example.map

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.map.data.AuthTokens
import com.example.map.data.TokenStorage
import com.example.map.ui.navigation.AppNavigation
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityLogoutTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var tokenStorage: FakeTokenStorage

    @Before
    fun setUp() {
        tokenStorage = FakeTokenStorage(
            initialTokens = AuthTokens(
                accessToken = "access-test-token",
                refreshToken = "refresh-test-token"
            )
        )

        composeRule.setContent {
            AppNavigation(tokenStorage = tokenStorage)
        }
    }

    @Test
    fun logoutClearsTokensShowsLoginAndBackDoesNotReturnToHome() {
        composeRule.onNodeWithText("Jan Kowalski").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Wyloguj").performClick()

        composeRule.onNodeWithText("Zaloguj się").assertIsDisplayed()
        assertNull(tokenStorage.getAccessToken())
        assertNull(tokenStorage.getRefreshToken())

        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Jan Kowalski").assertDoesNotExist()
    }
}

private class FakeTokenStorage(
    initialTokens: AuthTokens? = null
) : TokenStorage {

    private var tokens: AuthTokens? = initialTokens

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