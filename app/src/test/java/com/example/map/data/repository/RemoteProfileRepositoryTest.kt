package com.example.map.data.repository

import com.example.map.data.remote.dto.ApiResultDto
import com.example.map.data.remote.dto.EditUserNamesRequestDto
import com.example.map.data.remote.dto.UserProfileDataDto
import com.example.map.data.remote.dto.UserProfileResponseDto
import com.example.map.data.remote.service.UserApiService
import com.example.map.domain.model.ProfileUpdateError
import com.example.map.domain.model.ProfileUpdateResult
import com.example.map.domain.model.UserProfile
import com.google.gson.Gson
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class RemoteProfileRepositoryTest {

    private val gson = Gson()
    private val service = FakeProfileApiService()
    private val repository = RemoteProfileRepository(service, gson)

    @Test
    fun updateNamesSendsTrimmedFreePayloadAndReturnsSuccess() = runTest {
        service.updateNamesResponse = Response.success(ApiResultDto(isSuccess = true))

        val result = repository.updateNames("Anna", "Nowak")

        assertEquals(ProfileUpdateResult.Success, result)
        assertEquals("Anna" to "Nowak", service.lastNames)
    }

    @Test
    fun setVisibilitySendsRequestedValueAndReturnsSuccess() = runTest {
        service.setVisibilityResponse = Response.success(ApiResultDto(isSuccess = true))

        val result = repository.setVisibility(isPublic = true)

        assertEquals(ProfileUpdateResult.Success, result)
        assertEquals(true, service.lastVisibility)
    }

    @Test
    fun mapsInvalidFirstNameErrorCode() = runTest {
        service.updateNamesResponse = errorResultResponse(
            status = 400,
            errorCode = "USR_001",
            message = "Invalid first name"
        )

        assertEquals(
            ProfileUpdateResult.Failure(
                ProfileUpdateError.FIRST_NAME_INVALID,
                "Invalid first name"
            ),
            repository.updateNames("A", "Nowak")
        )
    }

    @Test
    fun mapsInvalidLastNameErrorCode() = runTest {
        service.updateNamesResponse = errorResultResponse(
            status = 400,
            errorCode = "USR_002",
            message = "Invalid last name"
        )

        assertEquals(
            ProfileUpdateResult.Failure(
                ProfileUpdateError.LAST_NAME_INVALID,
                "Invalid last name"
            ),
            repository.updateNames("Anna", "N")
        )
    }

    @Test
    fun mapsNotFoundErrorCode() = runTest {
        service.setVisibilityResponse = errorResultResponse(
            status = 404,
            errorCode = "COMMON_002"
        )

        assertEquals(
            ProfileUpdateResult.Failure(ProfileUpdateError.NOT_FOUND),
            repository.setVisibility(isPublic = true)
        )
    }

    @Test
    fun mapsRateLimitErrorCode() = runTest {
        service.updateNamesResponse = errorResultResponse(
            status = 429,
            errorCode = "COMMON_005"
        )

        assertEquals(
            ProfileUpdateResult.Failure(ProfileUpdateError.RATE_LIMITED),
            repository.updateNames("Anna", "Nowak")
        )
    }

    @Test
    fun mapsUnauthorizedStatus() = runTest {
        service.updateNamesResponse = errorResultResponse(status = 401)

        assertEquals(
            ProfileUpdateResult.Failure(ProfileUpdateError.UNAUTHORIZED),
            repository.updateNames("Anna", "Nowak")
        )
    }

    @Test
    fun mapsForbiddenStatus() = runTest {
        service.setVisibilityResponse = errorResultResponse(status = 403)

        assertEquals(
            ProfileUpdateResult.Failure(ProfileUpdateError.UNAUTHORIZED),
            repository.setVisibility(isPublic = false)
        )
    }

    @Test
    fun mapsNotFoundStatusWithoutErrorCode() = runTest {
        service.setVisibilityResponse = errorResultResponse(status = 404)

        assertEquals(
            ProfileUpdateResult.Failure(ProfileUpdateError.NOT_FOUND),
            repository.setVisibility(isPublic = true)
        )
    }

    @Test
    fun mapsRateLimitStatusWithoutErrorCode() = runTest {
        service.updateNamesResponse = errorResultResponse(status = 429)

        assertEquals(
            ProfileUpdateResult.Failure(ProfileUpdateError.RATE_LIMITED),
            repository.updateNames("Anna", "Nowak")
        )
    }

    @Test
    fun mapsUnknownFailureWithBackendMessage() = runTest {
        service.updateNamesResponse = errorResultResponse(
            status = 500,
            message = "Server error"
        )

        assertEquals(
            ProfileUpdateResult.Failure(ProfileUpdateError.UNKNOWN, "Server error"),
            repository.updateNames("Anna", "Nowak")
        )
    }

    @Test
    fun mapsNetworkFailure() = runTest {
        service.updateNamesException = IOException("offline")

        assertEquals(
            ProfileUpdateResult.Failure(ProfileUpdateError.NETWORK),
            repository.updateNames("Anna", "Nowak")
        )
    }

    @Test
    fun parsesErrorBodyWhenResultIsDeliveredAsErrorResponse() = runTest {
        val body = gson.toJson(
            ApiResultDto(
                isSuccess = false,
                message = "Invalid first name",
                errorCode = "USR_001"
            )
        )
        service.updateNamesResponse = Response.error(
            400,
            body.toResponseBody("application/json".toMediaType())
        )

        assertEquals(
            ProfileUpdateResult.Failure(
                ProfileUpdateError.FIRST_NAME_INVALID,
                "Invalid first name"
            ),
            repository.updateNames("A", "Nowak")
        )
    }

    @Test
    fun getProfileMapsSuccessfulResponse() = runTest {
        val result = repository.getProfile()

        assertEquals(
            UserProfile(firstName = "Jan", lastName = "Kowalski", isPublic = true),
            result
        )
    }

    @Test
    fun getProfileReturnsNullWhenResponseIsUnsuccessful() = runTest {
        service.profileResponse = Response.success(
            UserProfileResponseDto(isSuccess = false, errorCode = "COMMON_002")
        )

        assertNull(repository.getProfile())
    }

    @Test
    fun getProfileReturnsNullOnHttpError() = runTest {
        service.profileResponse = errorProfileResponse(status = 401)

        assertNull(repository.getProfile())
    }

    @Test
    fun getProfileReturnsNullOnNetworkFailure() = runTest {
        service.profileException = IOException("offline")

        assertNull(repository.getProfile())
    }

    private fun errorResultResponse(
        status: Int,
        errorCode: String? = null,
        message: String? = null
    ): Response<ApiResultDto> {
        val body = gson.toJson(
            ApiResultDto(
                isSuccess = false,
                message = message,
                errorCode = errorCode
            )
        )
        return Response.error(
            status,
            body.toResponseBody("application/json".toMediaType())
        )
    }

    private fun errorProfileResponse(status: Int): Response<UserProfileResponseDto> {
        val body = gson.toJson(
            UserProfileResponseDto(
                isSuccess = false,
                message = "Session expired",
                errorCode = "ACCESS_DENIED"
            )
        )
        return Response.error(
            status,
            body.toResponseBody("application/json".toMediaType())
        )
    }
}

private class FakeProfileApiService : UserApiService {

    var profileResponse: Response<UserProfileResponseDto> = Response.success(
        UserProfileResponseDto(
            isSuccess = true,
            data = UserProfileDataDto(
                userId = "test-user-id",
                firstName = "Jan",
                lastName = "Kowalski",
                bio = null,
                isPublic = true
            )
        )
    )
    var updateNamesResponse: Response<ApiResultDto> =
        Response.success(ApiResultDto(isSuccess = true))
    var setVisibilityResponse: Response<ApiResultDto> =
        Response.success(ApiResultDto(isSuccess = true))

    var profileException: Exception? = null
    var updateNamesException: Exception? = null
    var setVisibilityException: Exception? = null

    var lastNames: Pair<String, String>? = null
    var lastVisibility: Boolean? = null

    override suspend fun getProfile(): Response<UserProfileResponseDto> {
        profileException?.let { throw it }
        return profileResponse
    }

    override suspend fun updateNames(
        request: EditUserNamesRequestDto
    ): Response<ApiResultDto> {
        updateNamesException?.let { throw it }
        lastNames = request.firstName to request.lastName
        return updateNamesResponse
    }

    override suspend fun setVisibility(isPublic: Boolean): Response<ApiResultDto> {
        setVisibilityException?.let { throw it }
        lastVisibility = isPublic
        return setVisibilityResponse
    }
}
