package com.example.team_23_kotlin.di

import com.example.team_23_kotlin.domain.repository.AnalyticsRepository
import com.example.team_23_kotlin.data.repository.AnalyticsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindAnalyticsRepository(
        impl: AnalyticsRepositoryImpl
    ): AnalyticsRepository
}
