package com.example.map

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import com.example.map.data.TokenManager
import com.example.map.ui.navigation.AppNavigation
import com.example.map.ui.theme.MapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MapTheme {
                // TODO: Pass TokenStorage via DI once DI has been added
                val tokenStorage = remember {
                    TokenManager(applicationContext)
                }

                AppNavigation(tokenStorage = tokenStorage)
            }
        }
    }
}