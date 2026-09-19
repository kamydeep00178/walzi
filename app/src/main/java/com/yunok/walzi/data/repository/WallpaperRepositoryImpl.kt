package com.yunok.walzi.data.repository

import com.yunok.walzi.data.local.CacheConfig
import com.yunok.walzi.data.local.CacheMetaDataStore
import com.yunok.walzi.data.local.FavoritesDataStore
import com.yunok.walzi.data.local.dao.CategoryDao
import com.yunok.walzi.data.local.dao.TagDao
import com.yunok.walzi.data.local.dao.WallpaperCacheDao
import com.yunok.walzi.data.local.entity.CategoryEntity
import com.yunok.walzi.data.local.entity.TagEntity
import com.yunok.walzi.data.local.entity.WallpaperCacheEntity
import com.yunok.walzi.data.local.entity.toDomain
import com.yunok.walzi.data.model.CategoryDto
import com.yunok.walzi.data.model.TagDto
import com.yunok.walzi.data.model.WallpaperDto
import com.yunok.walzi.data.remote.FirestoreService
import com.yunok.walzi.domain.model.Category
import com.yunok.walzi.domain.model.Tag
import com.yunok.walzi.domain.model.Wallpaper
import com.yunok.walzi.domain.model.WallpaperCursor
import com.yunok.walzi.domain.model.WallpaperPage
import com.yunok.walzi.domain.model.WallpaperSource
import com.yunok.walzi.domain.repository.WallpaperRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class WallpaperRepositoryImpl @Inject constructor(
    private val firestoreService: FirestoreService,
    private val favoritesDataStore: FavoritesDataStore,
    private val categoryDao: CategoryDao,
    private val tagDao: TagDao,
    private val wallpaperCacheDao: WallpaperCacheDao,
    private val cacheMeta: CacheMetaDataStore
) : WallpaperRepository {

    private companion object {
        const val FEATURED_BUCKET = "featured"
        const val FEATURED_COUNT = 10
        const val FEATURED_POOL_SIZE = 50L
    }

    override fun observeCategories(): Flow<List<Category>> =
        categoryDao.observeAll()
            .onStart { refreshCategoriesIfStale() }
            .map { entities -> entities.map { it.toDomain() } }

    private suspend fun refreshCategoriesIfStale() {
        val stale = isStale(
            isEmpty = categoryDao.count() == 0,
            ttlDays = CacheConfig.CATEGORY_REFRESH_INTERVAL_DAYS,
            lastFetchedAt = cacheMeta.getCategoriesLastFetchedAt()
        )
        if (!stale) return
        try {
            val fresh = firestoreService.getCategoriesOnce()
            categoryDao.replaceAll(fresh.map { (id, dto) -> id.toCategoryEntity(dto) })
            cacheMeta.setCategoriesLastFetchedAt(System.currentTimeMillis())
        } catch (_: Exception) {
        }
    }

    override fun observeTags(): Flow<List<Tag>> =
        tagDao.observeAll()
            .onStart { refreshTagsIfStale() }
            .map { entities -> entities.map { it.toDomain() } }

    private suspend fun refreshTagsIfStale() {
        val stale = isStale(
            isEmpty = tagDao.count() == 0,
            ttlDays = CacheConfig.CATEGORY_REFRESH_INTERVAL_DAYS,
            lastFetchedAt = cacheMeta.getTagsLastFetchedAt()
        )
        if (!stale) return
        try {
            val fresh = firestoreService.getTagsOnce()
            tagDao.replaceAll(fresh.map { it.toTagEntity() })
            cacheMeta.setTagsLastFetchedAt(System.currentTimeMillis())
        } catch (_: Exception) {
        }
    }

    override suspend fun searchWallpapersByTag(tag: String, limit: Int): List<Wallpaper> {
        val dtoList = firestoreService.searchWallpapersByTag(tag, limit.toLong())
        val favoriteIds = favoritesDataStore.favoriteIds.first()
        return dtoList.map { (id, dto) -> id.toWallpaper(dto, favoriteIds.contains(id)) }
    }

    override fun observeCachedWallpapers(bucket: String): Flow<List<Wallpaper>> =
        combine(wallpaperCacheDao.observeByBucket(bucket), favoritesDataStore.favoriteIds) { entities, favoriteIds ->
            entities.map { it.toDomain(isFavorite = favoriteIds.contains(it.id)) }
        }

    override suspend fun ensureWallpaperBucketFresh(source: WallpaperSource, bucket: String, pageSize: Int) {
        val stale = isStale(
            isEmpty = wallpaperCacheDao.count(bucket) == 0,
            ttlDays = CacheConfig.WALLPAPER_REFRESH_INTERVAL_DAYS,
            lastFetchedAt = cacheMeta.getWallpaperBucketLastFetchedAt(bucket)
        )
        if (!stale) return
        try {
            val (dtoList, _) = firestoreService.getWallpaperPage(source, startAfter = null, pageSize = pageSize.toLong())
            wallpaperCacheDao.replaceBucket(bucket, dtoList.map { (id, dto) -> id.toWallpaperCacheEntity(dto, bucket) })
            cacheMeta.setWallpaperBucketLastFetchedAt(bucket, System.currentTimeMillis())
        } catch (_: Exception) {
        }
    }

    override suspend fun loadWallpaperPage(
        source: WallpaperSource,
        cursor: WallpaperCursor?,
        pageSize: Int
    ): WallpaperPage {
        val (dtoList, nextCursor) = firestoreService.getWallpaperPage(source, cursor, pageSize.toLong())
        val favoriteIds = favoritesDataStore.favoriteIds.first()
        val wallpapers = dtoList.map { (id, dto) -> id.toWallpaper(dto, favoriteIds.contains(id)) }
        return WallpaperPage(items = wallpapers, nextCursor = nextCursor, endReached = dtoList.size < pageSize)
    }

    override suspend fun getWallpapersByIds(ids: List<String>): List<Wallpaper> {
        if (ids.isEmpty()) return emptyList()
        val dtoList = firestoreService.getWallpapersByIds(ids)
        val favoriteIds = favoritesDataStore.favoriteIds.first()
        return dtoList.map { (id, dto) -> id.toWallpaper(dto, favoriteIds.contains(id)) }
    }

    override suspend fun getWallpaperById(id: String): Wallpaper? {
        val dto = firestoreService.getWallpaperById(id) ?: return null
        val favoriteIds = favoritesDataStore.favoriteIds.first()
        return id.toWallpaper(dto, favoriteIds.contains(id))
    }

    override fun observeFavoriteIds(): Flow<Set<String>> = favoritesDataStore.favoriteIds

    override suspend fun toggleFavorite(wallpaperId: String) {
        favoritesDataStore.toggle(wallpaperId)
    }

    override fun observeFeaturedWallpapers(): Flow<List<Wallpaper>> =
        observeCachedWallpapers(FEATURED_BUCKET)

    override suspend fun ensureFeaturedFresh() {
        val lastGeneratedAt = cacheMeta.getWallpaperBucketLastFetchedAt(FEATURED_BUCKET)
        if (!isDifferentCalendarDay(lastGeneratedAt)) return

        try {
            val (pool, _) = firestoreService.getWallpaperPage(WallpaperSource.Feed, startAfter = null, pageSize = FEATURED_POOL_SIZE)
            if (pool.isEmpty()) return

            val today = LocalDate.now()
            val picks = pool.shuffled(Random(today.toEpochDay())).take(FEATURED_COUNT)

            wallpaperCacheDao.replaceBucket(
                FEATURED_BUCKET,
                picks.map { (id, dto) -> id.toWallpaperCacheEntity(dto, FEATURED_BUCKET) }
            )
            cacheMeta.setWallpaperBucketLastFetchedAt(FEATURED_BUCKET, System.currentTimeMillis())
        } catch (_: Exception) {
        }
    }

    private fun isDifferentCalendarDay(lastFetchedAtMillis: Long?): Boolean {
        if (lastFetchedAtMillis == null) return true
        val lastDay = Instant.ofEpochMilli(lastFetchedAtMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        return lastDay != LocalDate.now()
    }

    private fun isStale(isEmpty: Boolean, ttlDays: Long, lastFetchedAt: Long?): Boolean = when {
        isEmpty -> true
        ttlDays <= 0 -> false
        lastFetchedAt == null -> true
        else -> System.currentTimeMillis() - lastFetchedAt >= TimeUnit.DAYS.toMillis(ttlDays)
    }

    private fun String.toCategoryEntity(dto: CategoryDto) = CategoryEntity(
        id = this,
        name = dto.name,
        imageUrl = dto.imageUrl,
        position = dto.position.toInt(),
        countryCodes = dto.countryCodes ?: emptyList()
    )

    private fun TagDto.toTagEntity() = TagEntity(
        name = name,
        wallpaperCount = wallpaperCount.toInt()
    )

    private fun String.toWallpaperCacheEntity(dto: WallpaperDto, bucket: String) = WallpaperCacheEntity(
        id = this,
        bucket = bucket,
        title = dto.title,
        imageUrl = dto.imageUrl,
        categoryId = dto.categoryId,
        categoryName = dto.categoryName,
        isPopular = dto.isPopular,
        priority = dto.priority,
        resolution = dto.resolution,
        sizeLabel = dto.sizeLabel,
        createdAt = dto.createdAt,
        tags = dto.tags ?: emptyList()
    )

    private fun String.toWallpaper(dto: WallpaperDto, isFavorite: Boolean) = Wallpaper(
        id = this,
        title = dto.title,
        imageUrl = dto.imageUrl,
        categoryId = dto.categoryId,
        categoryName = dto.categoryName,
        isPopular = dto.isPopular,
        priority = dto.priority.toInt(),
        resolution = dto.resolution,
        sizeLabel = dto.sizeLabel,
        createdAt = dto.createdAt,
        isFavorite = isFavorite,
        tags = dto.tags ?: emptyList()
    )
}