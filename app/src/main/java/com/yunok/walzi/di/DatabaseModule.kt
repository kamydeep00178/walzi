package com.yunok.walzi.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

    /**
     * v4 -> v5: wallpaper_cache gains `position` (Firestore order). The cache is disposable, so
     * it's simply emptied and refetched - but done as a real migration so the notification
     * history that shares this database survives the upgrade instead of being wiped by the
     * destructive fallback.
     */
    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE wallpaper_cache ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
            db.execSQL("DELETE FROM wallpaper_cache")
        }
    }

    @Provides
    @Singleton
    fun provideWalziDatabase(@ApplicationContext context: Context): WalziDatabase =
        Room.databaseBuilder(context, WalziDatabase::class.java, "walzi.db")
            .addMigrations(MIGRATION_4_5)
            // Only reached for installs older than v4 (no migration path) - see note above.
            .fallbackToDestructiveMigration(dropAllTables = true)
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