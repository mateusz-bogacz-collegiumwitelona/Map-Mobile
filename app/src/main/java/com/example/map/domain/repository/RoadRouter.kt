package com.example.map.domain.repository

import com.example.map.domain.model.GeoPoint
import com.example.map.domain.model.RoadRoute

interface RoadRouter {

    suspend fun route(origin: GeoPoint, destination: GeoPoint): RoadRoute?
}
