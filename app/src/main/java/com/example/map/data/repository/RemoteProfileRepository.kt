package com.example.map.data.repository

import com.example.map.data.remote.dto.ApiResultDto
import com.example.map.data.remote.dto.EditUserNamesRequestDto
import com.example.map.data.remote.dto.UserProfileDataDto
import com.example.map.data.remote.service.UserApiService
import com.example.map.domain.model.ProfileUpdateError
import com.example.map.domain.model.ProfileUpdateResult
import com.example.map.domain.model.UserProfile
import com.example.map.domain.repository.ProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject

class RemoteProfileRepository @Inject constructor(
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
        execute {
            userApiService.updateNames(
                EditUserNamesRequestDto(
                    firstName = firstName,
                    lastName = lastName
                )
            )
        }

    override suspend fun setVisibility(isPublic: Boolean): ProfileUpdateResult =
        execute {
            userApiService.setVisibility(isPublic)
        }

    private suspend fun execute(
        request: suspend () -> Response<ApiResultDto>
    ): ProfileUpdateResult {
        return try {
            val response = request()
            val body = response.body()

            if (response.isSuccessful && body?.isSuccess == true) {
                ProfileUpdateResult.Success
            } else {
                val errorBody = body ?: parseErrorBody(response)
                ProfileUpdateResult.Failure(
                    error = mapError(response.code(), errorBody?.errorCode),
                    message = errorBody?.message
                )
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: IOException) {
            ProfileUpdateResult.Failure(ProfileUpdateError.NETWORK)
        } catch (exception: Exception) {
            ProfileUpdateResult.Failure(
                error = ProfileUpdateError.UNKNOWN,
                message = exception.message
            )
        }
    }

    private fun parseErrorBody(response: Response<ApiResultDto>): ApiResultDto? {
        val rawBody = response.errorBody()?.string() ?: return null
        return runCatching {
            json.decodeFromString<ApiResultDto>(rawBody)
        }.getOrNull()
    }

    private fun UserProfileDataDto.toDomain(): UserProfile =
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
