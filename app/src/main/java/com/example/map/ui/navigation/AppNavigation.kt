package com.example.map.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.map.data.TokenStorage
import com.example.map.ui.screens.HomeScreen
import com.example.map.ui.screens.LoginScreen

private const val LOGIN_ROUTE = "login"
private const val HOME_ROUTE = "home"

@Composable
fun AppNavigation(
    tokenStorage: TokenStorage,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    val startDestination = remember(tokenStorage) {
        if (tokenStorage.getAccessToken() != null) {
            HOME_ROUTE
        } else {
            LOGIN_ROUTE
        }
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(LOGIN_ROUTE) {
                LoginScreen(
                    onLoginClick = { _, _, _ ->
                        navController.navigate(HOME_ROUTE) {
                            popUpTo(LOGIN_ROUTE) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(HOME_ROUTE) {
                HomeScreen(
                    onLogoutClick = {
                        tokenStorage.clearTokens()

                        navController.navigate(LOGIN_ROUTE) {
                            popUpTo(HOME_ROUTE) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}