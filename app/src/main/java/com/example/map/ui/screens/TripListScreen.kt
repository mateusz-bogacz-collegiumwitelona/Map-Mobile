package com.example.map.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.map.domain.model.TripSummary
import com.example.map.ui.components.Header
import com.example.map.ui.theme.BackgroundDark
import com.example.map.ui.theme.Black
import com.example.map.ui.theme.ButtonLight
import com.example.map.ui.theme.DangerRed
import com.example.map.ui.theme.SurfaceGray
import com.example.map.ui.theme.TextGray
import com.example.map.ui.viewmodels.TripListUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults

@Composable
fun TripListScreen(
    state: TripListUiState,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onProfileClick: () -> Unit = {},
    onRetryClick: () -> Unit,
    onEditTrip: (String) -> Unit,
    onDeleteTrip: (String) -> Unit,
    onSearchTermChange: (String) -> Unit = {},
    onSortChange: (String, Boolean) -> Unit = { _, _ -> },
    onShowTripDetails: (String, Double) -> Unit = { _, _ -> }
) {
    var tripPendingDeletion by remember {
        mutableStateOf<TripSummary?>(null)
    }

    var sortMenuExpanded by remember { mutableStateOf(false) }
    var searchVisible by remember { mutableStateOf(false) }

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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Moje podróże",
                color = TextGray,
                fontSize = 28.sp,
                modifier = Modifier.weight(1f)
            )

            Surface(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onBackClick)
                    .testTag("trip_list_back_button"),
                color = ButtonLight,
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Wróć",
                    tint = TextGray,
                    modifier = Modifier.padding(9.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                TextButton(
                    onClick = { sortMenuExpanded = true },
                    modifier = Modifier.testTag("trip_sort_button")
                ) {
                    val sortName = when (state.sortBy) {
                        "endDate" -> "Data zakończenia"
                        "title" -> "Nazwa"
                        else -> "Data rozpoczęcia"
                    }
                    val direction = if (state.sortDescending) "↓" else "↑"

                    Text(
                        text = "Sortuj po: $sortName $direction",
                        color = TextGray
                    )
                }

                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Data rozpoczęcia: najnowsze") },
                        onClick = {
                            sortMenuExpanded = false
                            onSortChange("startDate", true)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Data rozpoczęcia: najstarsze") },
                        onClick = {
                            sortMenuExpanded = false
                            onSortChange("startDate", false)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Data zakończenia: najpóźniejsze") },
                        onClick = {
                            sortMenuExpanded = false
                            onSortChange("endDate", true)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Data zakończenia: najwcześniejsze") },
                        onClick = {
                            sortMenuExpanded = false
                            onSortChange("endDate", false)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Nazwa: Z–A") },
                        onClick = {
                            sortMenuExpanded = false
                            onSortChange("title", true)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Nazwa: A–Z") },
                        onClick = {
                            sortMenuExpanded = false
                            onSortChange("title", false)
                        }
                    )
                }
            }

            TextButton(
                onClick = { searchVisible = !searchVisible },
                modifier = Modifier.testTag("trip_search_button")
            ) {
                Text(
                    text = if (searchVisible) "Ukryj szukanie" else "Szukaj",
                    color = TextGray
                )
            }
        }

        if (searchVisible) {
            OutlinedTextField(
                value = state.searchTerm,
                onValueChange = { onSearchTermChange(it.take(100)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("trip_search_input"),
                label = { Text("Szukaj po nazwie lub opisie") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = ButtonLight,
                    unfocusedBorderColor = SurfaceGray,
                    focusedLabelColor = ButtonLight,
                    unfocusedLabelColor = TextGray,
                    cursorColor = ButtonLight
                )
            )
        }

        when {
            state.isLoading && state.trips.isEmpty() -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(24.dp),
                    color = ButtonLight
                )
            }

            state.errorMessage != null && state.trips.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.errorMessage,
                        color = DangerRed,
                        modifier = Modifier.testTag("trip_list_error")
                    )
                    TextButton(
                        onClick = onRetryClick,
                        modifier = Modifier.testTag("trip_list_retry")
                    ) {
                        Text("Spróbuj ponownie")
                    }
                }
            }

            state.trips.isEmpty() -> {
                Text(
                    text = if (state.searchTerm.isBlank()) {
                        "Nie masz jeszcze żadnych podróży."
                    } else {
                        "Brak podróży pasujących do wyszukiwania."
                    },
                    color = TextGray,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp)
                        .testTag("trip_list"),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    state.trips.forEach { trip ->
                        TripSummaryCard(
                            trip = trip,
                            onEditClick = { onEditTrip(trip.id) },
                            onDeleteClick = {
                                tripPendingDeletion = trip
                            },
                            onShowDetailsClick = {
                                onShowTripDetails(trip.id, trip.totalDistanceKm)
                            }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    tripPendingDeletion?.let { trip ->
        AlertDialog(
            onDismissRequest = { tripPendingDeletion = null },
            title = { Text("Usunąć podróż?") },
            text = {
                Text("Czy na pewno chcesz usunąć podróż „${trip.title}”?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        tripPendingDeletion = null
                        onDeleteTrip(trip.id)
                    },
                    modifier = Modifier.testTag("trip_delete_confirm"),
                    colors = androidx.compose.material3.ButtonDefaults
                        .textButtonColors(contentColor = DangerRed)
                ) {
                    Text("Usuń")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { tripPendingDeletion = null },
                    modifier = Modifier.testTag("trip_delete_cancel")
                ) {
                    Text("Anuluj")
                }
            }
        )
    }
}

@Composable
private fun TripSummaryCard(
    trip: TripSummary,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onShowDetailsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceGray)
            .padding(10.dp)
            .testTag("trip_card_${trip.id}")
    ) {
        Text(
            text = trip.title,
            color = Color.White,
            fontSize = 19.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = "Dystans",
                tint = Color.White
            )
            Text(
                text = "${trip.totalDistanceKm.toInt()} km",
                color = Color.White,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = "Daty podróży",
                tint = Color.White
            )
            Text(
                text = "${formatDate(trip.startDateMillis)} - " +
                        (trip.endDateMillis?.let(::formatDate) ?: "Trwa"),
                color = Color.White,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TripAction(
                text = "Szczegóły",
                tag = "trip_show_route_${trip.id}",
                color = ButtonLight,
                onClick = onShowDetailsClick
            )
            TripAction(
                text = "Edytuj",
                tag = "trip_edit_button_${trip.id}",
                color = ButtonLight,
                onClick = onEditClick
            )
            TripAction(
                text = "Usuń",
                tag = "trip_delete_button_${trip.id}",
                color = DangerRed,
                onClick = onDeleteClick
            )
        }
    }
}

@Composable
private fun TripAction(
    text: String,
    tag: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag(tag),
        color = color,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            color = Black,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
        )
    }
}

private fun formatDate(millis: Long): String {
    val formatter = SimpleDateFormat("dd.MM.yy", Locale("pl", "PL")).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return formatter.format(Date(millis))
}