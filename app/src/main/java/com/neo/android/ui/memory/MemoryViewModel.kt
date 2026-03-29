package com.neo.android.ui.memory

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MemoryViewModel : ViewModel() {

    private val mockMemories = listOf(
        MemoryEntry(
            id = "1",
            category = "Preferences",
            title = "Prefers concise responses",
            content = "User tends to ask follow-up questions when responses are too long. Prefers bullet-point answers and short paragraphs over detailed explanations.",
            confidence = 0.92f,
            updatedAt = System.currentTimeMillis() - 2 * 60 * 60 * 1000, // 2h ago
        ),
        MemoryEntry(
            id = "2",
            category = "Facts",
            title = "Software developer",
            content = "Works as an Android developer. Primarily uses Kotlin and Jetpack Compose. Familiar with MVVM architecture and coroutines.",
            confidence = 0.88f,
            updatedAt = System.currentTimeMillis() - 5 * 60 * 60 * 1000, // 5h ago
        ),
        MemoryEntry(
            id = "3",
            category = "Interests",
            title = "Interested in AI/ML",
            content = "Frequently asks about on-device machine learning, LLM inference, and vector databases. Exploring RAG implementations for mobile.",
            confidence = 0.85f,
            updatedAt = System.currentTimeMillis() - 1 * 24 * 60 * 60 * 1000, // 1d ago
        ),
        MemoryEntry(
            id = "4",
            category = "Habits",
            title = "Codes late at night",
            content = "Most conversations happen between 10 PM and 2 AM. Usually works on personal projects during this time rather than work tasks.",
            confidence = 0.78f,
            updatedAt = System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000, // 2d ago
        ),
        MemoryEntry(
            id = "5",
            category = "Context",
            title = "Building a personal AI assistant",
            content = "Currently working on an Android app called Neo that runs LLMs locally. Uses llama.cpp for inference with quantized models.",
            confidence = 0.95f,
            updatedAt = System.currentTimeMillis() - 30 * 60 * 1000, // 30m ago
        ),
        MemoryEntry(
            id = "6",
            category = "Preferences",
            title = "Dark mode enthusiast",
            content = "Prefers dark UI themes across all applications. Has mentioned eye strain with light backgrounds during night coding sessions.",
            confidence = 0.72f,
            updatedAt = System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000, // 3d ago
        ),
        MemoryEntry(
            id = "7",
            category = "Facts",
            title = "Located in the US",
            content = "Based on timezone references and language patterns, user appears to be located in the United States, likely Pacific timezone.",
            confidence = 0.65f,
            updatedAt = System.currentTimeMillis() - 5 * 24 * 60 * 60 * 1000, // 5d ago
        ),
        MemoryEntry(
            id = "8",
            category = "Interests",
            title = "Privacy-focused computing",
            content = "Strongly prefers on-device processing over cloud APIs. Values data privacy and frequently discusses local-first architectures.",
            confidence = 0.90f,
            updatedAt = System.currentTimeMillis() - 12 * 60 * 60 * 1000, // 12h ago
        ),
        MemoryEntry(
            id = "9",
            category = "Habits",
            title = "Iterative development style",
            content = "Tends to build features incrementally, testing each small change. Prefers quick feedback loops and frequent builds.",
            confidence = 0.82f,
            updatedAt = System.currentTimeMillis() - 4 * 24 * 60 * 60 * 1000, // 4d ago
        ),
        MemoryEntry(
            id = "10",
            category = "Context",
            title = "Uses neumorphic design system",
            content = "The Neo app uses a custom spatial/neumorphic design language with glassmorphism elements. DM Sans font, accent blue #4D7BFF.",
            confidence = 0.97f,
            updatedAt = System.currentTimeMillis() - 1 * 60 * 60 * 1000, // 1h ago
        ),
    )

    val categories: List<String> = listOf("All") +
        mockMemories.map { it.category }.distinct().sorted()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _memories = MutableStateFlow(mockMemories)
    val memories: StateFlow<List<MemoryEntry>> = _memories.asStateFlow()

    val totalCount: Int = mockMemories.size

    val categoryCount: Int = mockMemories.map { it.category }.distinct().size

    val lastUpdated: String = "Just now"

    fun selectCategory(category: String?) {
        val effective = if (category == "All") null else category
        _selectedCategory.value = effective
        _memories.value = if (effective == null) {
            mockMemories
        } else {
            mockMemories.filter { it.category == effective }
        }
    }
}
