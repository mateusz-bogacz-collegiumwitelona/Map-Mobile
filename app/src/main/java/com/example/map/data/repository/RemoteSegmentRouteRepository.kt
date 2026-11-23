package com.example.map.data.repository

import com.example.map.data.remote.dto.ApiResultDto
import com.example.map.data.remote.dto.RoutePointDto
import com.example.map.data.remote.dto.UpdateSegmentRouteRequestDto
import com.example.map.data.remote.service.TripApiService
import com.example.map.domain.model.RouteGeometry
import com.example.map.domain.model.SegmentRouteSaveError
import com.example.map.domain.model.SegmentRouteSaveResult
import com.example.map.domain.repository.SegmentRouteRepository
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import kotlin.math.roundToLong

class RemoteSegmentRouteRepository @Inject constructor(
    private val tripApiService: TripApiService,
    private val json: Json
) : SegmentRouteRepository {

    override suspend fun saveRoute(
        segmentId: String,
        route: RouteGeometry
    ): SegmentRouteSaveResult {
        val request = route.toRequest()

        if (request.coordinates.distinct().size < MIN_DISTINCT_POINTS ||
            request.distanceKm < MIN_DISTANCE_KM
        ) {
            return SegmentRouteSaveResult.Failure(SegmentRouteSaveError.INVALID_ROUTE)
        }

        return try {
            val response = tripApiService.updateSegmentRoute(segmentId, request)
            val body = response.body()

            if (response.isSuccessful && body?.isSuccess == true) {
                SegmentRouteSaveResult.Success
            } else {
                val errorBody = body ?: parseErrorBody(response)
                SegmentRouteSaveResult.Failure(
                    error = mapError(response.code(), errorBody?.errorCode),
                    message = errorBody?.message
                )
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: IOException) {
            SegmentRouteSaveResult.Failure(SegmentRouteSaveError.NETWORK)
        } catch (exception: Exception) {
            SegmentRouteSaveResult.Failure(
                error = SegmentRouteSaveError.UNKNOWN,
                message = exception.message
            )
        }
    }

    private fun RouteGeometry.toRequest(): UpdateSegmentRouteRequestDto =
        UpdateSegmentRouteRequestDto(
            coordinates = points.map { point ->
                RoutePointDto(latitude = point.latitude, longitude = point.longitude)
            },
            distanceKm = (distanceKm * 100).roundToLong() / 100.0,
            isFallbackRoute = isFallbackRoute
        )

    private fun parseErrorBody(response: Response<ApiResultDto>): ApiResultDto? {
        val rawBody = response.errorBody()?.string() ?: return null
        return runCatching {
            json.decodeFromString<ApiResultDto>(rawBody)
        }.getOrNull()
    }

    private fun mapError(statusCode: Int, errorCode: String?): SegmentRouteSaveError =
        when (errorCode) {
            "TRIP_008", "TRIP_009", "TRIP_010", "VAL_001" -> SegmentRouteSaveError.INVALID_ROUTE
            "COMMON_002" -> SegmentRouteSaveError.NOT_FOUND
            "COMMON_005" -> SegmentRouteSaveError.RATE_LIMITED
            else -> when (statusCode) {
                400 -> SegmentRouteSaveError.INVALID_ROUTE
                401, 403 -> SegmentRouteSaveError.UNAUTHORIZED
                404 -> SegmentRouteSaveError.NOT_FOUND
                429 -> SegmentRouteSaveError.RATE_LIMITED
                else -> SegmentRouteSaveError.UNKNOWN
            }
        }

    private companion object {
        const val MIN_DISTINCT_POINTS = 2
        const val MIN_DISTANCE_KM = 0.01
    }
}
