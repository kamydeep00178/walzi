package com.yunok.walzi.data.repository

import com.yunok.walzi.data.remote.FirestoreService
import com.yunok.walzi.domain.model.Duo
import com.yunok.walzi.domain.model.DuoConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Duos and the Duo banner config. Both are small, so they're fetched once per app session and
 * kept in memory (Firestore's own offline cache covers app restarts without network).
 */
@Singleton
class DuoRepository @Inject constructor(
    private val firestoreService: FirestoreService
) {
    private val mutex = Mutex()
    private var config: DuoConfig? = null
    private var duos: List<Duo>? = null

    /** The banner config, or null if never configured / unavailable (banner then stays hidden). */
    suspend fun getConfig(): DuoConfig? = mutex.withLock {
        config ?: try {
            firestoreService.getDuoConfig()?.let { dto ->
                DuoConfig(
                    enabled = dto.enabled,
                    title = dto.title.ifBlank { "DUO" },
                    subtitle = dto.subtitle,
                    coverUrls = dto.coverUrls.orEmpty().filter { it.isNotBlank() }
                )
            }.also { config = it }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    /** All Duos, newest first. Throws on failure so the screen can offer Retry. */
    suspend fun getDuos(forceRefresh: Boolean = false): List<Duo> = mutex.withLock {
        if (!forceRefresh) duos?.let { return@withLock it }
        firestoreService.getDuos().map { (id, dto) ->
            Duo(
                id = id,
                title = dto.title,
                lockImageUrl = dto.lockImageUrl,
                lockThumbUrl = dto.lockThumbUrl,
                homeImageUrl = dto.homeImageUrl,
                homeThumbUrl = dto.homeThumbUrl,
                createdAt = dto.createdAt
            )
        }.filter { it.lockImageUrl.isNotBlank() && it.homeImageUrl.isNotBlank() }
            .also { duos = it }
    }
}
