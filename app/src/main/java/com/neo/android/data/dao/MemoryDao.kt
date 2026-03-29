package com.neo.android.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.neo.android.data.entity.MemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {

    @Insert
    suspend fun insert(memory: MemoryEntity): Long

    @Update
    suspend fun update(memory: MemoryEntity)

    @Delete
    suspend fun delete(memory: MemoryEntity)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM memories WHERE id = :id")
    suspend fun getById(id: Long): MemoryEntity?

    @Query("SELECT * FROM memories ORDER BY updatedAtMillis DESC")
    fun getAllFlow(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE category = :category ORDER BY updatedAtMillis DESC")
    fun getByCategoryFlow(category: String): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories ORDER BY updatedAtMillis DESC")
    suspend fun getAll(): List<MemoryEntity>

    @Query("SELECT COUNT(*) FROM memories")
    fun getCountFlow(): Flow<Int>

    @Query("UPDATE memories SET confidence = confidence * 0.98 WHERE source = 'auto'")
    suspend fun decayAutoConfidence()

    @Query("DELETE FROM memories WHERE source = 'auto' AND confidence < 0.40")
    suspend fun pruneWeakMemories(): Int

    @Query("DELETE FROM memories WHERE id IN (SELECT id FROM memories WHERE source = 'auto' ORDER BY confidence ASC LIMIT :count)")
    suspend fun deleteLowestConfidence(count: Int)

    @Query("SELECT COUNT(*) FROM memories WHERE source = 'auto'")
    suspend fun getAutoCount(): Int
}
