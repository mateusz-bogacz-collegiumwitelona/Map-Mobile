package com.example.map.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.map.domain.model.AccountDeletionError
import com.example.map.domain.model.AccountDeletionResult
import com.example.map.domain.model.AuthTokens
import com.example.map.domain.usecase.AccountSession
import com.example.map.domain.usecase.AuthSession
import com.example.map.testing.FakeAccountRepository
import com.example.map.testing.FakeProfileStorage
import com.example.map.testing.FakeTokenStorage
import com.example.map.ui.navigation.AppNavigation
import com.example.map.ui.viewmodels.AuthViewModel
import com.example.map.ui.viewmodels.ChangeEmailViewModel
import com.example.map.ui.viewmodels.SettingsViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountDeletionTeardownTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var repository: FakeAccountRepository
    private lateinit var tokenStorage: FakeTokenStorage
    private lateinit var profileStorage: FakeProfileStorage
    private lateinit var accountSession: AccountSession
    private lateinit var authViewModel: AuthViewModel
    private lateinit var settingsViewModel: SettingsViewModel

    @Before
    fun setUp() {
        repository = FakeAccountRepository()
        tokenStorage = FakeTokenStorage(
            AuthTokens(
                accessToken = "access-test-token",
                refreshToken = "refresh-test-token"
            )
        )
        profileStorage = FakeProfileStorage("user@example.com")
        accountSession = AccountSession(repository, profileStorage)
        authViewModel = AuthViewModel(
            authSession = AuthSession(tokenStorage),
            accountSession = accountSession
        )
        settingsViewModel = SettingsViewModel(accountSession)

        composeRule.setContent {
            AppNavigation(
                viewModel = authViewModel,
                settingsViewModelFactory = { settingsViewModel },
                changeEmailViewModelFactory = { ChangeEmailViewModel(accountSession) }
            )
        }
    }

    @Test
    fun confirmingDeletionCallsApiClearsSessionAndResetsNavigationToLogin() {
        composeRule.onNodeWithText("Jan Kowalski").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Ustawienia").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Usuń konto")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule.onNodeWithTag("settings_delete_row").performClick()
        composeRule.onNodeWithText("Usunąć konto?").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_delete_confirm").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Zaloguj się")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithText("Zaloguj się").assertIsDisplayed()
        assertEquals(1, repository.deletionCalls)
        assertNull(tokenStorage.getAccessToken())
        assertNull(tokenStorage.getRefreshToken())
        assertNull(profileStorage.getEmail())
        assertEquals("", accountSession.email.value)

        Espresso.pressBackUnconditionally()

        val returnedToHome = try {
            composeRule.onAllNodesWithText("Jan Kowalski")
                .fetchSemanticsNodes()
                .isNotEmpty()
        } catch (_: IllegalStateException) {
            false
        }

        assertFalse(returnedToHome)
    }

    @Test
    fun failedDeletionKeepsSessionAndStaysOnSettings() {
        repository.deletionResult =
            AccountDeletionResult.Failure(AccountDeletionError.NETWORK)

        composeRule.onNodeWithText("Jan Kowalski").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Ustawienia").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Usuń konto")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule.onNodeWithTag("settings_delete_row").performClick()
        composeRule.onNodeWithTag("settings_delete_confirm").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            settingsViewModel.uiState.value.deleteErrorMessage != null
        }
        composeRule.onNodeWithText("Usunąć konto?").assertIsDisplayed()
        composeRule.onNodeWithText("Brak połączenia z internetem. Spróbuj ponownie.")
            .assertIsDisplayed()
        composeRule.onAllNodesWithText("Zaloguj się").assertCountEquals(0)
        assertTrue(authViewModel.uiState.value.isAuthenticated)
        assertNotNull(tokenStorage.getAccessToken())
        assertNotNull(tokenStorage.getRefreshToken())
        assertEquals("user@example.com", profileStorage.getEmail())
    }
}
