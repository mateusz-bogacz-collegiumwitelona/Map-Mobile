package com.example.map.di

import com.example.map.data.repository.RemoteAccountRepository
import com.example.map.domain.repository.AccountRepository
import com.example.map.domain.repository.ProfileStorage
import com.example.map.domain.usecase.AccountSession
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AccountModule {

    @Binds
    @Singleton
    abstract fun bindAccountRepository(
        remoteAccountRepository: RemoteAccountRepository
    ): AccountRepository

    companion object {
        @Provides
        @Singleton
        fun provideAccountSession(
            accountRepository: AccountRepository,
            profileStorage: ProfileStorage
        ): AccountSession = AccountSession(accountRepository, profileStorage)
    }
}
