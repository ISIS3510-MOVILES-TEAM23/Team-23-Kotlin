package com.example.team_23_kotlin.di


import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import javax.inject.Singleton
import com.example.team_23_kotlin.domain.repository.BluetoothRepository
import com.example.team_23_kotlin.data.repository.BluetoothRepositoryImpl

@Module
@InstallIn(SingletonComponent::class)
object BluetoothModule {

    @Provides
    @Singleton
    fun provideBluetoothRepository(
        @ApplicationContext context: Context
    ): BluetoothRepository = BluetoothRepositoryImpl(context)
}
