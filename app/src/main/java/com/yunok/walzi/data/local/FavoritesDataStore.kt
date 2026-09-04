package com.yunok.walzi.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "walzi_favorites")

/**
 * Favorites are kept locally on-device (no user auth in this build), mirroring
 * the behaviour of the original design mock. Swap this out for a Firestore
 * `users/{uid}/favorites` collection later if you add sign-in.
 */
@Singleton
class FavoritesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val favoritesKey = stringSetPreferencesKey("favorite_wallpaper_ids")

    val favoriteIds: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[favoritesKey] ?: emptySet()
    }

    suspend fun toggle(wallpaperId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[favoritesKey] ?: emptySet()
            prefs[favoritesKey] = if (wallpaperId in current) {
                current - wallpaperId
            } else {
                current + wallpaperId
            }
        }
    }
}
