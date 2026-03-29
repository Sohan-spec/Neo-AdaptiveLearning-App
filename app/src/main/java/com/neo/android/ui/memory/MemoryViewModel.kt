package com.neo.android.ui.memory

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neo.android.data.AppDatabase
import com.neo.android.data.MemoryRepository
import com.neo.android.data.entity.MemoryEntity
import com.neo.android.engine.EmbeddingEngine
import com.neo.android.engine.MemoryExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MemoryViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "MemoryViewModel"
    private val db = AppDatabase.getInstance(application)
    private val memoryRepo = MemoryRepository(db.memoryDao())

    // ── Category filter ──────────────────────────────────────
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    // ── All memories from Room ───────────────────────────────
    private val allMemories: StateFlow<List<MemoryEntry>> =
        memoryRepo.getAllMemoriesFlow()
            .map { entities -> entities.map { it.toEntry() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Filtered memories ────────────────────────────────────
    val memories: StateFlow<List<MemoryEntry>> =
        combine(allMemories, _selectedCategory) { all, category ->
            if (category == null) all else all.filter { it.category == category }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Derived stats ────────────────────────────────────────
    val totalCount: StateFlow<Int> =
        allMemories.map { it.size }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val categoryCount: StateFlow<Int> =
        allMemories.map { it.map { m -> m.category }.distinct().size }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val lastUpdated: StateFlow<String> =
        allMemories.map { entries ->
            val newest = entries.maxByOrNull { it.updatedAt }
            if (newest != null) formatRelativeTime(newest.updatedAt) else "Never"
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Never")

    val categories: StateFlow<List<String>> =
        allMemories.map { entries ->
            listOf("All") + entries.map { it.category }.distinct().sorted()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("All"))

    // ── Extraction state ─────────────────────────────────────
    val isExtracting: StateFlow<Boolean> = MemoryExtractor.isExtracting

    // ── Actions ──────────────────────────────────────────────
    fun selectCategory(category: String?) {
        _selectedCategory.value = if (category == "All") null else category
    }

    fun addMemory(category: String, title: String, content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val textToEmbed = "$title. $content"
                val embedding = EmbeddingEngine.embed(textToEmbed)
                val embeddingBytes = if (embedding != null) {
                    MemoryRepository.floatsToBytes(embedding)
                } else null

                memoryRepo.insertMemory(
                    MemoryEntity(
                        category = category,
                        title = title,
                        content = content,
                        confidence = 1.0f,
                        embedding = embeddingBytes,
                        source = "manual",
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add memory", e)
            }
        }
    }

    fun editMemory(id: Long, category: String, title: String, content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val existing = memoryRepo.getById(id) ?: return@launch
                val textToEmbed = "$title. $content"
                val embedding = EmbeddingEngine.embed(textToEmbed)
                val embeddingBytes = if (embedding != null) {
                    MemoryRepository.floatsToBytes(embedding)
                } else existing.embedding

                memoryRepo.updateMemory(
                    existing.copy(
                        category = category,
                        title = title,
                        content = content,
                        embedding = embeddingBytes,
                        updatedAtMillis = System.currentTimeMillis(),
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to edit memory", e)
            }
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                memoryRepo.deleteMemory(id)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete memory", e)
            }
        }
    }

    private fun MemoryEntity.toEntry() = MemoryEntry(
        id = id,
        category = category,
        title = title,
        content = content,
        confidence = confidence,
        updatedAt = updatedAtMillis,
        source = source,
    )

    private fun formatRelativeTime(millis: Long): String {
        val diff = System.currentTimeMillis() - millis
        val minutes = diff / 60_000
        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            minutes < 1440 -> "${minutes / 60}h ago"
            minutes < 2880 -> "Yesterday"
            else -> "${minutes / 1440}d ago"
        }
    }
}
