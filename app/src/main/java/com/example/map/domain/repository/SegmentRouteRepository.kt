package com.example.map.domain.repository

import com.example.map.domain.model.RouteGeometry
import com.example.map.domain.model.SegmentRouteSaveResult

interface SegmentRouteRepository {

    suspend fun saveRoute(segmentId: String, route: RouteGeometry): SegmentRouteSaveResult
}
