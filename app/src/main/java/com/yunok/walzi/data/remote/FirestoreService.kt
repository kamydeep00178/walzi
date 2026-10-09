package com.yunok.walzi.data.remote

import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.yunok.walzi.data.model.CategoryDto
import com.yunok.walzi.data.model.DuoConfigDto
import com.yunok.walzi.data.model.DuoDto
import com.yunok.walzi.data.model.TagDto
import com.yunok.walzi.data.model.WallpaperDto
import com.yunok.walzi.domain.model.WallpaperCursor
import com.yunok.walzi.domain.model.WallpaperSource
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

    /**
     * [stableOrder] appends the document id as a final DESC sort key. Firestore's automatic and
     * composite indexes already end in `__name__` in the direction of their last field, so this
     * is served by the existing indexes - it just makes the sort total, so cursors can't skip
     * documents that tie on priority/createdAt.
     */
    private fun baseQueryFor(source: WallpaperSource, stableOrder: Boolean): Query {
        val query = when (source) {
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
        return if (stableOrder) query.orderBy(FieldPath.documentId(), Query.Direction.DESCENDING) else query
    }

    private fun startAfterValues(source: WallpaperSource, cursor: WallpaperCursor, stableOrder: Boolean): Array<Any> {
        val values: List<Any> = when (source) {
            is WallpaperSource.Feed -> listOf(cursor.priority, cursor.createdAt)
            is WallpaperSource.Recent -> listOf(cursor.createdAt)
            is WallpaperSource.Popular -> listOf(cursor.priority)
            is WallpaperSource.CategoryWallpapers -> listOf(cursor.priority)
        }
        return (if (stableOrder) values + cursor.id else values).toTypedArray()
    }

    suspend fun getWallpaperPage(
        source: WallpaperSource,
        startAfter: WallpaperCursor?,
        pageSize: Long
    ): Pair<List<Pair<String, WallpaperDto>>, WallpaperCursor?> {
        val snapshot = try {
            runPageQuery(source, startAfter, pageSize, stableOrder = true)
        } catch (e: FirebaseFirestoreException) {
            // Safety net: if the backend can't serve the tie-broken sort (missing index), fall
            // back to the legacy ordering rather than showing an empty app.
            if (e.code != FirebaseFirestoreException.Code.FAILED_PRECONDITION) throw e
            runPageQuery(source, startAfter, pageSize, stableOrder = false)
        }
        val items = snapshot.documents.mapNotNull { doc ->
            doc.toObject(WallpaperDto::class.java)?.let { doc.id to it }
        }
        val nextCursor = items.lastOrNull()?.let { (id, dto) -> WallpaperCursor(dto.priority, dto.createdAt, id) }
        return items to nextCursor
    }

    private suspend fun runPageQuery(
        source: WallpaperSource,
        startAfter: WallpaperCursor?,
        pageSize: Long,
        stableOrder: Boolean
    ) = baseQueryFor(source, stableOrder)
        .limit(pageSize)
        .let { q -> if (startAfter != null) q.startAfter(*startAfterValues(source, startAfter, stableOrder)) else q }
        .get().await()

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

    /** All Duos, newest first. A small, curated set - no pagination needed. */
    suspend fun getDuos(): List<Pair<String, DuoDto>> =
        firestore.collection("duos")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get().await()
            .documents.mapNotNull { doc -> doc.toObject(DuoDto::class.java)?.let { doc.id to it } }

    /** The Duo banner settings, or null if the admin portal never saved them. */
    suspend fun getDuoConfig(): DuoConfigDto? =
        firestore.collection("app_config").document("duo").get().await()
            .toObject(DuoConfigDto::class.java)

    /** Chunks of 10 (Firestore's whereIn limit) are fetched in parallel rather than one by one. */
    suspend fun getWallpapersByIds(ids: List<String>): List<Pair<String, WallpaperDto>> {
        if (ids.isEmpty()) return emptyList()
        return coroutineScope {
            ids.chunked(10).map { chunk ->
                async {
                    firestore.collection("wallpapers")
                        .whereIn(FieldPath.documentId(), chunk)
                        .get().await()
                        .documents.mapNotNull { doc ->
                            doc.toObject(WallpaperDto::class.java)?.let { doc.id to it }
                        }
                }
            }.awaitAll().flatten()
        }
    }
}
