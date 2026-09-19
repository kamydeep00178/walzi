package com.yunok.walzi.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.searchHistoryDataStore by preferencesDataStore(name = "walzi_search_history")

@Singleton
class SearchHistoryDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private companion object {
        const val MAX_ENTRIES = 8
        const val DELIMITER = "\u0001"
    }

    private val key = stringPreferencesKey("recent_searches")

    val recentSearches: Flow<List<String>> = context.searchHistoryDataStore.data.map { prefs ->
        prefs[key]?.split(DELIMITER)?.filter { it.isNotBlank() } ?: emptyList()
    }

    suspend fun addSearch(term: String) {
        val trimmed = term.trim()
        if (trimmed.isEmpty()) return
        context.searchHistoryDataStore.edit { prefs ->
            val current = prefs[key]?.split(DELIMITER)?.filter { it.isNotBlank() } ?: emptyList()
            val updated = (listOf(trimmed) + current.filterNot { it.equals(trimmed, ignoreCase = true) })
                .take(MAX_ENTRIES)
            prefs[key] = updated.joinToString(DELIMITER)
        }
    }

    suspend fun clear() {
        context.searchHistoryDataStore.edit { prefs -> prefs.remove(key) }
    }
}