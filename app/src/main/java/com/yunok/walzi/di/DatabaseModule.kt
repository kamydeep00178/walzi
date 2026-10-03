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

    /**
     * v5 -> v6: wallpaper_cache gains `thumbUrl`. Rows are kept (not deleted): an empty thumbUrl
     * just falls back to imageUrl until the bucket's normal refresh fills it. Deleting would blank
     * "Today's Picks" until tomorrow, since that bucket only regenerates once per calendar day.
     */
    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE wallpaper_cache ADD COLUMN thumbUrl TEXT NOT NULL DEFAULT ''")
        }
    }

    /**
     * v6 -> v7: categories gains `thumbUrl`. Rows are kept: an empty thumbUrl falls back to
     * imageUrl until the daily category refresh (CacheConfig) fills it in.
     */
    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE categories ADD COLUMN thumbUrl TEXT NOT NULL DEFAULT ''")
        }
    }

    @Provides
    @Singleton
    fun provideWalziDatabase(@ApplicationContext context: Context): WalziDatabase =
        Room.databaseBuilder(context, WalziDatabase::class.java, "walzi.db")
            .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
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