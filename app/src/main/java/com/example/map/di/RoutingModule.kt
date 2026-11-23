package com.example.map.di

import com.example.map.BuildConfig
import com.example.map.data.remote.service.OsrmApiService
import com.example.map.data.repository.OsrmRoadRouter
import com.example.map.domain.repository.RoadRouter
import com.example.map.domain.usecase.RouteGeometryCalculator
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RoutingClient

@Module
@InstallIn(SingletonComponent::class)
abstract class RoutingModule {

    @Binds
    @Singleton
    abstract fun bindRoadRouter(osrmRoadRouter: OsrmRoadRouter): RoadRouter

    companion object {
        const val USER_AGENT = "MAP-Mobile/${BuildConfig.VERSION_NAME} (Android)"

        @Provides
        @Singleton
        @RoutingClient
        fun provideRoutingOkHttpClient(): OkHttpClient {
            val builder = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request()
                            .newBuilder()
                            .header("User-Agent", USER_AGENT)
                            .build()
                    )
                }
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .callTimeout(15, TimeUnit.SECONDS)

            if (BuildConfig.DEBUG) {
                builder.addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                    }
                )
            }

            return builder.build()
        }

        @Provides
        @Singleton
        @RoutingClient
        fun provideRoutingRetrofit(
            json: Json,
            @RoutingClient okHttpClient: OkHttpClient
        ): Retrofit {
            val contentType = "application/json; charset=utf-8".toMediaType()

            return Retrofit.Builder()
                .baseUrl(BuildConfig.ROUTING_BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(json.asConverterFactory(contentType))
                .build()
        }

        @Provides
        @Singleton
        fun provideOsrmApiService(@RoutingClient retrofit: Retrofit): OsrmApiService =
            retrofit.create(OsrmApiService::class.java)

        @Provides
        @Singleton
        fun provideRouteGeometryCalculator(roadRouter: RoadRouter): RouteGeometryCalculator =
            RouteGeometryCalculator(roadRouter)
    }
}
