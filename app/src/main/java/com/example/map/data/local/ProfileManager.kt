package com.example.map.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.map.domain.model.UserProfile
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

    override fun getProfile(): UserProfile? {
        if (!sharedPreferences.contains(EMAIL_KEY) &&
            !sharedPreferences.contains(FIRST_NAME_KEY) &&
            !sharedPreferences.contains(LAST_NAME_KEY) &&
            !sharedPreferences.contains(IS_PUBLIC_KEY)
        ) {
            return null
        }

        return UserProfile(
            email = getEmail().orEmpty(),
            firstName = sharedPreferences.getString(FIRST_NAME_KEY, null).orEmpty(),
            lastName = sharedPreferences.getString(LAST_NAME_KEY, null).orEmpty(),
            isPublic = sharedPreferences.getBoolean(IS_PUBLIC_KEY, false)
        )
    }

    override fun saveProfile(profile: UserProfile) {
        sharedPreferences.edit()
            .putString(EMAIL_KEY, profile.email)
            .putString(FIRST_NAME_KEY, profile.firstName)
            .putString(LAST_NAME_KEY, profile.lastName)
            .putBoolean(IS_PUBLIC_KEY, profile.isPublic)
            .apply()
    }

    override fun clear() {
        sharedPreferences.edit()
            .remove(EMAIL_KEY)
            .remove(FIRST_NAME_KEY)
            .remove(LAST_NAME_KEY)
            .remove(IS_PUBLIC_KEY)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "profile_prefs"
        const val EMAIL_KEY = "EMAIL"
        const val FIRST_NAME_KEY = "FIRST_NAME"
        const val LAST_NAME_KEY = "LAST_NAME"
        const val IS_PUBLIC_KEY = "IS_PUBLIC"
    }
}
