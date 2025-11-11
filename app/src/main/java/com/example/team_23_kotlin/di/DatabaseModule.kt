package com.example.team_23_kotlin.di

import android.content.Context
import androidx.room.Room
import com.example.team_23_kotlin.data.local.room.AppDatabase
import com.example.team_23_kotlin.data.local.room.ChatDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "marketplace_db")
            .fallbackToDestructiveMigration(true)
            .build()

    @Provides
    fun provideChatDao(db: AppDatabase): ChatDao = db.chatDao()
}