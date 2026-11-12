package com.example.map.data.remote.dto

data class UserProfileDataDto(
    val userId: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val bio: String? = null,
    val isPublic: Boolean? = null
)

data class UserProfileResponseDto(
    val isSuccess: Boolean? = null,
    val message: String? = null,
    val errorCode: String? = null,
    val statusCode: Int? = null,
    val errors: List<String>? = null,
    val data: UserProfileDataDto? = null
)

data class EditUserNamesRequestDto(
    val firstName: String,
    val lastName: String
)
