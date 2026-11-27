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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.map.domain.model.trip.TripSortBy
import com.example.map.ui.components.Header
import com.example.map.ui.components.trip.TripListSection
import com.example.map.ui.theme.BackgroundDark
import com.example.map.ui.theme.ButtonLight
import com.example.map.ui.theme.SurfaceGray
import com.example.map.ui.theme.TextGray
import com.example.map.ui.viewmodels.TripsUiState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward

@Composable
fun TripsScreen(
    uiState: TripsUiState,
    onSearchChanged: (String) -> Unit,
    onSortChange: (TripSortBy, Boolean) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onViewOnMapClick: (String) -> Unit,
    onBackClick: () -> Unit,
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    title: String = "Moje podróże",
    isReadOnly: Boolean = false,
    onEditTripClick: (String) -> Unit = {},
    onDeleteTripClick: (String) -> Unit = {},
    onCreateTripClick: () -> Unit = {}
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
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    color = TextGray,
                    fontSize = 24.sp
                )

                Button(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("trips_back_button"),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonLight,
                        contentColor = TextGray
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Wróć",
                        tint = TextGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TripsSearchField(
                value = uiState.searchTerm,
                onValueChange = onSearchChanged
            )

            Spacer(modifier = Modifier.height(10.dp))

            TripsSortBar(
                selectedSort = uiState.sortBy ?: TripSortBy.START_DATE,
                sortDescending = uiState.sortDescending,
                onSortChange = onSortChange
            )

            Spacer(modifier = Modifier.height(12.dp))

            TripListSection(
                uiState = uiState,
                isReadOnly = isReadOnly,
                onRefresh = onRefresh,
                onLoadMore = onLoadMore,
                onRetry = onRetry,
                onViewOnMapClick = onViewOnMapClick,
                onEditTripClick = onEditTripClick,
                onDeleteTripClick = onDeleteTripClick,
                onCreateTripClick = onCreateTripClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TripsSearchField(
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = "Szukaj po tytule lub opisie...",
                color = TextGray,
                fontSize = 14.sp
            )
        },
        singleLine = true,
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = TextGray,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = if (value.isNotEmpty()) {
            {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Wyczyść wyszukiwanie",
                        tint = TextGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else {
            null
        },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("trips_search_field"),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = SurfaceGray,
            unfocusedContainerColor = SurfaceGray,
            focusedBorderColor = ButtonLight.copy(alpha = 0.7f),
            unfocusedBorderColor = TextGray.copy(alpha = 0.25f),
            cursorColor = ButtonLight
        )
    )
}

@Composable
private fun TripsSortBar(
    selectedSort: TripSortBy,
    sortDescending: Boolean,
    onSortChange: (TripSortBy, Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SortChip(
            label = "Początek",
            selected = selectedSort == TripSortBy.START_DATE,
            onClick = {
                val nextDesc = if (selectedSort == TripSortBy.START_DATE) !sortDescending else true
                onSortChange(TripSortBy.START_DATE, nextDesc)
            }
        )
        SortChip(
            label = "Koniec",
            selected = selectedSort == TripSortBy.END_DATE,
            onClick = {
                val nextDesc = if (selectedSort == TripSortBy.END_DATE) !sortDescending else true
                onSortChange(TripSortBy.END_DATE, nextDesc)
            }
        )
        SortChip(
            label = "Tytuł",
            selected = selectedSort == TripSortBy.TITLE,
            onClick = {
                val nextDesc = if (selectedSort == TripSortBy.TITLE) !sortDescending else false
                onSortChange(TripSortBy.TITLE, nextDesc)
            }
        )

        Spacer(modifier = Modifier.weight(1f))

        SortDirectionButton(
            sortDescending = sortDescending,
            onClick = { onSortChange(selectedSort, !sortDescending) }
        )
    }
}

@Composable
private fun SortChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) ButtonLight else SurfaceGray)
            .border(
                width = 1.dp,
                color = if (selected) ButtonLight else TextGray.copy(alpha = 0.3f),
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            color = if (selected) BackgroundDark else Color.White,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun SortDirectionButton(
    sortDescending: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(SurfaceGray)
            .border(1.dp, TextGray.copy(alpha = 0.3f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp)
            .testTag("trips_sort_direction"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (sortDescending) {
                Icons.Default.ArrowDownward
            } else {
                Icons.Default.ArrowUpward
            },
            contentDescription = null,
            tint = ButtonLight,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (sortDescending) "Malejąco" else "Rosnąco",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}