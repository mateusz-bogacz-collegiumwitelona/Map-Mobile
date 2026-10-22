package com.example.map

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.map.data.AuthTokens
import com.example.map.data.TokenManager
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityLogoutTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private lateinit var tokenManager: TokenManager
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        tokenManager = TokenManager(context)
        tokenManager.clearTokens()
        tokenManager.saveTokens(
            AuthTokens(
                accessToken = "access-test-token",
                refreshToken = "refresh-test-token"
            )
        )

        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        scenario.close()
        tokenManager.clearTokens()
    }

    @Test
    fun logoutClearsTokensShowsLoginAndBackDoesNotReturnToHome() {
        composeRule.onNodeWithText("Jan Kowalski").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Wyloguj").performClick()

        composeRule.onNodeWithText("Zaloguj się").assertIsDisplayed()
        assertNull(tokenManager.getAccessToken())
        assertNull(tokenManager.getRefreshToken())

        scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Jan Kowalski").assertDoesNotExist()
    }
}