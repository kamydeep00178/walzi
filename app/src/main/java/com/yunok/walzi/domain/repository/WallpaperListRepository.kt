package com.yunok.walzi.domain.repository

import com.yunok.walzi.domain.model.AutoRotateSettings
import com.yunok.walzi.domain.model.Wallpaper
import com.yunok.walzi.domain.model.WallpaperList
import com.yunok.walzi.util.WallpaperTarget
import kotlinx.coroutines.flow.Flow

interface WallpaperListRepository {

    /** All lists, default list first, custom lists in creation order. */
    fun observeLists(): Flow<List<WallpaperList>>

    /** @return the new list's id. */
    suspend fun createList(name: String): String
    suspend fun renameList(listId: String, newName: String)

    /** No-op if [listId] is the default list - it can be renamed but never deleted. */
    suspend fun deleteList(listId: String)

    suspend fun addWallpaperToList(listId: String, wallpaperId: String)
    suspend fun removeWallpaperFromList(listId: String, wallpaperId: String)

    /** Which list(s) a given wallpaper currently belongs to - drives the Add to List sheet's checkboxes. */
    fun observeListIdsContaining(wallpaperId: String): Flow<Set<String>>

    fun observeAutoRotateSettings(): Flow<AutoRotateSettings>
    suspend fun setAutoRotateEnabled(enabled: Boolean)
    suspend fun setActiveAutoRotateList(listId: String)
    suspend fun setAutoRotateTarget(target: WallpaperTarget)

    /**
     * Advances the active list's rotation pointer by one (wrapping around at the end) and
     * returns the wallpaper to set today, or null if the active list is empty. Called only by
     * AutoRotateWorker - resolves stored wallpaper ids into full Wallpaper objects via
     * WallpaperRepository under the hood.
     */
    suspend fun advanceAndGetNextRotationWallpaper(): Wallpaper?
}