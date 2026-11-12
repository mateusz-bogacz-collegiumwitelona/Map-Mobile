package com.example.map.di

import com.example.map.data.repository.RemoteProfileRepository
import com.example.map.domain.repository.ProfileRepository
import com.example.map.domain.repository.ProfileStorage
import com.example.map.domain.usecase.ProfileSession
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProfileModule {

    @Binds
    @Singleton
    abstract fun bindProfileRepository(
        remoteProfileRepository: RemoteProfileRepository
    ): ProfileRepository

    companion object {
        @Provides
        @Singleton
        fun provideProfileSession(
            profileRepository: ProfileRepository,
            profileStorage: ProfileStorage
        ): ProfileSession = ProfileSession(profileRepository, profileStorage)
    }
}
