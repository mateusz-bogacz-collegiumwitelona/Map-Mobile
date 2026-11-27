package com.example.map.di

import com.example.map.data.repository.AccountRepositoryImpl
import com.example.map.data.repository.AuthRepositoryImpl
import com.example.map.data.repository.ProfileRepositoryImpl
import com.example.map.data.repository.TripRepositoryImpl
import com.example.map.domain.repository.AccountRepository
import com.example.map.domain.repository.AuthRepository
import com.example.map.domain.repository.ProfileRepository
import com.example.map.domain.repository.TripRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        implementation: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindAccountRepository(
        implementation: AccountRepositoryImpl
    ): AccountRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(
        implementation: ProfileRepositoryImpl
    ): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindTripRepository(
        implementation: TripRepositoryImpl
    ): TripRepository
}