package com.example.map.data.repository

import com.example.map.data.remote.dto.common.ApiEnvelope
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import retrofit2.Response

    fun Response<*>.parseErrorEnvelope(json: Json): ApiEnvelope<Unit>? {
        val rawBody = errorBody()?.string()
        if (rawBody.isNullOrBlank()) return null;

        return runCatching {
            json.decodeFromString<ApiEnvelope<Unit>>(rawBody)
        }.getOrNull();
    }

    suspend fun <R> executeUnitApiCall(
        json: Json,
        request: suspend () -> Response<ApiEnvelope<Unit>>,
        onSuccess: () -> R,
        onApiError: (statusCode:Int, errorCode: String?, message: String?) -> R,
        onNetworkError: () -> R,
        onUnknownError: (message: String?) -> R
    ): R{
        return try {
            val response = request()
            val body = response.body()

            if (response.isSuccessful && body?.isSuccess == true) {
                onSuccess()
            } else {
                val errorBody = body ?: response.parseErrorEnvelope(json)
                onApiError(response.code(), errorBody?.errorCode, errorBody?.message)
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: IOException) {
            onNetworkError()
        } catch (exception: Exception) {
            onUnknownError(exception.message)
        }
    }