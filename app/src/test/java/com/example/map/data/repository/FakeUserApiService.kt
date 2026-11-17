package com.example.map.data.repository

import com.example.map.data.remote.dto.ApiResultDto
import com.example.map.data.remote.dto.ChangeEmailRequestDto
import com.example.map.data.remote.dto.ConfirmEmailChangeRequestDto
import com.example.map.data.remote.service.UserApiService
import retrofit2.Response
import com.example.map.data.remote.dto.ApiEnvelopeDto
import com.example.map.data.remote.dto.UserProfileDto

internal class FakeUserApiService : UserApiService {

    var requestResponse: Response<ApiResultDto> =
        Response.success(ApiResultDto(isSuccess = true))
    var confirmResponse: Response<ApiResultDto> =
        Response.success(ApiResultDto(isSuccess = true))
    var deleteResponse: Response<ApiResultDto> =
        Response.success(ApiResultDto(isSuccess = true))

    var requestException: Exception? = null
    var confirmException: Exception? = null
    var deleteException: Exception? = null

    var profileResponse: Response<ApiEnvelopeDto<UserProfileDto>> =
        Response.success(
            ApiEnvelopeDto(
                isSuccess = true,
                data = UserProfileDto(
                    firstName = "Jan",
                    lastName = "Kowalski"
                )
            )
        )

    override suspend fun requestEmailChange(
        request: ChangeEmailRequestDto
    ): Response<ApiResultDto> {
        requestException?.let { throw it }
        return requestResponse
    }

    override suspend fun confirmEmailChange(
        request: ConfirmEmailChangeRequestDto
    ): Response<ApiResultDto> {
        confirmException?.let { throw it }
        return confirmResponse
    }

    override suspend fun deleteAccount(): Response<ApiResultDto> {
        deleteException?.let { throw it }
        return deleteResponse
    }

    override suspend fun getCurrentUserProfile():
            Response<ApiEnvelopeDto<UserProfileDto>> = profileResponse
}