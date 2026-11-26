package com.example.map.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.map.domain.model.AuthTokens
import com.example.map.domain.model.UserProfile
import com.example.map.domain.session.AccountSession as SessionAccountSession
import com.example.map.domain.session.AuthSession
import com.example.map.domain.usecase.AccountSession as HomeAccountSession
import com.example.map.testing.FakeAccountRepository
import com.example.map.testing.FakeAuthRepository
import com.example.map.testing.FakeProfileStorage
import com.example.map.testing.FakeTokenStorage
import com.example.map.testing.FakeUserProfileRepository
import com.example.map.ui.navigation.AppNavigation
import com.example.map.ui.viewmodels.AuthViewModel
import com.example.map.ui.viewmodels.HomeViewModel
import com.example.map.ui.viewmodels.ProfilePrivacySettingsViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityLogoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var tokenStorage: FakeTokenStorage
    private lateinit var profileStorage: FakeProfileStorage
    private lateinit var authRepository: FakeAuthRepository
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var authSession: AuthSession
    private lateinit var accountSession: SessionAccountSession
    private lateinit var authViewModel: AuthViewModel
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var profileViewModel: ProfilePrivacySettingsViewModel

    @Before
    fun setUp() {
        tokenStorage = FakeTokenStorage(
            initialTokens = AuthTokens(
                accessToken = "access-test-token",
                refreshToken = "refresh-test-token"
            )
        )
        profileStorage = FakeProfileStorage(
            initialEmail = "user@example.com",
            initialProfile = UserProfile(
                firstName = "Jan",
                lastName = "Kowalski"
            )
        )
        authRepository = FakeAuthRepository()
        accountRepository = FakeAccountRepository()
        authSession = AuthSession(tokenStorage, authRepository)

        accountSession = SessionAccountSession(
            accountRepository = accountRepository,
            profileRepository = FakeUserProfileRepository(),
            profileStorage = profileStorage
        )
        homeViewModel = HomeViewModel(
            HomeAccountSession(
                accountRepository = accountRepository,
                profileStorage = profileStorage
            )
        )
        authViewModel = AuthViewModel(
            authSession = authSession,
            accountSession = accountSession
        )
        profileViewModel = ProfilePrivacySettingsViewModel(
            accountSession = accountSession,
            authSession = authSession
        )

        composeRule.setContent {
            AppNavigation(
                viewModel = authViewModel,
                homeViewModelFactory = { homeViewModel },
                profileViewModelFactory = { profileViewModel }
            )
        }
    }

    @Test
    fun homeShowsProfileLoadedFromSession() {
        composeRule.onNodeWithText("Jan Kowalski").assertIsDisplayed()
    }

    @Test
    fun logoutRevokesBackendSessionClearsLocalProfileAndResetsNavigation() {
        composeRule.onNodeWithText("Jan Kowalski").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Wyloguj").performClick()
        composeRule.onNodeWithText("Zaloguj się").assertIsDisplayed()

        composeRule.runOnIdle {
            assertEquals("access-test-token", authRepository.lastLoggedOutAccessToken)
            assertEquals("refresh-test-token", authRepository.lastLoggedOutRefreshToken)
            assertNull(tokenStorage.getAccessToken())
            assertNull(tokenStorage.getRefreshToken())
            assertNull(profileStorage.getEmail())
            assertNull(profileStorage.getProfile())
            assertEquals("", accountSession.email.value)
            assertEquals(UserProfile(), accountSession.profile.value)
        }

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
    fun profileIconOpensProfilePrivacyScreenAndBackReturnsHome() {
        composeRule.onNodeWithContentDescription("Profil i prywatność").performClick()

        composeRule.onNodeWithText("Dane konta").assertIsDisplayed()
        composeRule.onNodeWithText("user@example.com").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Wróć").performClick()

        composeRule.onNodeWithText("Moje podróże").assertIsDisplayed()
    }
}