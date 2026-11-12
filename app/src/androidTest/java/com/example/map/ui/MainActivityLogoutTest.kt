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
import com.example.map.domain.repository.TokenStorage
import com.example.map.domain.usecase.AuthSession
import com.example.map.domain.usecase.ProfileSession
import com.example.map.testing.FakeProfileStorage
import com.example.map.testing.FakeUserProfileRepository
import com.example.map.ui.navigation.AppNavigation
import com.example.map.ui.viewmodels.AuthViewModel
import com.example.map.ui.viewmodels.ProfilePrivacySettingsViewModel
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
    private lateinit var profileSession: ProfileSession

    @Before
    fun setUp() {
        tokenStorage = FakeTokenStorage(
            initialTokens = AuthTokens(
                accessToken = "access-test-token",
                refreshToken = "refresh-test-token"
            )
        )

        val storedProfile = UserProfile(
            email = "user@example.com",
            firstName = "Anna",
            lastName = "Nowak",
            isPublic = false
        )
        profileStorage = FakeProfileStorage(storedProfile)
        profileSession = ProfileSession(
            FakeUserProfileRepository().apply { remoteProfile = storedProfile },
            profileStorage
        )

        val authViewModel = AuthViewModel(
            authSession = AuthSession(tokenStorage),
            profileSession = profileSession
        )
        val profileViewModel = ProfilePrivacySettingsViewModel(
            profileSession = profileSession,
            authSession = AuthSession(tokenStorage)
        )

        composeRule.setContent {
            AppNavigation(
                viewModel = authViewModel,
                profileViewModelFactory = { profileViewModel }
            )
        }
    }

    @Test
    fun homeShowsProfileNameFromSession() {
        composeRule.onNodeWithText("Anna Nowak").assertIsDisplayed()
    }

    @Test
    fun logoutClearsTokensAndProfileShowsLoginAndBackDoesNotReturnToHome() {
        composeRule.onNodeWithText("Anna Nowak").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Wyloguj").performClick()

        composeRule.onNodeWithText("Zaloguj się").assertIsDisplayed()
        assertNull(tokenStorage.getAccessToken())
        assertNull(tokenStorage.getRefreshToken())
        assertNull(profileStorage.getProfile())
        assertNull(profileStorage.getEmail())

        Espresso.pressBackUnconditionally()

        val returnedToHome = try {
            composeRule.onAllNodesWithText("Anna Nowak").fetchSemanticsNodes().isNotEmpty()
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
