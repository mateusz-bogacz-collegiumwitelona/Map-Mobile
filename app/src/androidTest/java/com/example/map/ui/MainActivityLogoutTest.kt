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
import com.example.map.domain.model.AuthenticatedAccount
import com.example.map.domain.model.RegistrationResult
import com.example.map.domain.model.UserProfile
import com.example.map.domain.repository.AuthRepository
import com.example.map.domain.session.AccountSession
import com.example.map.domain.session.AuthSession
import com.example.map.testing.FakeAccountRepository
import com.example.map.testing.FakeProfileStorage
import com.example.map.testing.FakeUserProfileRepository
import com.example.map.ui.navigation.AppNavigation
import com.example.map.ui.viewmodels.AuthViewModel
import com.example.map.ui.viewmodels.ProfilePrivacySettingsViewModel
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
    private lateinit var profileStorage: FakeProfileStorage
    private lateinit var accountSession: AccountSession
    private lateinit var authSession: AuthSession

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
        profileStorage = FakeProfileStorage(
            initialProfile = UserProfile(firstName = "Jan", lastName = "Kowalski"),
            initialEmail = "user@example.com"
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
            ): AuthenticatedAccount {
                error("Refresh nie powinien być wywołany po udanym logout.")
            }

            override suspend fun logout(
                accessToken: String,
                refreshToken: String
            ) {
                remotelyLoggedOutAccessToken = accessToken
                remotelyLoggedOutRefreshToken = refreshToken
            }

            override suspend fun register(
                firstName: String,
                lastName: String,
                email: String,
                password: String,
                confirmPassword: String,
                bio: String?,
                isPublic: Boolean
            ): RegistrationResult {
                error("Rejestracja nie jest używana w tym teście.")
            }
        }

        authSession = AuthSession(tokenStorage, authRepository)
        accountSession = AccountSession(
            accountRepository = FakeAccountRepository(),
            profileRepository = FakeUserProfileRepository(),
            profileStorage = profileStorage
        )

        val authViewModel = AuthViewModel(
            authSession = authSession,
            accountSession = accountSession
        )
        val profileViewModel = ProfilePrivacySettingsViewModel(
            accountSession = accountSession,
            authSession = authSession
        )

        composeRule.setContent {
            AppNavigation(
                viewModel = authViewModel,
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
            assertEquals("access-test-token", remotelyLoggedOutAccessToken)
            assertEquals("refresh-test-token", remotelyLoggedOutRefreshToken)
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

        assertTrue(!returnedToHome)
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
