package com.yunok.walzi.di

import android.content.Context
import androidx.room.Room
import com.yunok.walzi.data.local.WalziDatabase
import com.yunok.walzi.data.local.dao.CategoryDao
import com.yunok.walzi.data.local.dao.NotificationDao
import com.yunok.walzi.data.local.dao.TagDao
import com.yunok.walzi.data.local.dao.WallpaperCacheDao
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
    fun provideWalziDatabase(@ApplicationContext context: Context): WalziDatabase =
        Room.databaseBuilder(context, WalziDatabase::class.java, "walzi.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideCategoryDao(database: WalziDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideWallpaperCacheDao(database: WalziDatabase): WallpaperCacheDao = database.wallpaperCacheDao()

    @Provides
    fun provideNotificationDao(database: WalziDatabase): NotificationDao = database.notificationDao()

    @Provides
    fun provideTagDao(database: WalziDatabase): TagDao = database.tagDao()
}