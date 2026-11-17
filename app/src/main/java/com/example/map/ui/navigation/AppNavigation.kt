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
import com.example.map.ui.screens.ChangeEmailScreen
import com.example.map.ui.screens.HomeScreen
import com.example.map.ui.screens.LoginScreen
import com.example.map.ui.screens.SettingsScreen
import com.example.map.ui.screens.TripListScreen
import com.example.map.ui.screens.TripManagerScreen
import com.example.map.ui.viewmodels.AuthViewModel
import com.example.map.ui.viewmodels.ChangeEmailViewModel
import com.example.map.ui.viewmodels.HomeViewModel
import com.example.map.ui.viewmodels.SettingsViewModel

private const val LOGIN_ROUTE = "login"
private const val HOME_ROUTE = "home"
private const val SETTINGS_ROUTE = "settings"
private const val CHANGE_EMAIL_ROUTE = "change_email"
private const val TRIP_LIST_ROUTE = "trip_list"
private const val TRIP_MANAGER_ROUTE = "trip_manager"

@Composable
fun AppNavigation(
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier,
    homeViewModelFactory: @Composable () -> HomeViewModel = { hiltViewModel() },
    settingsViewModelFactory: @Composable () -> SettingsViewModel = { hiltViewModel() },
    changeEmailViewModelFactory: @Composable () -> ChangeEmailViewModel = { hiltViewModel() }
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
                LoginScreen(
                    state = uiState,
                    onLoginClick = viewModel::login,
                    onInputsChanged = viewModel::clearErrors
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
                    },
                    onNewTripClick = {
                        navController.navigate(TRIP_MANAGER_ROUTE)
                    },
                    onRetryProfileClick = homeViewModel::loadProfile
                )
            }

            composable(TRIP_LIST_ROUTE) {
                TripListScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(TRIP_MANAGER_ROUTE) {
                TripManagerScreen(
                    onBackClick = { navController.popBackStack() }
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
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}