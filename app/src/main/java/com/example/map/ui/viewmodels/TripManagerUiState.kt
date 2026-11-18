package com.example.map.ui.viewmodels

import com.example.map.domain.model.PlaceOption
import com.example.map.domain.model.TripSegmentDraft

data class TripManagerUiState(
    val title: String = "",
    val description: String = "",
    val startDateMillis: Long? = null,
    val hasEndDate: Boolean = false,
    val endDateMillis: Long? = null,
    val segments: List<TripSegmentDraft> = listOf(TripSegmentDraft(id = 1)),
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
    val activePlaceSegmentId: Long? = null,
    val activePlaceIsOrigin: Boolean = true,
    val placeQuery: String = "",
    val placeResults: List<PlaceOption> = emptyList(),
    val isSearchingPlaces: Boolean = false,
    val placeSearchError: String? = null
) {
    val validationError: String?
        get() {
            if (title.isBlank()) return "Wpisz nazwę podróży."
            if (title.trim().length > 200) {
                return "Nazwa podróży może mieć maksymalnie 200 znaków."
            }
            if (description.length > 2000) {
                return "Opis może mieć maksymalnie 2000 znaków."
            }

            val startDate = startDateMillis
                ?: return "Wybierz datę rozpoczęcia."

            if (hasEndDate && endDateMillis == null) {
                return "Wybierz datę zakończenia albo wyłącz tę opcję."
            }

            if (hasEndDate && endDateMillis!! < startDate) {
                return "Data zakończenia nie może być wcześniejsza niż rozpoczęcia."
            }

            if (segments.isEmpty()) {
                return "Dodaj co najmniej jeden odcinek."
            }

            if (segments.any { it.origin == null || it.destination == null }) {
                return "Wybierz początek i koniec każdego odcinka."
            }

            for (index in 1 until segments.size) {
                val previous = segments[index - 1].destination!!
                val current = segments[index].origin!!

                if (
                    kotlin.math.abs(previous.latitude - current.latitude) > 0.000001 ||
                    kotlin.math.abs(previous.longitude - current.longitude) > 0.000001
                ) {
                    return "Kolejne odcinki muszą łączyć się w tym samym miejscu."
                }
            }

            return null
        }
}