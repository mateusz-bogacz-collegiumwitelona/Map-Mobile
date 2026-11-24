package com.example.map.data.repository

import com.example.map.data.remote.dto.common.ApiEnvelope
import com.example.map.data.remote.dto.request.EditUserNamesRequest
import com.example.map.data.remote.dto.response.UserProfileDataResponse
import com.example.map.data.remote.service.UserApiService
import com.example.map.domain.model.ProfileUpdateError
import com.example.map.domain.model.ProfileUpdateResult
import com.example.map.domain.model.UserProfile
import com.example.map.domain.repository.ProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import retrofit2.Response
import javax.inject.Inject

class ProfileRepositoryImpl @Inject constructor(
    private val userApiService: UserApiService,
    private val json: Json
) : ProfileRepository {

    override suspend fun getProfile(): UserProfile? {
        return try {
            val response = userApiService.getProfile()
            val body = response.body()

            if (response.isSuccessful && body?.isSuccess == true) {
                body.data?.toDomain()
            } else {
                null
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun updateNames(
        firstName: String,
        lastName: String
    ): ProfileUpdateResult =
        executeProfileUpdate {
            userApiService.updateNames(
                EditUserNamesRequest(
                    firstName = firstName,
                    lastName = lastName
                )
            )
        }

    override suspend fun setVisibility(isPublic: Boolean): ProfileUpdateResult =
        executeProfileUpdate {
            userApiService.setVisibility(isPublic)
        }

    private suspend fun executeProfileUpdate(
        request: suspend () -> Response<ApiEnvelope<Unit>>
    ): ProfileUpdateResult =
        executeUnitApiCall(
            json = json,
            request = request,
            onSuccess = { ProfileUpdateResult.Success },
            onApiError = { status, code, msg ->
                ProfileUpdateResult.Failure(mapError(status, code), msg)
            },
            onNetworkError = { ProfileUpdateResult.Failure(ProfileUpdateError.NETWORK) },
            onUnknownError = { msg -> ProfileUpdateResult.Failure(ProfileUpdateError.UNKNOWN, msg) }
        )

    private fun UserProfileDataResponse.toDomain(): UserProfile =
        UserProfile(
            firstName = firstName.orEmpty(),
            lastName = lastName.orEmpty(),
            isPublic = isPublic ?: false
        )

    private fun mapError(statusCode: Int, errorCode: String?): ProfileUpdateError =
        when (errorCode) {
            "USR_001" -> ProfileUpdateError.FIRST_NAME_INVALID
            "USR_002" -> ProfileUpdateError.LAST_NAME_INVALID
            "COMMON_002" -> ProfileUpdateError.NOT_FOUND
            "COMMON_005" -> ProfileUpdateError.RATE_LIMITED
            else -> when (statusCode) {
                401, 403 -> ProfileUpdateError.UNAUTHORIZED
                404 -> ProfileUpdateError.NOT_FOUND
                429 -> ProfileUpdateError.RATE_LIMITED
                else -> ProfileUpdateError.UNKNOWN
            }
        }
}