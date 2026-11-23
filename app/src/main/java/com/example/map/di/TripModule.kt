package com.example.map.di

import com.example.map.data.repository.RemoteSegmentRouteRepository
import com.example.map.domain.repository.SegmentRouteRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TripModule {

    @Binds
    @Singleton
    abstract fun bindSegmentRouteRepository(
        remoteSegmentRouteRepository: RemoteSegmentRouteRepository
    ): SegmentRouteRepository
}
