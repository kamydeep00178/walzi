package com.yunok.walzi.di

import com.yunok.walzi.data.repository.WallpaperListRepositoryImpl
import com.yunok.walzi.data.repository.WallpaperRepositoryImpl
import com.yunok.walzi.domain.repository.WallpaperListRepository
import com.yunok.walzi.domain.repository.WallpaperRepository
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
    abstract fun bindWallpaperRepository(
        impl: WallpaperRepositoryImpl
    ): WallpaperRepository

    @Binds
    @Singleton
    abstract fun bindWallpaperListRepository(
        impl: WallpaperListRepositoryImpl
    ): WallpaperListRepository
}