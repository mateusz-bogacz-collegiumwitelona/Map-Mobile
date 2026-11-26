package com.example.map.di

import com.example.map.domain.repository.AccountRepository
import com.example.map.domain.repository.AuthRepository
import com.example.map.domain.repository.ProfileRepository
import com.example.map.domain.repository.ProfileStorage
import com.example.map.domain.repository.TokenStorage
import com.example.map.domain.session.AccountSession
import com.example.map.domain.session.AuthSession
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SessionModule {

    @Provides
    @Singleton
    fun provideAuthSession(
        tokenStorage: TokenStorage,
        authRepository: AuthRepository
    ): AuthSession = AuthSession(tokenStorage, authRepository)

    @Provides
    @Singleton
    fun provideAccountSession(
        accountRepository: AccountRepository,
        profileRepository: ProfileRepository,
        profileStorage: ProfileStorage
    ): AccountSession = AccountSession(
        accountRepository = accountRepository,
        profileRepository = profileRepository,
        profileStorage = profileStorage
    )
}