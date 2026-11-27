package com.example.map.data.repository

import com.example.map.data.remote.dto.common.ApiEnvelope
import com.example.map.data.remote.dto.request.ChangeEmailRequest
import com.example.map.data.remote.dto.request.ConfirmEmailChangeRequest
import com.example.map.data.remote.dto.request.EditUserNamesRequest
import com.example.map.data.remote.dto.response.UserProfileDataResponse
import com.example.map.data.remote.service.UserApiService
import com.example.map.domain.model.user.ProfileUpdateError
import com.example.map.domain.model.user.ProfileUpdateResult
import com.example.map.domain.model.user.UserProfile
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class RemoteProfileRepositoryTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val service = FakeProfileApiService()
    private val repository = ProfileRepositoryImpl(service, json)

    @Test
    fun updateNamesSendsTrimmedFreePayloadAndReturnsSuccess() = runTest {
        service.updateNamesResponse = Response.success(ApiEnvelope<Unit>(isSuccess = true))

        val result = repository.updateNames("Anna", "Nowak")

        assertEquals(ProfileUpdateResult.Success, result)
        assertEquals("Anna" to "Nowak", service.lastNames)
    }

    @Test
    fun setVisibilitySendsRequestedValueAndReturnsSuccess() = runTest {
        service.setVisibilityResponse = Response.success(ApiEnvelope<Unit>(isSuccess = true))

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
        val body = json.encodeToString(
            ApiEnvelope<Unit>(
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
    fun getProfileNormalizesMissingNamesToEmptyStrings() = runTest {
        service.profileResponse = Response.success(
            ApiEnvelope(
                isSuccess = true,
                data = UserProfileDataResponse(
                    userId = "test-user-id",
                    firstName = null,
                    lastName = null,
                    bio = null,
                    isPublic = null
                )
            )
        )

        assertEquals(UserProfile(), repository.getProfile())
    }

    @Test
    fun getProfileReturnsNullWhenResponseIsUnsuccessful() = runTest {
        service.profileResponse = Response.success(
            ApiEnvelope(isSuccess = false, errorCode = "COMMON_002")
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
    ): Response<ApiEnvelope<Unit>> {
        val body = json.encodeToString(
            ApiEnvelope<Unit>(
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

    private fun errorProfileResponse(
        status: Int
    ): Response<ApiEnvelope<UserProfileDataResponse>> {
        val body = json.encodeToString(
            ApiEnvelope<UserProfileDataResponse>(
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

    var profileResponse: Response<ApiEnvelope<UserProfileDataResponse>> =
        Response.success(
            ApiEnvelope(
                isSuccess = true,
                data = UserProfileDataResponse(
                    userId = "test-user-id",
                    firstName = "Jan",
                    lastName = "Kowalski",
                    bio = null,
                    isPublic = true
                )
            )
        )
    var updateNamesResponse: Response<ApiEnvelope<Unit>> =
        Response.success(ApiEnvelope<Unit>(isSuccess = true))
    var setVisibilityResponse: Response<ApiEnvelope<Unit>> =
        Response.success(ApiEnvelope<Unit>(isSuccess = true))

    var profileException: Exception? = null
    var updateNamesException: Exception? = null
    var setVisibilityException: Exception? = null

    var lastNames: Pair<String, String>? = null
    var lastVisibility: Boolean? = null

    override suspend fun getProfile(): Response<ApiEnvelope<UserProfileDataResponse>> {
        profileException?.let { throw it }
        return profileResponse
    }

    override suspend fun updateNames(
        request: EditUserNamesRequest
    ): Response<ApiEnvelope<Unit>> {
        updateNamesException?.let { throw it }
        lastNames = request.firstName to request.lastName
        return updateNamesResponse
    }

    override suspend fun setVisibility(isPublic: Boolean): Response<ApiEnvelope<Unit>> {
        setVisibilityException?.let { throw it }
        lastVisibility = isPublic
        return setVisibilityResponse
    }

    override suspend fun requestEmailChange(
        request: ChangeEmailRequest
    ): Response<ApiEnvelope<Unit>> {
        error("Nie używane w testach profilu.")
    }

    override suspend fun confirmEmailChange(
        request: ConfirmEmailChangeRequest
    ): Response<ApiEnvelope<Unit>> {
        error("Nie używane w testach profilu.")
    }

    override suspend fun deleteAccount(): Response<ApiEnvelope<Unit>> {
        error("Nie używane w testach profilu.")
    }
}
