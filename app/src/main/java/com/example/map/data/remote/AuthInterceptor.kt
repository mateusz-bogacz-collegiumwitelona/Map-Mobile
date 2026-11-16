package com.example.map.data.remote

import com.example.map.domain.repository.TokenStorage
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenStorage: TokenStorage
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val accessToken = tokenStorage.getAccessToken()

        val shouldAttachToken = !accessToken.isNullOrBlank() &&
                !request.url.encodedPath.contains(PUBLIC_AUTH_PATH)

        if (!shouldAttachToken) {
            return chain.proceed(request)
        }

        return chain.proceed(
            request.newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .build()
        )
    }

    private companion object {
        const val PUBLIC_AUTH_PATH = "/api/auth/"
    }
}
