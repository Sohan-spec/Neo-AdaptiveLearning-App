package com.neo.android.ui.memory

data class MemoryEntry(
    val id: Long,
    val category: String,
    val title: String,
    val content: String,
    val confidence: Float,
    val updatedAt: Long,
    val source: String = "auto",
)
