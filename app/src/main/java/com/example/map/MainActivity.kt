package com.example.map

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.map.data.TokenManager
import com.example.map.data.TokenStorage
import com.example.map.ui.screens.HomeScreen
import com.example.map.ui.screens.LoginScreen
import com.example.map.ui.theme.MapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MapTheme {
                // TODO(auth)
                val tokenStorage: TokenStorage = remember {
                    TokenManager(applicationContext)
                }

                val navController = rememberNavController()

                val startDestination = remember {
                    if (tokenStorage.getAccessToken() != null) "home" else "login"
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        composable("login") {

                            /* TODO: po dodaniu Api do logowania:.
                            * LoginScreen(
                                * onLoginClick = { email, password, rememberMe ->
                                     * if (rememberMe) {
                                     *     tokenStorage.saveTokens(tokens)
                                     * } else {
                                     *     tokenStorage.clearTokens()
                                     * }

                                     * navController.navigate("home") {
                                     *     popUpTo("login") { inclusive = true }
                                     *     launchSingleTop = true
                                     * }
                                *}
                            *)
                            * i usunąć kod poniżej w LoginScreen
                        */

                            LoginScreen(
                                onLoginClick = { _, _, _ ->
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }

                        composable("home") {
                            HomeScreen(
                                onLogoutClick = {
                                    tokenStorage.clearTokens()

                                    navController.navigate("login") {
                                        popUpTo("home") { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}