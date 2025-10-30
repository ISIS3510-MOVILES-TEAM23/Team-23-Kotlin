package com.example.team_23_kotlin.di

import android.content.Context
import com.example.team_23_kotlin.domain.repository.AnalyticsRepository
import com.example.team_23_kotlin.data.repository.AnalyticsRepositoryImpl
import com.example.team_23_kotlin.domain.repository.LocationRepository
import com.example.team_23_kotlin.data.repository.LocationRepositoryImpl
import com.example.team_23_kotlin.data.sales.SalesRepository
import com.example.team_23_kotlin.data.sales.FirestoreSalesRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    // 🔹 Vincula AnalyticsRepository (usa @Binds porque no requiere Context)
    @Binds
    @Singleton
    abstract fun bindAnalyticsRepository(
        impl: AnalyticsRepositoryImpl
    ): AnalyticsRepository

    companion object {
        // 🔹 Provee LocationRepository (usa @Provides porque necesita Context)
        @Provides
        @Singleton
        fun provideLocationRepository(
            @ApplicationContext context: Context
        ): LocationRepository {
            return LocationRepositoryImpl(context)
        }

        // 🔹 Provee SalesRepository
        @Provides
        @Singleton
        fun provideSalesRepository(
            firestore: FirebaseFirestore
        ): SalesRepository {
            return FirestoreSalesRepository(firestore)
        }
    }
}
