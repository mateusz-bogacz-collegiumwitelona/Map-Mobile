package com.example.map.data.remote.dto.common

import kotlinx.serialization.Serializable

@Serializable
data class PagedResponse<T>(
    val items: List<T> = emptyList(),
    val pageNumber: Int = 1,
    val pageSize: Int = 10,
    val totalCount: Int = 0,
    val totalPages: Int = 0,
    val hasPreviousPage: Boolean = false,
    val hasNextPage: Boolean = false
)