package com.example.map.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.map.domain.model.AuthenticatedAccount
import com.example.map.domain.model.AuthTokens
import com.example.map.domain.repository.AuthRepository
import com.example.map.domain.usecase.AuthSession
import com.example.map.ui.navigation.AppNavigation
import com.example.map.ui.viewmodels.AuthViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityLogoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var tokenStorage: FakeTokenStorage
    private lateinit var authViewModel: AuthViewModel

    private var remotelyLoggedOutAccessToken: String? = null
    private var remotelyLoggedOutRefreshToken: String? = null

    @Before
    fun setUp() {
        remotelyLoggedOutAccessToken = null
        remotelyLoggedOutRefreshToken = null

        tokenStorage = FakeTokenStorage(
            initialTokens = AuthTokens(
                accessToken = "access-test-token",
                refreshToken = "refresh-test-token"
            )
        )

        val authRepository = object : AuthRepository {
            override suspend fun login(
                email: String,
                password: String
            ): AuthenticatedAccount {
                error("Login nie jest używany w tym teście.")
            }

            override suspend fun refresh(
                refreshToken: String
            ): AuthenticatedAccount = AuthenticatedAccount(
                tokens = AuthTokens(
                    accessToken = "access-refreshed",
                    refreshToken = "refresh-refreshed"
                ),
                roles = listOf("User")
            )

            override suspend fun logout(
                accessToken: String,
                refreshToken: String
            ) {
                remotelyLoggedOutAccessToken = accessToken
                remotelyLoggedOutRefreshToken = refreshToken
            }
        }

        authViewModel = AuthViewModel(
            AuthSession(tokenStorage, authRepository)
        )

        composeRule.setContent {
            AppNavigation(viewModel = authViewModel)
        }
    }

    @Test
    fun logoutRevokesBackendSessionClearsTokensAndResetsNavigation() {
        composeRule.onNodeWithText("Jan Kowalski").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Wyloguj").performClick()
        composeRule.onNodeWithText("Zaloguj się").assertIsDisplayed()

        composeRule.runOnIdle {
            assertEquals("access-refreshed", remotelyLoggedOutAccessToken)
            assertEquals("refresh-refreshed", remotelyLoggedOutRefreshToken)
            assertNull(tokenStorage.getAccessToken())
            assertNull(tokenStorage.getRefreshToken())
        }

        Espresso.pressBackUnconditionally()

        val returnedToHome = try {
            composeRule.onAllNodesWithText("Jan Kowalski")
                .fetchSemanticsNodes()
                .isNotEmpty()
        } catch (_: IllegalStateException) {
            false
        }

        assertTrue(!returnedToHome)
    }
}