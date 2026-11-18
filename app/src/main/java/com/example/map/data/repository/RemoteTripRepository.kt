package com.example.map.data.repository

import com.example.map.data.remote.dto.ApiEnvelopeDto
import com.example.map.data.remote.dto.PlaceSearchDto
import com.example.map.data.remote.dto.TripPointRequestDto
import com.example.map.data.remote.dto.TripSegmentRequestDto
import com.example.map.data.remote.dto.UpsertTripRequestDto
import com.example.map.data.remote.service.TripApi
import com.example.map.domain.model.PlaceOption
import com.example.map.domain.model.TripDraft
import com.example.map.domain.repository.TripRepository
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

class RemoteTripRepository @Inject constructor(
    private val tripApi: TripApi,
    private val json: Json
) : TripRepository {

    override suspend fun searchPlaces(query: String): List<PlaceOption> {
        val response = tripApi.searchPlaces(query)
        val body = response.body()

        if (!response.isSuccessful || body?.isSuccess != true || body.data == null) {
            throw IOException(
                body?.message
                    ?: readErrorMessage(response.errorBody()?.string())
                    ?: "Nie udało się wyszukać miejsc."
            )
        }

        return body.data.map { it.toDomain() }
    }

    override suspend fun createTrip(trip: TripDraft) {
        val request = UpsertTripRequestDto(
            title = trip.title,
            description = trip.description,
            startDate = formatUtc(trip.startDateMillis),
            endDate = trip.endDateMillis?.let(::formatUtc),
            segments = trip.segments.map { segment ->
                TripSegmentRequestDto(
                    origin = segment.origin!!.toRequest(),
                    destination = segment.destination!!.toRequest(),
                    transportType = segment.transportType.apiValue
                )
            }
        )

        val response = tripApi.createTrip(request)
        val body = response.body()

        if (!response.isSuccessful || body?.isSuccess != true) {
            throw IOException(
                body?.message
                    ?: readErrorMessage(response.errorBody()?.string())
                    ?: "Nie udało się zapisać podróży."
            )
        }
    }

    private fun PlaceSearchDto.toDomain() = PlaceOption(
        id = id,
        name = name,
        countryName = countryName,
        countryCode = countryCode,
        latitude = latitude,
        longitude = longitude
    )

    private fun PlaceOption.toRequest() = TripPointRequestDto(
        name = name,
        latitude = latitude,
        longitude = longitude
    )

    private fun formatUtc(millis: Long): String {
        val formatter = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            Locale.US
        ).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        return formatter.format(Date(millis))
    }

    private fun readErrorMessage(rawBody: String?): String? {
        if (rawBody.isNullOrBlank()) return null

        return runCatching {
            json.decodeFromString<ApiEnvelopeDto<JsonObject>>(rawBody).message
        }.getOrNull()
    }
}