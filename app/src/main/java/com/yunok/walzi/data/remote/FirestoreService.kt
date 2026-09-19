package com.yunok.walzi.data.remote

import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.yunok.walzi.data.model.CategoryDto
import com.yunok.walzi.data.model.TagDto
import com.yunok.walzi.data.model.WallpaperDto
import com.yunok.walzi.domain.model.WallpaperCursor
import com.yunok.walzi.domain.model.WallpaperSource
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun getCategoriesOnce(): List<Pair<String, CategoryDto>> {
        val snapshot = firestore.collection("categories")
            .orderBy("position", Query.Direction.ASCENDING)
            .get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(CategoryDto::class.java)?.let { doc.id to it }
        }
    }

    suspend fun getTagsOnce(): List<TagDto> {
        val snapshot = firestore.collection("tags")
            .orderBy("wallpaperCount", Query.Direction.DESCENDING)
            .get().await()
        return snapshot.documents.mapNotNull { it.toObject(TagDto::class.java) }
    }

    private fun baseQueryFor(source: WallpaperSource): Query = when (source) {
        is WallpaperSource.Feed -> firestore.collection("wallpapers")
            .orderBy("priority", Query.Direction.DESCENDING)
            .orderBy("createdAt", Query.Direction.DESCENDING)

        is WallpaperSource.Recent -> firestore.collection("wallpapers")
            .orderBy("createdAt", Query.Direction.DESCENDING)

        is WallpaperSource.Popular -> firestore.collection("wallpapers")
            .whereEqualTo("isPopular", true)
            .orderBy("priority", Query.Direction.DESCENDING)

        is WallpaperSource.CategoryWallpapers -> firestore.collection("wallpapers")
            .whereEqualTo("categoryId", source.categoryId)
            .orderBy("priority", Query.Direction.DESCENDING)
    }

    private fun startAfterValues(source: WallpaperSource, cursor: WallpaperCursor): Array<Any> = when (source) {
        is WallpaperSource.Feed -> arrayOf(cursor.priority, cursor.createdAt)
        is WallpaperSource.Recent -> arrayOf(cursor.createdAt)
        is WallpaperSource.Popular -> arrayOf(cursor.priority)
        is WallpaperSource.CategoryWallpapers -> arrayOf(cursor.priority)
    }

    suspend fun getWallpaperPage(
        source: WallpaperSource,
        startAfter: WallpaperCursor?,
        pageSize: Long
    ): Pair<List<Pair<String, WallpaperDto>>, WallpaperCursor?> {
        var query = baseQueryFor(source).limit(pageSize)
        if (startAfter != null) query = query.startAfter(*startAfterValues(source, startAfter))

        val snapshot = query.get().await()
        val items = snapshot.documents.mapNotNull { doc ->
            doc.toObject(WallpaperDto::class.java)?.let { doc.id to it }
        }
        val nextCursor = items.lastOrNull()?.second?.let { WallpaperCursor(it.priority, it.createdAt) }
        return items to nextCursor
    }

    suspend fun searchWallpapersByTag(tag: String, limit: Long): List<Pair<String, WallpaperDto>> {
        val snapshot = firestore.collection("wallpapers")
            .whereArrayContains("tags", tag)
            .limit(limit)
            .get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(WallpaperDto::class.java)?.let { doc.id to it }
        }
    }

    suspend fun getWallpaperById(id: String): WallpaperDto? =
        firestore.collection("wallpapers").document(id).get().await()
            .toObject(WallpaperDto::class.java)

    suspend fun getWallpapersByIds(ids: List<String>): List<Pair<String, WallpaperDto>> {
        if (ids.isEmpty()) return emptyList()
        val results = mutableListOf<Pair<String, WallpaperDto>>()
        ids.chunked(10).forEach { chunk ->
            val snapshot = firestore.collection("wallpapers")
                .whereIn(FieldPath.documentId(), chunk)
                .get().await()
            results += snapshot.documents.mapNotNull { doc ->
                doc.toObject(WallpaperDto::class.java)?.let { doc.id to it }
            }
        }
        return results
    }
}