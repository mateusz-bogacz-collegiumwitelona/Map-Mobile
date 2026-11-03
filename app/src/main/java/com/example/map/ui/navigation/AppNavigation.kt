package com.example.map.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.map.ui.screens.HomeScreen
import com.example.map.ui.screens.LoginScreen
import com.example.map.ui.viewmodels.AuthViewModel

private const val LOGIN_ROUTE = "login"
private const val HOME_ROUTE = "home"

@Composable
fun AppNavigation(
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier
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
                    onLoginClick = { _, _, _ ->
                        viewModel.onDemoLogin()
                    }
                )
            }

            composable(HOME_ROUTE) {
                HomeScreen(
                    onLogoutClick = {
                        viewModel.logout()
                    }
                )
            }
        }
    }
}