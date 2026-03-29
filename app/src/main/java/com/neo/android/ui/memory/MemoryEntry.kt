package com.neo.android.ui.memory

data class MemoryEntry(
    val id: String,
    val category: String,
    val title: String,
    val content: String,
    val confidence: Float,
    val updatedAt: Long,
)
