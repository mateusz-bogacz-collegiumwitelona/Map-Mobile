package com.example.map.di

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.map.BuildConfig
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class NetworkInjectionTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun hiltProvidesJsonOkHttpAndRetrofit() {
        val appContext =
            InstrumentationRegistry.getInstrumentation()
                .targetContext
                .applicationContext

        val dependencies = EntryPointAccessors.fromApplication(
            appContext,
            NetworkTestEntryPoint::class.java
        )

        assertNotNull(dependencies.json())
        assertNotNull(dependencies.okHttpClient())
        assertEquals(
            BuildConfig.API_BASE_URL,
            dependencies.retrofit().baseUrl().toString()
        )
        assertSame(
            dependencies.okHttpClient(),
            dependencies.retrofit().callFactory()
        )
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NetworkTestEntryPoint {
    fun json(): Json
    fun okHttpClient(): OkHttpClient
    fun retrofit(): Retrofit
}