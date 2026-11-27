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
import com.example.map.domain.model.user.UserProfile
import com.example.map.ui.screens.ChangeEmailScreen
import com.example.map.ui.screens.ConfirmEmailScreen
import com.example.map.ui.screens.HomeScreen
import com.example.map.ui.screens.LoginScreen
import com.example.map.ui.screens.ProfilePrivacySettingsScreen
import com.example.map.ui.screens.RegisterScreen
import com.example.map.ui.screens.SettingsScreen
import com.example.map.ui.screens.TripsScreen
import com.example.map.ui.viewmodels.AuthViewModel
import com.example.map.ui.viewmodels.ChangeEmailViewModel
import com.example.map.ui.viewmodels.ConfirmEmailViewModel
import com.example.map.ui.viewmodels.ProfilePrivacySettingsViewModel
import com.example.map.ui.viewmodels.RegisterViewModel
import com.example.map.ui.viewmodels.SettingsViewModel
import com.example.map.ui.viewmodels.TripsViewModel
import kotlinx.coroutines.delay

private const val LOGIN_ROUTE = "login"
private const val HOME_ROUTE = "home"
private const val SETTINGS_ROUTE = "settings"
private const val CHANGE_EMAIL_ROUTE = "change_email"
private const val PROFILE_PRIVACY_ROUTE = "profile_privacy"
private const val REGISTER_ROUTE = "register"
private const val CONFIRM_EMAIL_ROUTE = "confirm_email"
private const val TRIPS_ROUTE = "trips"

@Composable
fun AppNavigation(
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier,
    settingsViewModelFactory: @Composable () -> SettingsViewModel = { hiltViewModel() },
    changeEmailViewModelFactory: @Composable () -> ChangeEmailViewModel = { hiltViewModel() },
    profileViewModelFactory: @Composable () -> ProfilePrivacySettingsViewModel = {
        hiltViewModel()
    },
    registerViewModelFactory: @Composable () -> RegisterViewModel = { hiltViewModel() },
    confirmEmailViewModelFactory: @Composable () -> ConfirmEmailViewModel = { hiltViewModel() },
    tripsViewModelFactory: @Composable () -> TripsViewModel = { hiltViewModel()}
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
                popUpTo(navController.graph.id) {
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
                LaunchedEffect(uiState.requiresEmailVerification) {
                    if (uiState.requiresEmailVerification) {
                        viewModel.consumeEmailVerificationNavigation()
                        navController.navigate(CONFIRM_EMAIL_ROUTE) {
                            launchSingleTop = true
                        }
                    }
                }

                LoginScreen(
                    state = uiState,
                    onLoginClick = viewModel::login,
                    onInputsChanged = viewModel::clearErrors,
                    onRegisterClick = {
                        navController.navigate(REGISTER_ROUTE)
                    }
                )
            }

            composable(REGISTER_ROUTE) {
                val registerViewModel = registerViewModelFactory()

                RegisterScreen(
                    onRegisterSuccess = { _ ->
                        navController.navigate(CONFIRM_EMAIL_ROUTE) {
                            popUpTo(LOGIN_ROUTE) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    },
                    onNavigateBackToLogin = {
                        navController.popBackStack()
                    },
                    viewModel = registerViewModel
                )
            }

            composable(CONFIRM_EMAIL_ROUTE) {
                val confirmEmailViewModel = confirmEmailViewModelFactory()
                val confirmEmailState by confirmEmailViewModel.uiState.collectAsState()

                LaunchedEffect(confirmEmailState.isVerified) {
                    if (confirmEmailState.isVerified) {
                        delay(3_000L)
                        navController.popBackStack(LOGIN_ROUTE, inclusive = false)
                    }
                }

                ConfirmEmailScreen(
                    uiState = confirmEmailState,
                    onCodeChange = confirmEmailViewModel::onCodeChange,
                    onConfirmClick = confirmEmailViewModel::confirmEmail,
                    onResendClick = confirmEmailViewModel::resendCode,
                    onBackClick = {
                        confirmEmailViewModel.leaveScreen()
                        navController.popBackStack(LOGIN_ROUTE, inclusive = false)
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
                    },
                    onSettingsClick = {
                        navController.navigate(SETTINGS_ROUTE)
                    },
                    onMyTripsClick = {
                        navController.navigate(TRIPS_ROUTE)
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

            composable(SETTINGS_ROUTE) {
                val settingsViewModel = settingsViewModelFactory()
                val settingsState by settingsViewModel.uiState.collectAsState()

                LaunchedEffect(settingsState.isAccountDeleted) {
                    if (settingsState.isAccountDeleted) {
                        viewModel.onAccountDeleted()
                    }
                }

                SettingsScreen(
                    uiState = settingsState,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onChangeEmailClick = {
                        navController.navigate(CHANGE_EMAIL_ROUTE)
                    },
                    onDeleteAccountClick = settingsViewModel::showDeleteDialog,
                    onDismissDeleteDialog = settingsViewModel::dismissDeleteDialog,
                    onConfirmDeleteAccount = settingsViewModel::deleteAccount
                )
            }

            composable(CHANGE_EMAIL_ROUTE) {
                val changeEmailViewModel = changeEmailViewModelFactory()
                val changeEmailState by changeEmailViewModel.uiState.collectAsState()

                ChangeEmailScreen(
                    uiState = changeEmailState,
                    onNewEmailChange = changeEmailViewModel::onNewEmailChange,
                    onCodeChange = changeEmailViewModel::onCodeChange,
                    onSubmitEmail = changeEmailViewModel::submitNewEmail,
                    onSubmitCode = changeEmailViewModel::submitCode,
                    onBackToEmail = changeEmailViewModel::backToEmailEntry,
                    onDone = {
                        navController.popBackStack(SETTINGS_ROUTE, inclusive = false)
                    },
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }

            composable(TRIPS_ROUTE) {
                val tripsViewModel = tripsViewModelFactory()
                val tripsState by tripsViewModel.uiState.collectAsState()

                TripsScreen(
                    uiState = tripsState,
                    onSearchChanged = tripsViewModel::onSearchChange,
                    onSortChange = tripsViewModel::onSortChange,
                    onRefresh = tripsViewModel::refresh,
                    onLoadMore = tripsViewModel::loadNextPage,
                    onRetry = tripsViewModel::retry,
                    onViewOnMapClick = { _ -> /* TODO: nawigacja do widoku mapy pojedynczej podróży */ },
                    onEditTripClick = { _ -> /* TODO: nawigacja do kreatora edycji */ },
                    onDeleteTripClick = { _ -> /* TODO: dialog potwierdzenia usunięcia */ },
                    onCreateTripClick = { /* TODO: nawigacja do tworzenia nowej podróży */ },
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onProfileClick = {
                        navController.navigate(PROFILE_PRIVACY_ROUTE)
                    },
                    onSettingsClick = {
                        navController.navigate(SETTINGS_ROUTE)
                    },
                    onLogoutClick = {
                        viewModel.logout()
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
