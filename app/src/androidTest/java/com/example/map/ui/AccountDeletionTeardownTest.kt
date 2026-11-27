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
import com.example.map.domain.model.UserProfile
import com.example.map.domain.session.AccountSession
import com.example.map.domain.session.AuthSession
import com.example.map.testing.FakeAccountRepository
import com.example.map.testing.FakeAuthRepository
import com.example.map.testing.FakeProfileStorage
import com.example.map.testing.FakeTokenStorage
import com.example.map.testing.FakeUserProfileRepository
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
import com.example.map.ui.viewmodels.HomeViewModel
import com.example.map.domain.usecase.AccountSession as HomeAccountSession

@RunWith(AndroidJUnit4::class)
class AccountDeletionTeardownTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var repository: FakeAccountRepository
    private lateinit var authRepository: FakeAuthRepository
    private lateinit var tokenStorage: FakeTokenStorage
    private lateinit var profileStorage: FakeProfileStorage
    private lateinit var accountSession: AccountSession
    private lateinit var authViewModel: AuthViewModel
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var homeViewModel: HomeViewModel

    @Before
    fun setUp() {
        repository = FakeAccountRepository()
        authRepository = FakeAuthRepository()
        tokenStorage = FakeTokenStorage(
            AuthTokens(
                accessToken = "access-test-token",
                refreshToken = "refresh-test-token"
            )
        )
        profileStorage = FakeProfileStorage(
            initialProfile = UserProfile(firstName = "Jan", lastName = "Kowalski"),
            initialEmail = "user@example.com"
        )
        accountSession = AccountSession(
            accountRepository = repository,
            profileRepository = FakeUserProfileRepository(),
            profileStorage = profileStorage
        )
        authViewModel = AuthViewModel(
            authSession = AuthSession(tokenStorage, authRepository),
            accountSession = accountSession
        )
        settingsViewModel = SettingsViewModel(accountSession)

        homeViewModel = HomeViewModel(
            HomeAccountSession(
                accountRepository = repository,
                profileStorage = profileStorage
            )
        )

        composeRule.setContent {
            AppNavigation(
                viewModel = authViewModel,
                settingsViewModelFactory = { settingsViewModel },
                changeEmailViewModelFactory = { ChangeEmailViewModel(accountSession) },
                homeViewModelFactory = { homeViewModel }
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
        assertNull(profileStorage.getProfile())
        assertEquals("", accountSession.email.value)
        assertEquals(UserProfile(), accountSession.profile.value)
        assertEquals(0, authRepository.refreshCalls)
        assertEquals(0, authRepository.logoutCalls)

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
