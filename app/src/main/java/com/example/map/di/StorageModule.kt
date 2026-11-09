package com.example.map.di

import com.example.map.data.local.ProfileManager
import com.example.map.data.TokenManager
import com.example.map.domain.repository.ProfileStorage
import com.example.map.domain.repository.TokenStorage
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StorageModule {
    @Binds
    @Singleton
    abstract fun bindTokenStorage(
        tokenManager: TokenManager
    ): TokenStorage

    @Binds
    @Singleton
    abstract fun bindProfileStorage(
        profileManager: ProfileManager
    ): ProfileStorage
}
