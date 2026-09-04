package com.yunok.walzi.di

import android.content.Context
import androidx.room.Room
import com.yunok.walzi.data.local.WalziDatabase
import com.yunok.walzi.data.local.dao.CategoryDao
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
            // This database is pure cache (Room mirrors Firestore + local prefs) - if the
            // schema version bumps (e.g. a new column), wiping and re-fetching fresh is
            // always safe and simpler than writing a migration for cache-only data.
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideCategoryDao(database: WalziDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideWallpaperCacheDao(database: WalziDatabase): WallpaperCacheDao = database.wallpaperCacheDao()
}