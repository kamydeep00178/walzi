package com.yunok.walzi.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.yunok.walzi.domain.model.AutoRotateSettings
import com.yunok.walzi.domain.model.DEFAULT_LIST_ID
import com.yunok.walzi.domain.model.WallpaperList
import com.yunok.walzi.util.WallpaperTarget
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "walzi_wallpaper_lists")

/**
 * Lists and auto-rotate settings, kept purely on-device (no auth in this build - matches
 * FavoritesDataStore's approach). Preferences DataStore doesn't support relational data, so
 * each list's wallpaper id order is stored as a single delimited string (a plain stringSet
 * loses order, which sequential rotation depends on) under a per-list dynamic key.
 */
@Singleton
class WallpaperListsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val LIST_IDS = stringPreferencesKey("list_ids") // delimited, preserves creation order
        val AUTO_ROTATE_ENABLED = booleanPreferencesKey("auto_rotate_enabled")
        val ACTIVE_LIST_ID = stringPreferencesKey("auto_rotate_active_list_id")
        val AUTO_ROTATE_TARGET = stringPreferencesKey("auto_rotate_target")
        val LAST_ROTATED_WALLPAPER_ID = stringPreferencesKey("auto_rotate_last_wallpaper_id")

        fun listName(id: String) = stringPreferencesKey("list_name_$id")
        fun listWallpapers(id: String) = stringPreferencesKey("list_wallpapers_$id") // delimited, preserves add order
    }

    private companion object {
        const val DELIMITER = "\u0001" // unlikely to appear in a name or a Firestore doc id
        const val DEFAULT_LIST_NAME = "My Wallpapers"
    }

    private fun String.toIdList() = if (isEmpty()) emptyList() else split(DELIMITER)
    private fun List<String>.toStored() = joinToString(DELIMITER)

    val lists: Flow<List<WallpaperList>> = context.dataStore.data.map { prefs ->
        val storedIds = prefs[Keys.LIST_IDS]?.toIdList().orEmpty()
        val ids = if (DEFAULT_LIST_ID in storedIds) storedIds else listOf(DEFAULT_LIST_ID) + storedIds
        ids.map { id ->
            WallpaperList(
                id = id,
                name = prefs[Keys.listName(id)] ?: if (id == DEFAULT_LIST_ID) DEFAULT_LIST_NAME else "Untitled",
                wallpaperIds = prefs[Keys.listWallpapers(id)]?.toIdList().orEmpty(),
                isDefault = id == DEFAULT_LIST_ID
            )
        }
    }

    val autoRotateSettings: Flow<AutoRotateSettings> = context.dataStore.data.map { prefs ->
        AutoRotateSettings(
            enabled = prefs[Keys.AUTO_ROTATE_ENABLED] ?: false,
            activeListId = prefs[Keys.ACTIVE_LIST_ID] ?: DEFAULT_LIST_ID,
            target = prefs[Keys.AUTO_ROTATE_TARGET]?.let { runCatching { WallpaperTarget.valueOf(it) }.getOrNull() }
                ?: WallpaperTarget.BOTH,
            lastRotatedWallpaperId = prefs[Keys.LAST_ROTATED_WALLPAPER_ID]
        )
    }

    suspend fun createList(name: String): String {
        val id = "list_${System.currentTimeMillis()}"
        context.dataStore.edit { prefs ->
            val currentIds = prefs[Keys.LIST_IDS]?.toIdList().orEmpty()
            prefs[Keys.LIST_IDS] = (currentIds + id).toStored()
            prefs[Keys.listName(id)] = name
        }
        return id
    }

    suspend fun renameList(listId: String, newName: String) {
        context.dataStore.edit { prefs -> prefs[Keys.listName(listId)] = newName }
    }

    suspend fun deleteList(listId: String) {
        if (listId == DEFAULT_LIST_ID) return
        context.dataStore.edit { prefs ->
            val currentIds = prefs[Keys.LIST_IDS]?.toIdList().orEmpty()
            prefs[Keys.LIST_IDS] = (currentIds - listId).toStored()
            prefs.remove(Keys.listName(listId))
            prefs.remove(Keys.listWallpapers(listId))
            // If the deleted list was the active auto-rotate source, fall back to the default list.
            if (prefs[Keys.ACTIVE_LIST_ID] == listId) {
                prefs[Keys.ACTIVE_LIST_ID] = DEFAULT_LIST_ID
            }
        }
    }

    suspend fun addWallpaperToList(listId: String, wallpaperId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.listWallpapers(listId)]?.toIdList().orEmpty()
            if (wallpaperId !in current) {
                prefs[Keys.listWallpapers(listId)] = (current + wallpaperId).toStored()
            }
        }
    }

    suspend fun removeWallpaperFromList(listId: String, wallpaperId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.listWallpapers(listId)]?.toIdList().orEmpty()
            prefs[Keys.listWallpapers(listId)] = (current - wallpaperId).toStored()
        }
    }

    suspend fun setAutoRotateEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.AUTO_ROTATE_ENABLED] = enabled }
    }

    suspend fun setActiveListId(listId: String) {
        context.dataStore.edit { prefs -> prefs[Keys.ACTIVE_LIST_ID] = listId }
    }

    suspend fun setAutoRotateTarget(target: WallpaperTarget) {
        context.dataStore.edit { prefs -> prefs[Keys.AUTO_ROTATE_TARGET] = target.name }
    }

    suspend fun setLastRotatedWallpaperId(wallpaperId: String) {
        context.dataStore.edit { prefs -> prefs[Keys.LAST_ROTATED_WALLPAPER_ID] = wallpaperId }
    }

    suspend fun currentLists(): List<WallpaperList> = lists.first()
    suspend fun currentAutoRotateSettings(): AutoRotateSettings = autoRotateSettings.first()
}