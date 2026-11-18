package com.example.map.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.map.domain.model.PlaceOption
import com.example.map.domain.model.TripSegmentDraft
import com.example.map.domain.model.TripTransportType
import com.example.map.ui.components.Header
import com.example.map.ui.components.PrimaryButton
import com.example.map.ui.theme.BackgroundDark
import com.example.map.ui.theme.Black
import com.example.map.ui.theme.ButtonLight
import com.example.map.ui.theme.DangerRed
import com.example.map.ui.theme.SurfaceGray
import com.example.map.ui.theme.TextGray
import com.example.map.ui.viewmodels.TripManagerUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import sh.calvin.reorderable.ReorderableColumn

private enum class DateField {
    START,
    END
}

@Composable
fun TripManagerScreen(
    state: TripManagerUiState,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onTitleChange: (String) -> Unit = {},
    onDescriptionChange: (String) -> Unit = {},
    onStartDateSelected: (Long) -> Unit = {},
    onEndDateEnabledChange: (Boolean) -> Unit = {},
    onEndDateSelected: (Long) -> Unit = {},
    onAddSegment: () -> Unit = {},
    onRemoveSegment: (Long) -> Unit = {},
    onMoveSegment: (Int, Int) -> Unit = { _, _ -> },
    onTransportTypeChange: (Long, TripTransportType) -> Unit = { _, _ -> },
    onOpenPlacePicker: (Long, Boolean) -> Unit = { _, _ -> },
    onClosePlacePicker: () -> Unit = {},
    onPlaceQueryChange: (String) -> Unit = {},
    onPlaceSelected: (PlaceOption) -> Unit = {},
    onSaveTrip: () -> Unit = {},
    onTripCreated: () -> Unit = {}
) {
    var dateField by remember { mutableStateOf<DateField?>(null) }

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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Dodawanie podróży",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium
            )

            Surface(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onBackClick)
                    .testTag("trip_manager_back_button"),
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            OutlinedTextField(
                value = state.title,
                onValueChange = onTitleChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("trip_title_field"),
                placeholder = { Text("Nazwa", color = TextGray) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                colors = tripFieldColors(),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = state.description,
                onValueChange = onDescriptionChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp)
                    .testTag("trip_description_field"),
                placeholder = { Text("Opis", color = TextGray) },
                minLines = 3,
                maxLines = 6,
                colors = tripFieldColors(),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(Modifier.height(12.dp))

            DateButton(
                text = state.startDateMillis?.let(::formatDate) ?: "od",
                tag = "trip_start_date",
                onClick = { dateField = DateField.START }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Dodaj datę zakończenia",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Switch(
                    checked = state.hasEndDate,
                    onCheckedChange = onEndDateEnabledChange,
                    modifier = Modifier.testTag("trip_end_date_toggle")
                )
            }

            if (state.hasEndDate) {
                DateButton(
                    text = state.endDateMillis?.let(::formatDate) ?: "do",
                    tag = "trip_end_date",
                    onClick = { dateField = DateField.END }
                )
            }

            Spacer(Modifier.height(16.dp))

            ReorderableColumn(
                modifier = Modifier.fillMaxWidth(),
                list = state.segments,
                onSettle = onMoveSegment,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) { index, segment, isDragging ->
                key(segment.id) {
                    ReorderableItem {
                        val elevation by animateDpAsState(
                            targetValue = if (isDragging) 12.dp else 0.dp,
                            label = "trip_segment_elevation"
                        )
                        val scale by animateFloatAsState(
                            targetValue = if (isDragging) 1.03f else 1f,
                            label = "trip_segment_scale"
                        )

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .zIndex(if (isDragging) 1f else 0f),
                            color = Color.Transparent,
                            shadowElevation = elevation,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            TripSegmentCard(
                                index = index,
                                segment = segment,
                                canRemove = state.segments.size > 1,
                                onRemove = { onRemoveSegment(segment.id) },
                                dragHandleModifier =
                                    Modifier.longPressDraggableHandle(),
                                onOpenPlacePicker = onOpenPlacePicker,
                                onTransportTypeChange = onTransportTypeChange
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            PrimaryButton(
                text = "Dodaj kolejny odcinek",
                onClick = onAddSegment,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .widthIn(min = 190.dp)
                    .height(42.dp)
                    .testTag("trip_add_segment_button"),
                textColor = Black,
                border = BorderStroke(1.dp, Color.White)
            )

            state.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = DangerRed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .testTag("trip_error_message")
                )
            }

            PrimaryButton(
                text = "Zapisz trasę",
                onClick = onSaveTrip,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .testTag("trip_save_button"),
                enabled = !state.isSaving,
                isLoading = state.isSaving,
                textColor = Black,
                border = BorderStroke(1.dp, Color.White)
            )

            OutlinedButton(
                onClick = onBackClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .testTag("trip_cancel_button"),
                border = BorderStroke(1.dp, SurfaceGray),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextGray
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Anuluj")
            }
        }
    }

    dateField?.let { selectedField ->
        val initialDate = when (selectedField) {
            DateField.START -> state.startDateMillis
            DateField.END -> state.endDateMillis
        }

        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialDate
        )

        DatePickerDialog(
            onDismissRequest = { dateField = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            if (selectedField == DateField.START) {
                                onStartDateSelected(millis)
                            } else {
                                onEndDateSelected(millis)
                            }
                        }
                        dateField = null
                    }
                ) {
                    Text("Wybierz")
                }
            },
            dismissButton = {
                TextButton(onClick = { dateField = null }) {
                    Text("Anuluj")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (state.activePlaceSegmentId != null) {
        PlaceSearchDialog(
            state = state,
            onClose = onClosePlacePicker,
            onQueryChange = onPlaceQueryChange,
            onPlaceSelected = onPlaceSelected
        )
    }

    if (state.isSaved) {
        AlertDialog(
            onDismissRequest = onTripCreated,
            title = { Text("Podróż zapisana") },
            text = { Text("Nowa podróż została dodana.") },
            confirmButton = {
                TextButton(onClick = onTripCreated) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun TripSegmentCard(
    index: Int,
    segment: TripSegmentDraft,
    canRemove: Boolean,
    onRemove: () -> Unit,
    dragHandleModifier: Modifier,
    onOpenPlacePicker: (Long, Boolean) -> Unit,
    onTransportTypeChange: (Long, TripTransportType) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceGray)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Odcinek ${index + 1}",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            Surface(
                modifier = Modifier
                    .size(34.dp)
                    .then(dragHandleModifier)
                    .testTag("trip_segment_drag_handle_${segment.id}"),
                color = BackgroundDark,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, TextGray)
            ) {
                Text(
                    text = "=",
                    color = TextGray,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(start = 10.dp, top = 4.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        PlaceButton(
            text = segment.origin?.let { "${it.name}, ${it.countryName}" } ?: "Początek",
            tag = "trip_segment_${segment.id}_origin",
            onClick = { onOpenPlacePicker(segment.id, true) }
        )

        Spacer(Modifier.height(6.dp))

        PlaceButton(
            text = segment.destination?.let {
                "${it.name}, ${it.countryName}"
            } ?: "Koniec",
            tag = "trip_segment_${segment.id}_destination",
            onClick = { onOpenPlacePicker(segment.id, false) }
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = "Wybierz środek transportu",
            color = Color.White,
            fontSize = 14.sp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TripTransportType.entries.forEach { transport ->
                val selected = segment.transportType == transport
                val icon = when (transport) {
                    TripTransportType.CAR -> Icons.Default.DirectionsCar
                    TripTransportType.PLANE -> Icons.Default.Flight
                    TripTransportType.TRAIN -> Icons.Default.Train
                    TripTransportType.OTHER -> Icons.Default.AllInclusive
                }

                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            onTransportTypeChange(segment.id, transport)
                        }
                        .testTag(
                            "trip_transport_${segment.id}_${transport.name.lowercase()}"
                        ),
                    color = if (selected) BackgroundDark else SurfaceGray,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (selected) ButtonLight else BackgroundDark
                    )
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = transport.label,
                        tint = Color.White,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        if (canRemove) {
            Button(
                onClick = onRemove,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .testTag("trip_remove_segment_${segment.id}"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DangerRed,
                    contentColor = Black
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Usuń odcinek", color = Black)
            }
        }
    }
}

@Composable
private fun PlaceButton(
    text: String,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag(tag),
        color = BackgroundDark,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = text,
                color = if (text == "Początek" || text == "Koniec") {
                    TextGray
                } else {
                    Color.White
                },
                fontSize = 18.sp
            )
            Icon(
                imageVector = Icons.Default.AddLocation,
                contentDescription = "Wyszukaj miejsce",
                tint = ButtonLight
            )
        }
    }
}

@Composable
private fun DateButton(
    text: String,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag(tag),
        color = SurfaceGray,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                color = if (text == "od" || text == "do") TextGray else Color.White
            )
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = "Wybierz datę",
                tint = ButtonLight
            )
        }
    }
}

@Composable
private fun PlaceSearchDialog(
    state: TripManagerUiState,
    onClose: () -> Unit,
    onQueryChange: (String) -> Unit,
    onPlaceSelected: (PlaceOption) -> Unit
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Wybierz miejsce") },
        text = {
            Column {
                OutlinedTextField(
                    value = state.placeQuery,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("trip_place_search_field"),
                    placeholder = { Text("Wpisz nazwę miasta") },
                    singleLine = true
                )

                Spacer(Modifier.height(8.dp))

                when {
                    state.isSearchingPlaces -> Text("Wyszukiwanie…")
                    state.placeSearchError != null ->
                        Text(state.placeSearchError, color = DangerRed)
                    state.placeQuery.trim().length < 2 ->
                        Text("Wpisz co najmniej 2 znaki.")
                    state.placeResults.isEmpty() ->
                        Text("Brak pasujących miejsc.")
                    else -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            state.placeResults.forEach { place ->
                                Text(
                                    text = "${place.countryName} / ${place.name}",
                                    color = Color.White,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onPlaceSelected(place) }
                                        .padding(vertical = 12.dp)
                                        .testTag("trip_place_result_${place.id}")
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) {
                Text("Zamknij")
            }
        }
    )
}

@Composable
private fun tripFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = SurfaceGray,
    unfocusedContainerColor = SurfaceGray,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = ButtonLight,
    unfocusedBorderColor = SurfaceGray,
    cursorColor = ButtonLight
)

private fun formatDate(millis: Long): String {
    val formatter = SimpleDateFormat("dd.MM.yy", Locale("pl", "PL")).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return formatter.format(Date(millis))
}