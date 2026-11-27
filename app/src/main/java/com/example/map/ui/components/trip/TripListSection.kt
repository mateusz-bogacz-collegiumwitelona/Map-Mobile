package com.example.map.ui.components.trip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Commute
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.map.domain.model.trip.TransportType
import com.example.map.domain.model.trip.TripSummary
import com.example.map.domain.model.trip.TripsError
import com.example.map.ui.components.PrimaryButton
import com.example.map.ui.theme.BackgroundDark
import com.example.map.ui.theme.ButtonLight
import com.example.map.ui.theme.DangerRed
import com.example.map.ui.theme.ErrorRed
import com.example.map.ui.theme.SurfaceGray
import com.example.map.ui.theme.TextGray
import com.example.map.ui.viewmodels.TripsUiState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripListSection(
    uiState: TripsUiState,
    isReadOnly: Boolean = false,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onViewOnMapClick: (tripId: String) -> Unit,
    onEditTripClick: (tripId: String) -> Unit = {},
    onDeleteTripClick: (tripId: String) -> Unit = {},
    onCreateTripClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState, uiState.hasNextPage, uiState.isLoadingMore) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisible >= totalItems - 3
        }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                if (uiState.hasNextPage && !uiState.isLoadingMore && !uiState.isLoading) {
                    onLoadMore()
                }
            }
    }

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
            .fillMaxSize()
            .testTag("trips_pull_to_refresh")
    ) {
        when {
            uiState.isLoading && uiState.items.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = ButtonLight,
                        modifier = Modifier.testTag("trips_loading")
                    )
                }
            }

            uiState.error != null && uiState.items.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    TripsErrorBanner(
                        error = uiState.error,
                        backendMessage = uiState.errorMessage,
                        onRetry = onRetry,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            uiState.isEmpty -> {
                TripsEmptyState(
                    hasSearchQuery = uiState.searchTerm.isNotBlank(),
                    isReadOnly = isReadOnly,
                    onCreateTripClick = onCreateTripClick
                )
            }

            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("trips_list"),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    if (uiState.error != null) {
                        item(key = "error_banner") {
                            TripsErrorBanner(
                                error = uiState.error,
                                backendMessage = uiState.errorMessage,
                                onRetry = onRetry
                            )
                        }
                    }

                    items(items = uiState.items, key = { it.id }) { trip ->
                        TripCard(
                            trip = trip,
                            isReadOnly = isReadOnly,
                            onViewOnMapClick = { onViewOnMapClick(trip.id) },
                            onEditClick = { onEditTripClick(trip.id) },
                            onDeleteClick = { onDeleteTripClick(trip.id) }
                        )
                    }

                    if (uiState.isLoadingMore) {
                        item(key = "loading_more") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = ButtonLight,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .testTag("trips_loading_more"),
                                    strokeWidth = 2.5.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TripCard(
    trip: TripSummary,
    isReadOnly: Boolean,
    onViewOnMapClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceGray)
            .padding(16.dp)
            .testTag("trip_card_${trip.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = trip.title,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = formatDistance(trip.totalDistance),
                color = ButtonLight,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = formatDateRange(trip.startDate, trip.endDate),
            color = TextGray,
            fontSize = 13.sp
        )

        if (!trip.description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = trip.description,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (trip.transportTypes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                trip.transportTypes.distinct().forEach { type ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(BackgroundDark)
                            .padding(6.dp)
                    ) {
                        Icon(
                            imageVector = type.toIcon(),
                            contentDescription = type.toContentDescription(),
                            tint = ButtonLight,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = BackgroundDark, thickness = 1.dp)
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onViewOnMapClick,
                modifier = Modifier.testTag("trip_map_${trip.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = null,
                    tint = ButtonLight,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Zobacz na mapie", color = ButtonLight, fontSize = 13.sp)
            }

            if (!isReadOnly) {
                Row {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.testTag("trip_edit_${trip.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edytuj podróż",
                            tint = TextGray
                        )
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.testTag("trip_delete_${trip.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Usuń podróż",
                            tint = DangerRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TripsEmptyState(
    hasSearchQuery: Boolean,
    isReadOnly: Boolean,
    onCreateTripClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("trips_empty"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = when {
                hasSearchQuery -> "Brak podróży pasujących do wyszukiwania."
                isReadOnly -> "Ten użytkownik nie ma jeszcze żadnych podróży."
                else -> "Nie masz jeszcze żadnych podróży."
            },
            color = TextGray,
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )

        if (!hasSearchQuery && !isReadOnly) {
            Spacer(modifier = Modifier.height(16.dp))
            PrimaryButton(
                text = "Dodaj pierwszą podróż",
                onClick = onCreateTripClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("trips_create_first")
            )
        }
    }
}

@Composable
private fun TripsErrorBanner(
    error: TripsError,
    backendMessage: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val message = when (error) {
        TripsError.NETWORK -> "Brak połączenia z internetem."
        TripsError.UNAUTHORIZED -> "Sesja wygasła. Zaloguj się ponownie."
        TripsError.RATE_LIMITED -> "Zbyt wiele żądań. Odczekaj chwilę."
        TripsError.UNKNOWN -> backendMessage ?: "Nie udało się pobrać podróży."
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceGray)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("trips_error_banner"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = message,
            color = ErrorRed,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        TextButton(
            onClick = onRetry,
            modifier = Modifier.testTag("trips_retry")
        ) {
            Text(text = "Ponów", color = ButtonLight)
        }
    }
}

private fun formatDistance(distanceKm: Double): String =
    String.format(Locale.getDefault(), "%.1f km", distanceKm)

private fun formatDateRange(startDate: String, endDate: String?): String {
    val start = startDate.substringBefore("T")
    val end = endDate?.substringBefore("T")
    return if (end.isNullOrBlank() || end == start) start else "$start – $end"
}

private fun TransportType.toIcon(): ImageVector = when (this) {
    TransportType.CAR -> Icons.Default.DirectionsCar
    TransportType.PLANE -> Icons.Default.Flight
    TransportType.TRAIN -> Icons.Default.Train
    TransportType.OTHER -> Icons.Default.Commute
}

private fun TransportType.toContentDescription(): String = when (this) {
    TransportType.CAR -> "Samochód"
    TransportType.PLANE -> "Samolot"
    TransportType.TRAIN -> "Pociąg"
    TransportType.OTHER -> "Inny środek transportu"
}