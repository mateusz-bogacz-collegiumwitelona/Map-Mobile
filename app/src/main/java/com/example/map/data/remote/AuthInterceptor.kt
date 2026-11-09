package com.example.map.data.remote

import com.example.map.domain.repository.TokenStorage
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenStorage: TokenStorage
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val accessToken = tokenStorage.getAccessToken()

        val request = if (accessToken.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request()
                .newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .build()
        }

        return chain.proceed(request)
    }
}
