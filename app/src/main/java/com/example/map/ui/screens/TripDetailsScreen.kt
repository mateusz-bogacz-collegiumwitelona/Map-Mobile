package com.example.map.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.example.map.domain.model.TripSegmentDetails
import com.example.map.ui.components.Header
import com.example.map.ui.theme.BackgroundDark
import com.example.map.ui.theme.ButtonLight
import com.example.map.ui.theme.DangerRed
import com.example.map.ui.theme.SurfaceGray
import com.example.map.ui.theme.TextGray
import com.example.map.ui.viewmodels.TripDetailsUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun TripDetailsScreen(
    state: TripDetailsUiState,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onRetryClick: () -> Unit
) {
    val trip = state.trip

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Header(
            title = "Map",
            onSettingsClick = onSettingsClick,
            onLogoutClick = onLogoutClick
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = trip?.draft?.title ?: "Szczegóły podróży",
                color = TextGray,
                fontSize = 26.sp,
                modifier = Modifier.weight(1f)
            )

            Surface(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .testTag("trip_details_back_button"),
                onClick = onBackClick,
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

        when {
            state.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(24.dp),
                    color = ButtonLight
                )
            }

            state.errorMessage != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.errorMessage,
                        color = DangerRed
                    )
                    TextButton(onClick = onRetryClick) {
                        Text("Spróbuj ponownie")
                    }
                }
            }

            trip != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = trip.draft.description
                            ?.takeIf(String::isNotBlank)
                            ?: "Brak opisu podróży.",
                        color = TextGray,
                        fontSize = 16.sp
                    )

                    InfoCard(
                        title = "Całkowity dystans",
                        value = formatDistance(state.totalDistanceKm)
                    )

                    val dateRange =
                        "${formatDate(trip.draft.startDateMillis)} – " +
                                (trip.draft.endDateMillis?.let(::formatDate) ?: "Trwa")

                    InfoCard(
                        title = "Termin podróży",
                        value = dateRange
                    )

                    Text(
                        text = "Odcinki trasy",
                        color = Color.White,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    trip.segmentDetails.forEachIndexed { index, segment ->
                        SegmentDetailsCard(
                            number = index + 1,
                            segment = segment
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    value: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceGray)
            .padding(14.dp)
    ) {
        Text(
            text = title,
            color = TextGray,
            fontSize = 14.sp
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SegmentDetailsCard(
    number: Int,
    segment: TripSegmentDetails
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceGray)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Odcinek $number",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )

        Text(
            text = "${segment.originName} → ${segment.destinationName}",
            color = Color.White,
            fontSize = 16.sp
        )

        Text(
            text = "Transport: ${segment.transportType.label}",
            color = TextGray
        )

        Text(
            text = "Dystans: ${formatDistance(segment.distanceKm)}",
            color = TextGray
        )

        if (segment.isFallbackRoute) {
            Text(
                text = "Dystans orientacyjny",
                color = TextGray,
                fontSize = 13.sp
            )
        }
    }
}

private fun formatDistance(distanceKm: Double): String =
    String.format(Locale("pl", "PL"), "%.2f km", distanceKm)

private fun formatDate(millis: Long): String {
    val formatter = SimpleDateFormat("dd.MM.yy", Locale("pl", "PL")).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return formatter.format(Date(millis))
}