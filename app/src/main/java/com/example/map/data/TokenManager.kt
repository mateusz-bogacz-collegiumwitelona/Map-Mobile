package com.example.map.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenManager(context: Context) : TokenStorage {

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context.applicationContext,
        PREFERENCES_NAME,
        MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun saveTokens(tokens: AuthTokens) {
        sharedPreferences.edit()
            .putString(ACCESS_TOKEN_KEY, tokens.accessToken)
            .putString(REFRESH_TOKEN_KEY, tokens.refreshToken)
            .apply()
    }

    override fun getTokens(): AuthTokens? {
        val accessToken = sharedPreferences.getString(ACCESS_TOKEN_KEY, null)
            ?: return null
        val refreshToken = sharedPreferences.getString(REFRESH_TOKEN_KEY, null)
            ?: return null

        return AuthTokens(
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }

    override fun clearTokens() {
        sharedPreferences.edit()
            .remove(ACCESS_TOKEN_KEY)
            .remove(REFRESH_TOKEN_KEY)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "secure_prefs"
        const val ACCESS_TOKEN_KEY = "ACCESS_TOKEN"
        const val REFRESH_TOKEN_KEY = "REFRESH_TOKEN"
    }
}