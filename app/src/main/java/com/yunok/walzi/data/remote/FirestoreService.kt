package com.yunok.walzi.data.remote

import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.yunok.walzi.data.model.CategoryDto
import com.yunok.walzi.data.model.WallpaperDto
import com.yunok.walzi.domain.model.WallpaperCursor
import com.yunok.walzi.domain.model.WallpaperSource
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Both categories and wallpapers are one-shot `.get()` calls now, not realtime listeners -
 * they're fronted by a Room cache with a TTL (see WallpaperRepositoryImpl), which decides
 * when it's actually time to call these methods again rather than holding an open connection.
 */
@Singleton
class FirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    /** One-shot fetch of every category, used to (re)populate the Room cache. */
    suspend fun getCategoriesOnce(): List<Pair<String, CategoryDto>> {
        val snapshot = firestore.collection("categories")
            .orderBy("position", Query.Direction.ASCENDING)
            .get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(CategoryDto::class.java)?.let { doc.id to it }
        }
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

    /**
     * Which of the cursor's fields actually correspond to an `orderBy()` clause for this
     * source - must match baseQueryFor's clause count *and* order exactly, or Firestore
     * throws "Too many arguments provided to startAfter()".
     *   Feed               -> orderBy(priority, createdAt)  -> 2 values
     *   Recent             -> orderBy(createdAt)             -> 1 value
     *   Popular            -> orderBy(priority)               -> 1 value
     *   CategoryWallpapers -> orderBy(priority)               -> 1 value
     */
    private fun startAfterValues(source: WallpaperSource, cursor: WallpaperCursor): Array<Any> = when (source) {
        is WallpaperSource.Feed -> arrayOf(cursor.priority, cursor.createdAt)
        is WallpaperSource.Recent -> arrayOf(cursor.createdAt)
        is WallpaperSource.Popular -> arrayOf(cursor.priority)
        is WallpaperSource.CategoryWallpapers -> arrayOf(cursor.priority)
    }

    /**
     * Fetches exactly one page (default 20 docs) starting after [startAfter], if given.
     * The cursor is the last page's own sort-key values (priority + createdAt), not an opaque
     * DocumentSnapshot - this is what lets a cursor be derived from a Room-cached item just as
     * easily as from a freshly-fetched one, so cached-first-page + live-load-more can hand off
     * to each other seamlessly.
     */
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

    /** Single one-shot document fetch - used when a specific id isn't in an already-loaded page. */
    suspend fun getWallpaperById(id: String): WallpaperDto? =
        firestore.collection("wallpapers").document(id).get().await()
            .toObject(WallpaperDto::class.java)

    /**
     * Fetches a specific, bounded set of wallpapers by id (used for Favorites, which is
     * always a small, known list - never the whole collection). Firestore's `whereIn` caps
     * at 10 values per query, so larger id lists are chunked and fetched in parallel batches.
     */
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