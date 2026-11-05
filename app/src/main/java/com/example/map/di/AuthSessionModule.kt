package com.example.map.di

import com.example.map.domain.repository.TokenStorage
import com.example.map.domain.usecase.AuthSession
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthSessionModule {

    @Provides
    @Singleton
    fun provideAuthSession(tokenStorage: TokenStorage): AuthSession =
        AuthSession(tokenStorage)
}