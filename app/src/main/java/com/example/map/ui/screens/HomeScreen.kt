package com.example.map.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.map.ui.components.Header
import com.example.map.ui.theme.BackgroundDark
import com.example.map.ui.theme.ButtonLight
import com.example.map.ui.theme.ErrorRed
import com.example.map.ui.theme.SurfaceGray
import com.example.map.ui.theme.TextGray
import com.example.map.ui.viewmodels.HomeUiState

@Composable
fun HomeScreen(
    profileName: String,
    profileInitials: String,
    onProfileClick: () -> Unit,
    onLogoutClick: () -> Unit,
    uiState: HomeUiState = HomeUiState(),
    onMyTripsClick: () -> Unit = {},
    onNewTripClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onRetryProfileClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Header(
            title = "Map",
            onProfileClick = onProfileClick,
            onSettingsClick = onSettingsClick,
            onLogoutClick = onLogoutClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
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
                Text(
                    text = uiState.initials.ifBlank {
                        if (uiState.isLoading) "…" else "?"
                    },
                    color = ButtonLight,
                    fontSize = 24.sp
                )
                Text(text = profileInitials, color = Color.White, fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            val displayName = when {
                uiState.fullName.isNotBlank() -> uiState.fullName
                uiState.isLoading -> "Ładowanie profilu…"
                else -> "Użytkownik"
            }

            Text(
                text = displayName,
                color = ButtonLight,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("home_profile_name")
            )

            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage,
                    color = ErrorRed,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                TextButton(onClick = onRetryProfileClick) {
                    Text("Spróbuj ponownie")
                }
            }
            Text(text = profileName, color = Color.White, fontSize = 18.sp)

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                HomeButton(
                    title = "Moje podróże",
                    icon = Icons.Outlined.Public,
                    testTag = "home_my_trips_button",
                    onClick = onMyTripsClick
                )
                HomeButton(
                    title = "Nowa podróż",
                    icon = Icons.Outlined.DirectionsCar,
                    testTag = "home_new_trip_button",
                    onClick = onNewTripClick
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                HomeButton(
                    title = "Inni",
                    icon = Icons.Outlined.PersonOutline,
                    onClick = {}
                )
                HomeButton(
                    title = "Przejechany dystans",
                    icon = Icons.Outlined.TravelExplore,
                    onClick = {}
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun HomeButton(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String? = null
) {
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = Modifier
            .size(150.dp)
            .clip(shape)
            .background(SurfaceGray)
            .clickable(role = Role.Button, onClick = onClick)
            .then(
                if (testTag != null) Modifier.testTag(testTag) else Modifier
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ButtonLight,
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                color = TextGray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}