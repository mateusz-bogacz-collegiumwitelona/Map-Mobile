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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.map.domain.model.UserProfile
import androidx.navigation.navArgument
import com.example.map.ui.screens.ChangeEmailScreen
import com.example.map.ui.screens.ConfirmEmailScreen
import com.example.map.ui.screens.HomeScreen
import com.example.map.ui.screens.LoginScreen
import com.example.map.ui.screens.ProfilePrivacySettingsScreen
import com.example.map.ui.screens.RegisterScreen
import com.example.map.ui.screens.SettingsScreen
import com.example.map.ui.screens.TripListScreen
import com.example.map.ui.screens.TripManagerScreen
import com.example.map.ui.viewmodels.AuthViewModel
import com.example.map.ui.viewmodels.ChangeEmailViewModel
import com.example.map.ui.viewmodels.HomeViewModel
import com.example.map.ui.viewmodels.ConfirmEmailViewModel
import com.example.map.ui.viewmodels.ProfilePrivacySettingsViewModel
import com.example.map.ui.viewmodels.RegisterViewModel
import com.example.map.ui.viewmodels.SettingsViewModel
import com.example.map.ui.viewmodels.TripListViewModel
import com.example.map.ui.viewmodels.TripManagerViewModel
import com.example.map.ui.screens.TripDetailsScreen
import com.example.map.ui.viewmodels.TripDetailsViewModel
import kotlinx.coroutines.delay

private const val LOGIN_ROUTE = "login"
private const val HOME_ROUTE = "home"
private const val SETTINGS_ROUTE = "settings"
private const val CHANGE_EMAIL_ROUTE = "change_email"
private const val PROFILE_PRIVACY_ROUTE = "profile_privacy"
private const val REGISTER_ROUTE = "register"
private const val CONFIRM_EMAIL_ROUTE = "confirm_email"
private const val TRIP_LIST_ROUTE = "trip_list"
private const val TRIP_MANAGER_ROUTE = "trip_manager"
private const val TRIP_DETAILS_ROUTE = "trip_details"

@Composable
fun AppNavigation(
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier,
    homeViewModelFactory: @Composable () -> HomeViewModel = { hiltViewModel() },
    settingsViewModelFactory: @Composable () -> SettingsViewModel = { hiltViewModel() },
    changeEmailViewModelFactory: @Composable () -> ChangeEmailViewModel = { hiltViewModel() },
    tripListViewModelFactory: @Composable () -> TripListViewModel = { hiltViewModel() },
    tripManagerViewModelFactory: @Composable () -> TripManagerViewModel = { hiltViewModel() },
    tripDetailsViewModelFactory: @Composable () -> TripDetailsViewModel = { hiltViewModel() }
    changeEmailViewModelFactory: @Composable () -> ChangeEmailViewModel = { hiltViewModel() },
    profileViewModelFactory: @Composable () -> ProfilePrivacySettingsViewModel = {
        hiltViewModel()
    },
    registerViewModelFactory: @Composable () -> RegisterViewModel = { hiltViewModel() },
    confirmEmailViewModelFactory: @Composable () -> ConfirmEmailViewModel = { hiltViewModel() }
) {
    val uiState by viewModel.uiState.collectAsState()
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()

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
                val homeViewModel = homeViewModelFactory()
                val homeState by homeViewModel.uiState.collectAsState()

                HomeScreen(
                    uiState = homeState,
                    onLogoutClick = viewModel::logout,
                    onSettingsClick = {
                        navController.navigate(SETTINGS_ROUTE)
                    },
                    onMyTripsClick = {
                        navController.navigate(TRIP_LIST_ROUTE)
                    profileName = uiState.profile.displayName(),
                    profileInitials = uiState.profile.displayInitials(),
                    onProfileClick = {
                        navController.navigate(PROFILE_PRIVACY_ROUTE)
                    },
                    onLogoutClick = {
                        viewModel.logout()
                    },
                    onNewTripClick = {
                        navController.navigate(TRIP_MANAGER_ROUTE)
                    },
                    onRetryProfileClick = homeViewModel::loadProfile
                )
            }

            composable(TRIP_LIST_ROUTE) {
                val tripListViewModel = tripListViewModelFactory()
                val tripListState by tripListViewModel.uiState.collectAsState()

                LaunchedEffect(currentBackStackEntry) {
                    if (
                        currentBackStackEntry?.destination?.route ==
                        TRIP_LIST_ROUTE
                    ) {
                        tripListViewModel.loadTrips()
                    }
                }

                TripListScreen(
                    state = tripListState,
                    onBackClick = { navController.popBackStack() },
                    onSettingsClick = {
                        navController.navigate(SETTINGS_ROUTE)
                    },
                    onLogoutClick = viewModel::logout,
                    onRetryClick = tripListViewModel::loadTrips,
                    onEditTrip = { tripId ->
                        navController.navigate(
                            "$TRIP_MANAGER_ROUTE?tripId=$tripId"
                        )
                    },
                    onDeleteTrip = tripListViewModel::deleteTrip,
                    onSearchTermChange = tripListViewModel::onSearchTermChange,
                    onSortChange = tripListViewModel::onSortChange,
                    onShowTripDetails = { tripId, totalDistanceKm ->
                        navController.navigate(
                            "$TRIP_DETAILS_ROUTE/$tripId?totalDistanceKm=$totalDistanceKm"
                        )
                    }
                )
            }

            composable(
                route = "$TRIP_DETAILS_ROUTE/{tripId}?totalDistanceKm={totalDistanceKm}",
                arguments = listOf(
                    navArgument("tripId") {
                        type = NavType.StringType
                    },
                    navArgument("totalDistanceKm") {
                        type = NavType.StringType
                        defaultValue = "0.0"
                    }
                )
            ) { backStackEntry ->
                val tripId = backStackEntry.arguments
                    ?.getString("tripId")
                    .orEmpty()
                val totalDistanceKm = backStackEntry.arguments
                    ?.getString("totalDistanceKm")
                    ?.toDoubleOrNull()
                    ?: 0.0

                val tripDetailsViewModel = tripDetailsViewModelFactory()
                val tripDetailsState by tripDetailsViewModel.uiState.collectAsState()

                LaunchedEffect(tripId, totalDistanceKm) {
                    tripDetailsViewModel.loadTrip(tripId, totalDistanceKm)
                }

                TripDetailsScreen(
                    state = tripDetailsState,
                    onBackClick = { navController.popBackStack() },
                    onSettingsClick = {
                        navController.navigate(SETTINGS_ROUTE)
                    },
                    onLogoutClick = viewModel::logout,
                    onRetryClick = {
                        tripDetailsViewModel.loadTrip(tripId, totalDistanceKm)
                    }
                )
            }

            composable(
                route = "$TRIP_MANAGER_ROUTE?tripId={tripId}",
                arguments = listOf(
                    navArgument("tripId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val tripId = backStackEntry.arguments?.getString("tripId")
                val tripManagerViewModel = tripManagerViewModelFactory()
                val tripManagerState by tripManagerViewModel.uiState.collectAsState()

                LaunchedEffect(tripId) {
                    tripManagerViewModel.loadTrip(tripId)
                }

                TripManagerScreen(
                    state = tripManagerState,
                    onBackClick = { navController.popBackStack() },
                    onTitleChange = tripManagerViewModel::onTitleChange,
                    onDescriptionChange = tripManagerViewModel::onDescriptionChange,
                    onStartDateSelected =
                        tripManagerViewModel::onStartDateSelected,
                    onEndDateEnabledChange =
                        tripManagerViewModel::onEndDateEnabledChange,
                    onEndDateSelected =
                        tripManagerViewModel::onEndDateSelected,
                    onAddSegment = tripManagerViewModel::addSegment,
                    onRemoveSegment = tripManagerViewModel::removeSegment,
                    onMoveSegment = tripManagerViewModel::moveSegment,
                    onTransportTypeChange =
                        tripManagerViewModel::onTransportTypeChange,
                    onOpenPlacePicker =
                        tripManagerViewModel::openPlacePicker,
                    onClosePlacePicker =
                        tripManagerViewModel::closePlacePicker,
                    onPlaceQueryChange =
                        tripManagerViewModel::onPlaceQueryChange,
                    onPlaceSelected =
                        tripManagerViewModel::selectPlace,
                    onSaveTrip = tripManagerViewModel::saveTrip,
                    onTripCreated = { navController.popBackStack() },
                    onSettingsClick = {
                        navController.navigate(SETTINGS_ROUTE)
                    },
                    onLogoutClick = viewModel::logout
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
                    onBackClick = { navController.popBackStack() },
                    onChangeEmailClick = {
                        navController.navigate(CHANGE_EMAIL_ROUTE)
                    },
                    onDeleteAccountClick =
                        settingsViewModel::showDeleteDialog,
                    onDismissDeleteDialog =
                        settingsViewModel::dismissDeleteDialog,
                    onConfirmDeleteAccount =
                        settingsViewModel::deleteAccount
                )
            }

            composable(CHANGE_EMAIL_ROUTE) {
                val changeEmailViewModel = changeEmailViewModelFactory()
                val changeEmailState by changeEmailViewModel.uiState.collectAsState()

                ChangeEmailScreen(
                    uiState = changeEmailState,
                    onNewEmailChange =
                        changeEmailViewModel::onNewEmailChange,
                    onCodeChange =
                        changeEmailViewModel::onCodeChange,
                    onSubmitEmail =
                        changeEmailViewModel::submitNewEmail,
                    onSubmitCode =
                        changeEmailViewModel::submitCode,
                    onBackToEmail =
                        changeEmailViewModel::backToEmailEntry,
                    onDone = {
                        navController.popBackStack(
                            SETTINGS_ROUTE,
                            inclusive = false
                        )
                    },
                    onBackClick = { navController.popBackStack() }
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
