package com.example.map

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.map.ui.navigation.AppNavigation
import com.example.map.ui.theme.MapTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.viewModels
import com.example.map.ui.viewmodels.AuthViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MapTheme {
                AppNavigation(viewModel = authViewModel)
            }
        }
    }
}