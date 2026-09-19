package com.yunok.walzi.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.yunok.walzi.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY wallpaperCount DESC")
    fun observeAll(): Flow<List<TagEntity>>

    @Query("SELECT COUNT(*) FROM tags")
    suspend fun count(): Int

    @Query("DELETE FROM tags")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tags: List<TagEntity>)

    @Transaction
    suspend fun replaceAll(tags: List<TagEntity>) {
        clearAll()
        insertAll(tags)
    }
}