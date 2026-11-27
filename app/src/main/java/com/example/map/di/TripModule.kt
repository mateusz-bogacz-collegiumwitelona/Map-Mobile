package com.example.map.di

import com.example.map.data.repository.RemoteTripRepository
import com.example.map.domain.repository.TripRepository
import com.example.map.domain.usecase.TripSession
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TripModule {

    @Binds
    @Singleton
    abstract fun bindTripRepository(
        repository: RemoteTripRepository
    ): TripRepository

    companion object {
        @Provides
        @Singleton
        fun provideTripSession(
            repository: TripRepository
        ): TripSession = TripSession(repository)
    }
}