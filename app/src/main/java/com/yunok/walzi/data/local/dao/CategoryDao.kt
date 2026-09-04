package com.yunok.walzi.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.yunok.walzi.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY position ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories")
    suspend fun clearAll()

    /** Atomically replaces the whole cached set - avoids a brief "empty list" flash between
     *  clear and insert, and avoids leaving stale rows if a category was deleted server-side. */
    @Transaction
    suspend fun replaceAll(categories: List<CategoryEntity>) {
        clearAll()
        insertAll(categories)
    }
}