package com.example.map.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.map.ui.components.Header
import com.example.map.ui.theme.BackgroundDark

@Composable
fun TripListScreen(
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Header(
            title = "Moje podróże",
            onBackClick = onBackClick
        )
    }
}