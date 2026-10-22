package com.example.map.ui

import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.map.ui.screens.HomeScreen
import com.example.map.ui.screens.LoginScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthenticationUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loginPassesRememberMeAsFalseByDefault() {
        var receivedRememberMe: Boolean? = null

        composeRule.setContent {
            LoginScreen(
                onLoginClick = { _, _, rememberMe ->
                    receivedRememberMe = rememberMe
                }
            )
        }

        composeRule.onNodeWithText("Zaloguj się").performClick()

        composeRule.runOnIdle {
            assertEquals(false, receivedRememberMe)
        }
    }

    @Test
    fun loginPassesRememberMeAsTrueWhenCheckboxIsSelected() {
        var receivedRememberMe: Boolean? = null

        composeRule.setContent {
            LoginScreen(
                onLoginClick = { _, _, rememberMe ->
                    receivedRememberMe = rememberMe
                }
            )
        }

        composeRule.onNode(isToggleable()).performClick()
        composeRule.onNodeWithText("Zaloguj się").performClick()

        composeRule.runOnIdle {
            assertEquals(true, receivedRememberMe)
        }
    }

    @Test
    fun homeDisplaysLogoutButtonAndInvokesCallback() {
        var logoutCalled = false

        composeRule.setContent {
            HomeScreen(onLogoutClick = { logoutCalled = true })
        }

        composeRule.onNodeWithContentDescription("Wyloguj").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Wyloguj").performClick()

        composeRule.runOnIdle {
            assertTrue(logoutCalled)
        }
    }
}