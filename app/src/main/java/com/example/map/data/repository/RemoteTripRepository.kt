package com.example.map.data.repository

import com.example.map.data.remote.dto.ApiEnvelopeDto
import com.example.map.data.remote.dto.PlaceSearchDto
import com.example.map.data.remote.dto.TripDetailsDto
import com.example.map.data.remote.dto.TripPointRequestDto
import com.example.map.data.remote.dto.TripSegmentRequestDto
import com.example.map.data.remote.dto.TripSummaryDto
import com.example.map.data.remote.dto.UpsertTripRequestDto
import com.example.map.data.remote.service.TripApi
import com.example.map.domain.model.PlaceOption
import com.example.map.domain.model.TripDetails
import com.example.map.domain.model.TripDraft
import com.example.map.domain.model.TripSegmentDraft
import com.example.map.domain.model.TripSummary
import com.example.map.domain.model.TripTransportType
import com.example.map.domain.repository.TripRepository
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import com.example.map.domain.model.TripSegmentDetails

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

    override suspend fun getTrips(
        searchTerm: String?,
        sortBy: String,
        sortDescending: Boolean
    ): List<TripSummary> {
        val response = tripApi.getTrips(
            sortBy = sortBy,
            sortDescending = sortDescending,
            searchTerm = searchTerm
        )
        val body = response.body()

        if (!response.isSuccessful || body?.isSuccess != true || body.data == null) {
            throw IOException(
                body?.message
                    ?: readErrorMessage(response.errorBody()?.string())
                    ?: "Nie udało się pobrać podróży."
            )
        }

        return body.data.items.map { it.toDomain() }
    }

    override suspend fun getTripDetails(tripId: String): TripDetails {
        val response = tripApi.getTripDetails(tripId)
        val body = response.body()

        if (!response.isSuccessful || body?.isSuccess != true || body.data == null) {
            throw IOException(
                body?.message
                    ?: readErrorMessage(response.errorBody()?.string())
                    ?: "Nie udało się pobrać szczegółów podróży."
            )
        }

        return body.data.toDomain()
    }

    override suspend fun createTrip(trip: TripDraft) {
        val response = tripApi.createTrip(trip.toRequest())
        ensureUpsertSucceeded(response.body(), response.errorBody()?.string())
    }

    override suspend fun updateTrip(tripId: String, trip: TripDraft) {
        val response = tripApi.updateTrip(tripId, trip.toRequest())
        ensureUpsertSucceeded(response.body(), response.errorBody()?.string())
    }

    override suspend fun deleteTrip(tripId: String) {
        val response = tripApi.deleteTrip(tripId)
        val body = response.body()

        if (!response.isSuccessful || body?.isSuccess != true) {
            throw IOException(
                body?.message
                    ?: readErrorMessage(response.errorBody()?.string())
                    ?: "Nie udało się usunąć podróży."
            )
        }
    }

    private fun TripDraft.toRequest() = UpsertTripRequestDto(
        title = title,
        description = description,
        startDate = formatUtc(startDateMillis),
        endDate = endDateMillis?.let(::formatUtc),
        segments = segments.map { segment ->
            TripSegmentRequestDto(
                origin = requireNotNull(segment.origin).toRequest(),
                destination = requireNotNull(segment.destination).toRequest(),
                transportType = segment.transportType.apiValue
            )
        }
    )

    private fun ensureUpsertSucceeded(
        body: ApiEnvelopeDto<JsonObject>?,
        rawErrorBody: String?
    ) {
        if (body?.isSuccess != true) {
            throw IOException(
                body?.message
                    ?: readErrorMessage(rawErrorBody)
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

    private fun TripSummaryDto.toDomain() = TripSummary(
        id = id,
        title = title,
        startDateMillis = parseUtc(startDate),
        endDateMillis = endDate?.let(::parseUtc),
        totalDistanceKm = totalDistance
    )

    private fun TripDetailsDto.toDomain(): TripDetails {
        val orderedSegments = segments.sortedBy { it.orderIndex }

        val draft = TripDraft(
            title = title,
            description = description,
            startDateMillis = parseUtc(startDate),
            endDateMillis = endDate?.let(::parseUtc),
            segments = orderedSegments.mapIndexed { index, segment ->
                TripSegmentDraft(
                    id = (index + 1).toLong(),
                    origin = segment.origin.toPlaceOption("origin-$index"),
                    destination = segment.destination.toPlaceOption("destination-$index"),
                    transportType = TripTransportType.entries.firstOrNull {
                        it.apiValue.equals(segment.transportType, ignoreCase = true)
                    } ?: TripTransportType.OTHER
                )
            }
        )

        val segmentDetails = orderedSegments.map { segment ->
            TripSegmentDetails(
                orderIndex = segment.orderIndex,
                originName = segment.origin.name,
                destinationName = segment.destination.name,
                transportType = TripTransportType.entries.firstOrNull {
                    it.apiValue.equals(segment.transportType, ignoreCase = true)
                } ?: TripTransportType.OTHER,
                distanceKm = segment.distanceKm,
                isFallbackRoute = segment.isFallbackRoute
            )
        }

        return TripDetails(
            id = id,
            draft = draft,
            segmentDetails = segmentDetails
        )
    }

    private fun com.example.map.data.remote.dto.TripPointResponseDto.toPlaceOption(
        pointId: String
    ) = PlaceOption(
        id = pointId,
        name = name,
        countryName = "",
        countryCode = "",
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

    private fun parseUtc(value: String): Long {
        if (!value.endsWith("Z")) {
            throw IOException("Nieprawidłowa data otrzymana z serwera.")
        }

        val valueWithoutZone = value.dropLast(1)
        val separatorIndex = valueWithoutZone.indexOf('.')
        val datePart = if (separatorIndex >= 0) {
            valueWithoutZone.substring(0, separatorIndex)
        } else {
            valueWithoutZone
        }
        val fraction = if (separatorIndex >= 0) {
            valueWithoutZone.substring(separatorIndex + 1)
                .padEnd(3, '0')
                .take(3)
        } else {
            "000"
        }

        val normalized = "$datePart.$fraction" + "Z"
        val formatter = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            Locale.US
        ).apply {
            timeZone = TimeZone.getTimeZone("UTC")
            isLenient = false
        }

        return formatter.parse(normalized)?.time
            ?: throw IOException("Nieprawidłowa data otrzymana z serwera.")
    }

    private fun readErrorMessage(rawBody: String?): String? {
        if (rawBody.isNullOrBlank()) return null

        return runCatching {
            json.decodeFromString<ApiEnvelopeDto<JsonObject>>(rawBody).message
        }.getOrNull()
    }
}