package com.neo.android.data

import com.neo.android.data.dao.MemoryDao
import com.neo.android.data.entity.MemoryEntity
import kotlinx.coroutines.flow.Flow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

class MemoryRepository(private val memoryDao: MemoryDao) {

    fun getAllMemoriesFlow(): Flow<List<MemoryEntity>> = memoryDao.getAllFlow()

    fun getByCategoryFlow(category: String): Flow<List<MemoryEntity>> =
        memoryDao.getByCategoryFlow(category)

    fun getCountFlow(): Flow<Int> = memoryDao.getCountFlow()

    suspend fun getById(id: Long): MemoryEntity? = memoryDao.getById(id)

    suspend fun insertMemory(memory: MemoryEntity): Long = memoryDao.insert(memory)

    suspend fun updateMemory(memory: MemoryEntity) = memoryDao.update(memory)

    suspend fun deleteMemory(id: Long) = memoryDao.deleteById(id)

    suspend fun findRelevantMemories(
        queryEmbedding: FloatArray,
        topK: Int = 5,
        minConfidence: Float = 0.50f,
    ): List<MemoryEntity> {
        val all = memoryDao.getAll()
        return all
            .filter { it.confidence >= minConfidence && it.embedding != null }
            .map { entity ->
                val entityEmbedding = bytesToFloats(entity.embedding!!)
                val similarity = cosineSimilarity(queryEmbedding, entityEmbedding)
                entity to similarity
            }
            .sortedByDescending { it.second }
            .take(topK)
            .map { it.first }
    }

    suspend fun findBestMatch(embedding: FloatArray): Pair<MemoryEntity, Float>? {
        val all = memoryDao.getAll()
        return all
            .filter { it.embedding != null }
            .map { entity ->
                val entityEmbedding = bytesToFloats(entity.embedding!!)
                entity to cosineSimilarity(embedding, entityEmbedding)
            }
            .maxByOrNull { it.second }
    }

    suspend fun decayAndPrune() {
        memoryDao.decayAutoConfidence()
        memoryDao.pruneWeakMemories()
    }

    suspend fun enforceMemoryCap(maxAuto: Int = 200) {
        val autoCount = memoryDao.getAutoCount()
        if (autoCount > maxAuto) {
            memoryDao.deleteLowestConfidence(autoCount - maxAuto)
        }
    }

    companion object {
        fun floatsToBytes(floats: FloatArray): ByteArray {
            val buffer = ByteBuffer.allocate(floats.size * 4).order(ByteOrder.LITTLE_ENDIAN)
            buffer.asFloatBuffer().put(floats)
            return buffer.array()
        }

        fun bytesToFloats(bytes: ByteArray): FloatArray {
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val floats = FloatArray(bytes.size / 4)
            buffer.asFloatBuffer().get(floats)
            return floats
        }

        fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
            if (a.size != b.size) return 0f
            var dot = 0f
            var normA = 0f
            var normB = 0f
            for (i in a.indices) {
                dot += a[i] * b[i]
                normA += a[i] * a[i]
                normB += b[i] * b[i]
            }
            val denom = sqrt(normA) * sqrt(normB)
            return if (denom > 0f) dot / denom else 0f
        }
    }
}
