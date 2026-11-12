package com.example.map.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.map.domain.model.UserProfile
import com.example.map.ui.screens.HomeScreen
import com.example.map.ui.screens.LoginScreen
import com.example.map.ui.screens.ProfilePrivacySettingsScreen
import com.example.map.ui.viewmodels.AuthViewModel
import com.example.map.ui.viewmodels.ProfilePrivacySettingsViewModel

private const val LOGIN_ROUTE = "login"
private const val HOME_ROUTE = "home"
private const val PROFILE_PRIVACY_ROUTE = "profile_privacy"

@Composable
fun AppNavigation(
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier,
    profileViewModelFactory: @Composable () -> ProfilePrivacySettingsViewModel = {
        hiltViewModel()
    }
) {
    val uiState by viewModel.uiState.collectAsState()
    val navController = rememberNavController()

    val destination =
        if (uiState.isAuthenticated) HOME_ROUTE else LOGIN_ROUTE

    LaunchedEffect(destination) {
        val currentRoute = navController.currentBackStackEntry
            ?.destination
            ?.route

        if (currentRoute != null && currentRoute != destination) {
            navController.navigate(destination) {
                popUpTo(currentRoute) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        }
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = destination,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(LOGIN_ROUTE) {
                LoginScreen(
                    onLoginClick = { email, _, _ ->
                        viewModel.onDemoLogin(email)
                    }
                )
            }

            composable(HOME_ROUTE) {
                HomeScreen(
                    profileName = uiState.profile.displayName(),
                    profileInitials = uiState.profile.displayInitials(),
                    onProfileClick = {
                        navController.navigate(PROFILE_PRIVACY_ROUTE)
                    },
                    onLogoutClick = {
                        viewModel.logout()
                    }
                )
            }

            composable(PROFILE_PRIVACY_ROUTE) {
                val profileViewModel = profileViewModelFactory()
                val profileState by profileViewModel.uiState.collectAsState()

                ProfilePrivacySettingsScreen(
                    uiState = profileState,
                    onFirstNameChange = profileViewModel::onFirstNameChange,
                    onLastNameChange = profileViewModel::onLastNameChange,
                    onVisibilityChange = profileViewModel::onVisibilityChange,
                    onSaveClick = profileViewModel::save,
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

private fun UserProfile.displayName(): String =
    listOf(firstName.trim(), lastName.trim())
        .filter { it.isNotEmpty() }
        .joinToString(" ")
        .ifEmpty { "Użytkownik" }

private fun UserProfile.displayInitials(): String {
    val initials = listOf(firstName.trim(), lastName.trim())
        .filter { it.isNotEmpty() }
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")

    return initials.ifEmpty { "?" }
}
