package com.yunok.walzi.data.repository

import com.yunok.walzi.data.local.WallpaperListsDataStore
import com.yunok.walzi.domain.model.AutoRotateSettings
import com.yunok.walzi.domain.model.DEFAULT_LIST_ID
import com.yunok.walzi.domain.model.Wallpaper
import com.yunok.walzi.domain.model.WallpaperList
import com.yunok.walzi.domain.repository.WallpaperListRepository
import com.yunok.walzi.domain.repository.WallpaperRepository
import com.yunok.walzi.util.WallpaperTarget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WallpaperListRepositoryImpl @Inject constructor(
    private val listsDataStore: WallpaperListsDataStore,
    private val wallpaperRepository: WallpaperRepository
) : WallpaperListRepository {

    override fun observeLists(): Flow<List<WallpaperList>> = listsDataStore.lists

    override suspend fun createList(name: String): String = listsDataStore.createList(name)

    override suspend fun renameList(listId: String, newName: String) =
        listsDataStore.renameList(listId, newName)

    override suspend fun deleteList(listId: String) = listsDataStore.deleteList(listId)

    override suspend fun addWallpaperToList(listId: String, wallpaperId: String) =
        listsDataStore.addWallpaperToList(listId, wallpaperId)

    override suspend fun removeWallpaperFromList(listId: String, wallpaperId: String) =
        listsDataStore.removeWallpaperFromList(listId, wallpaperId)

    override fun observeListIdsContaining(wallpaperId: String): Flow<Set<String>> =
        listsDataStore.lists.map { lists ->
            lists.filter { wallpaperId in it.wallpaperIds }.map { it.id }.toSet()
        }

    override fun observeAutoRotateSettings(): Flow<AutoRotateSettings> = listsDataStore.autoRotateSettings

    override suspend fun setAutoRotateEnabled(enabled: Boolean) = listsDataStore.setAutoRotateEnabled(enabled)

    override suspend fun setActiveAutoRotateList(listId: String) = listsDataStore.setActiveListId(listId)

    override suspend fun setAutoRotateTarget(target: WallpaperTarget) = listsDataStore.setAutoRotateTarget(target)

    override suspend fun advanceAndGetNextRotationWallpaper(): Wallpaper? {
        val settings = listsDataStore.currentAutoRotateSettings()
        val activeList = listsDataStore.currentLists().firstOrNull { it.id == settings.activeListId }
            ?: listsDataStore.currentLists().firstOrNull { it.id == DEFAULT_LIST_ID }
        val wallpaperIds = activeList?.wallpaperIds.orEmpty()
        if (wallpaperIds.isEmpty()) return null

        // Sequential rotation: find where we left off, move to the next id, wrapping at the end.
        val lastIndex = wallpaperIds.indexOf(settings.lastRotatedWallpaperId)
        val nextIndex = if (lastIndex == -1) 0 else (lastIndex + 1) % wallpaperIds.size
        val nextWallpaperId = wallpaperIds[nextIndex]

        val wallpaper = wallpaperRepository.getWallpaperById(nextWallpaperId) ?: return null
        listsDataStore.setLastRotatedWallpaperId(nextWallpaperId)
        return wallpaper
    }
}