package com.example.map.data.repository

import com.example.map.data.remote.dto.OsrmRouteResponseDto
import com.example.map.data.remote.service.OsrmApiService
import com.example.map.domain.model.GeoPoint
import com.example.map.domain.model.RoadRoute
import com.example.map.domain.repository.RoadRouter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

class OsrmRoadRouter internal constructor(
    private val osrmApiService: OsrmApiService,
    private val currentTimeMillis: () -> Long
) : RoadRouter {

    @Inject
    constructor(osrmApiService: OsrmApiService) : this(osrmApiService, System::currentTimeMillis)

    private val mutex = Mutex()
    private var lastRequestAt: Long? = null

    private val cache = object : LinkedHashMap<Pair<GeoPoint, GeoPoint>, RoadRoute>(16, 0.75f, true) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<Pair<GeoPoint, GeoPoint>, RoadRoute>?
        ): Boolean = size > CACHE_SIZE
    }

    override suspend fun route(origin: GeoPoint, destination: GeoPoint): RoadRoute? =
        mutex.withLock {
            val key = origin to destination

            cache[key]?.let { return@withLock it }

            waitForRequestSlot()

            fetch(origin, destination)?.also { route -> cache[key] = route }
        }

    private suspend fun waitForRequestSlot() {
        lastRequestAt?.let { last ->
            val waitMillis = last + MIN_REQUEST_INTERVAL_MS - currentTimeMillis()
            if (waitMillis > 0) {
                delay(waitMillis)
            }
        }
        lastRequestAt = currentTimeMillis()
    }

    private suspend fun fetch(origin: GeoPoint, destination: GeoPoint): RoadRoute? {
        return try {
            val response = osrmApiService.route("${origin.toOsrm()};${destination.toOsrm()}")
            val body = response.body()

            if (response.isSuccessful && body?.code == OK_CODE) {
                body.toRoadRoute()
            } else {
                null
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            null
        }
    }

    private fun OsrmRouteResponseDto.toRoadRoute(): RoadRoute? {
        val route = routes?.firstOrNull() ?: return null
        val distanceMeters = route.distance ?: return null
        val coordinates = route.geometry?.coordinates ?: return null

        val isSnappedTooFar = waypoints.orEmpty().any { waypoint ->
            (waypoint.distance ?: 0.0) > MAX_ENDPOINT_SNAP_KM * METERS_IN_KM
        }

        if (isSnappedTooFar || !distanceMeters.isFinite() || distanceMeters < 0.0) {
            return null
        }

        val points = coordinates.map { coordinate ->
            if (coordinate.size < 2) return null
            runCatching {
                GeoPoint(latitude = coordinate[1], longitude = coordinate[0])
            }.getOrNull() ?: return null
        }

        if (points.size < 2) {
            return null
        }

        return RoadRoute(
            points = points.limitTo(MAX_ROUTE_POINTS),
            distanceKm = distanceMeters / METERS_IN_KM
        )
    }

    private fun GeoPoint.toOsrm(): String =
        String.format(Locale.ROOT, "%.6f,%.6f", longitude, latitude)

    private fun List<GeoPoint>.limitTo(maxPoints: Int): List<GeoPoint> {
        if (size <= maxPoints) {
            return this
        }

        val step = (size - 1).toDouble() / (maxPoints - 1)
        return List(maxPoints) { index -> this[(index * step).roundToInt()] }
    }

    companion object {
        const val MAX_ROUTE_POINTS = 10_000
        const val MAX_ENDPOINT_SNAP_KM = 10.0
        const val MIN_REQUEST_INTERVAL_MS = 1_000L

        private const val CACHE_SIZE = 50
        private const val METERS_IN_KM = 1000.0
        private const val OK_CODE = "Ok"
    }
}
