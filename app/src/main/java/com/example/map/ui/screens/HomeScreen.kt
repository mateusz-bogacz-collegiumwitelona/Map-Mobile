package com.example.map.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.map.ui.components.Header
import com.example.map.ui.theme.BackgroundDark
import com.example.map.ui.theme.ButtonLight
import com.example.map.ui.theme.SurfaceGray

@Composable
fun HomeScreen(onLogoutClick: () -> Unit) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Header(title = "Map", onLogoutClick = onLogoutClick)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(SurfaceGray),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "JK", color = Color.White, fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Jan Kowalski", color = Color.White, fontSize = 18.sp)

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                HomeButton(title = "Moje podróże")
                HomeButton(title = "Nowa podróż")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                HomeButton(title = "Inni")
                HomeButton(title = "Przejechany dystans")
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun HomeButton(title: String) {
    Box(
        modifier = Modifier
            .size(150.dp)
            .background(SurfaceGray, RoundedCornerShape(12.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = title, color = ButtonLight, fontSize = 14.sp)
    }
}