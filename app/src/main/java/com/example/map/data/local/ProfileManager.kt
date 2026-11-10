package com.example.map.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.map.domain.repository.ProfileStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ProfileManager @Inject constructor(
    @ApplicationContext context: Context
) : ProfileStorage {

    private val sharedPreferences by lazy {
        val appContext = context.applicationContext

        EncryptedSharedPreferences.create(
            appContext,
            PREFERENCES_NAME,
            MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override fun getEmail(): String? =
        sharedPreferences.getString(EMAIL_KEY, null)

    override fun saveEmail(email: String) {
        sharedPreferences.edit()
            .putString(EMAIL_KEY, email)
            .apply()
    }

    override fun clear() {
        sharedPreferences.edit()
            .remove(EMAIL_KEY)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "profile_prefs"
        const val EMAIL_KEY = "EMAIL"
    }
}
