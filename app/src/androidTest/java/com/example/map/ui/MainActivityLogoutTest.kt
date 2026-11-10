package com.example.map.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.map.domain.model.AccountDeletionResult
import com.example.map.domain.model.AuthTokens
import com.example.map.domain.model.EmailChangeResult
import com.example.map.domain.repository.AccountRepository
import com.example.map.domain.repository.ProfileStorage
import com.example.map.domain.repository.TokenStorage
import com.example.map.domain.usecase.AccountSession
import com.example.map.domain.usecase.AuthSession
import com.example.map.ui.navigation.AppNavigation
import com.example.map.ui.viewmodels.AuthViewModel
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

    @Before
    fun setUp() {
        tokenStorage = FakeTokenStorage(
            initialTokens = AuthTokens(
                accessToken = "access-test-token",
                refreshToken = "refresh-test-token"
            )
        )
        authViewModel = AuthViewModel(
            authSession = AuthSession(tokenStorage),
            accountSession = AccountSession(
                accountRepository = FakeAccountRepository(),
                profileStorage = FakeProfileStorage()
            )
        )

        composeRule.setContent {
            AppNavigation(viewModel = authViewModel)
        }
    }

    @Test
    fun logoutClearsTokensShowsLoginAndBackDoesNotReturnToHome() {
        composeRule.onNodeWithText("Jan Kowalski").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Wyloguj").performClick()

        composeRule.onNodeWithText("Zaloguj się").assertIsDisplayed()
        assertNull(tokenStorage.getAccessToken())
        assertNull(tokenStorage.getRefreshToken())

        Espresso.pressBackUnconditionally()

        val returnedToHome = try {
            composeRule.onAllNodesWithText("Jan Kowalski").fetchSemanticsNodes().isNotEmpty()
        } catch (_: IllegalStateException) {
            false
        }

        assertTrue(!returnedToHome)
    }
}

private class FakeTokenStorage(
    initialTokens: AuthTokens? = null
) : TokenStorage {

    private var tokens: AuthTokens? = initialTokens

    override fun saveTokens(tokens: AuthTokens) {
        this.tokens = tokens
    }

    override fun getAccessToken(): String? = tokens?.accessToken

    override fun getRefreshToken(): String? = tokens?.refreshToken

    override fun clearTokens() {
        tokens = null
    }
}

private class FakeAccountRepository : AccountRepository {
    override suspend fun requestEmailChange(newEmail: String): EmailChangeResult =
        EmailChangeResult.Success

    override suspend fun confirmEmailChange(code: String): EmailChangeResult =
        EmailChangeResult.Success

    override suspend fun deleteAccount(): AccountDeletionResult =
        AccountDeletionResult.Success
}

private class FakeProfileStorage : ProfileStorage {
    private var email: String? = null

    override fun getEmail(): String? = email

    override fun saveEmail(email: String) {
        this.email = email
    }

    override fun clear() {
        email = null
    }
}