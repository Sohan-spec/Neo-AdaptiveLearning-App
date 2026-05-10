package com.neo.android.engine

import android.util.Log
import com.neo.android.data.MemoryRepository
import com.neo.android.data.entity.MemoryEntity
import com.neo.android.ui.chat.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RawMemory(
    val category: String,
    val title: String,
    val content: String,
)

/**
 * Rule-based memory extractor that pulls personal facts from user messages
 * using pattern matching. This is much more reliable than LLM-based extraction,
 * especially with small (1B) models.
 */
object MemoryExtractor {

    private const val TAG = "MemoryExtractor"

    private val _isExtracting = MutableStateFlow(false)
    val isExtracting: StateFlow<Boolean> = _isExtracting.asStateFlow()

    // ── Pattern definitions ──────────────────────────────────────
    // Each pattern: Regex → (category, titleGenerator)
    // We only match USER messages, never assistant messages.

    private data class ExtractionPattern(
        val regex: Regex,
        val category: String,
        val titleFn: (MatchResult) -> String,
        val contentFn: (MatchResult) -> String,
    )

    private val patterns = listOf(
        // Name patterns
        ExtractionPattern(
            regex = Regex("""(?:my name is|i'm called|call me|i am)\s+([A-Z][a-z]+(?:\s+[A-Z][a-z]+)*)""", RegexOption.IGNORE_CASE),
            category = "Facts",
            titleFn = { "Name" },
            contentFn = { m -> m.groupValues[1].trim() },
        ),
        // Age patterns
        ExtractionPattern(
            regex = Regex("""(?:i'm|i am|im)\s+(\d{1,3})\s*(?:years?\s*old|yrs?\s*old|yo)""", RegexOption.IGNORE_CASE),
            category = "Facts",
            titleFn = { "Age" },
            contentFn = { m -> "${m.groupValues[1]} years old" },
        ),
        // Location patterns
        ExtractionPattern(
            regex = Regex("""(?:i live in|i'm from|i am from|i stay in|i reside in)\s+(.+?)(?:\.|,|$)""", RegexOption.IGNORE_CASE),
            category = "Facts",
            titleFn = { "Location" },
            contentFn = { m -> m.groupValues[1].trim() },
        ),
        // Occupation patterns
        ExtractionPattern(
            regex = Regex("""(?:i work as|i'm a|i am a|i am an|i'm an|my job is|i work in|my profession is)\s+(.+?)(?:\.|,|$)""", RegexOption.IGNORE_CASE),
            category = "Facts",
            titleFn = { "Occupation" },
            contentFn = { m -> m.groupValues[1].trim() },
        ),
        // Student patterns
        ExtractionPattern(
            regex = Regex("""(?:i study|i'm studying|i am studying|i'm a student|i am a student)\s*(?:at|in|of)?\s*(.+?)(?:\.|,|$)""", RegexOption.IGNORE_CASE),
            category = "Facts",
            titleFn = { "Education" },
            contentFn = { m -> "Studies ${m.groupValues[1].trim()}" },
        ),
        // Likes/Interests
        ExtractionPattern(
            regex = Regex("""(?:i (?:really )?(?:like|love|enjoy|adore))\s+(.+?)(?:\.|,|$)""", RegexOption.IGNORE_CASE),
            category = "Interests",
            titleFn = { m ->
                val topic = m.groupValues[1].trim().split(" ").take(2).joinToString(" ")
                topic.replaceFirstChar { it.uppercase() }
            },
            contentFn = { m -> "Enjoys ${m.groupValues[1].trim()}" },
        ),
        // Dislikes
        ExtractionPattern(
            regex = Regex("""(?:i (?:really )?(?:hate|dislike|don't like|can't stand))\s+(.+?)(?:\.|,|$)""", RegexOption.IGNORE_CASE),
            category = "Preferences",
            titleFn = { m ->
                val topic = m.groupValues[1].trim().split(" ").take(2).joinToString(" ")
                topic.replaceFirstChar { it.uppercase() }
            },
            contentFn = { m -> "Dislikes ${m.groupValues[1].trim()}" },
        ),
        // Preferences
        ExtractionPattern(
            regex = Regex("""(?:i prefer)\s+(.+?)(?:\.|,|$)""", RegexOption.IGNORE_CASE),
            category = "Preferences",
            titleFn = { m ->
                val topic = m.groupValues[1].trim().split(" ").take(2).joinToString(" ")
                topic.replaceFirstChar { it.uppercase() }
            },
            contentFn = { m -> "Prefers ${m.groupValues[1].trim()}" },
        ),
        // Hobby patterns
        ExtractionPattern(
            regex = Regex("""(?:my hobbies?\s+(?:is|are|include))\s+(.+?)(?:\.|,|$)""", RegexOption.IGNORE_CASE),
            category = "Interests",
            titleFn = { "Hobbies" },
            contentFn = { m -> m.groupValues[1].trim() },
        ),
        // Favorite patterns
        ExtractionPattern(
            regex = Regex("""(?:my fav(?:ou?rite)?\s+(\w+)\s+is)\s+(.+?)(?:\.|,|$)""", RegexOption.IGNORE_CASE),
            category = "Preferences",
            titleFn = { m -> "Favorite ${m.groupValues[1].trim()}" },
            contentFn = { m -> m.groupValues[2].trim() },
        ),
        // Pet patterns
        ExtractionPattern(
            regex = Regex("""(?:i have a|i own a|i've got a)\s+(dog|cat|pet|bird|fish|hamster|rabbit)(?:\s+(?:named|called)\s+(\w+))?""", RegexOption.IGNORE_CASE),
            category = "Facts",
            titleFn = { "Pet" },
            contentFn = { m ->
                val pet = m.groupValues[1].trim()
                val name = m.groupValues[2].trim()
                if (name.isNotEmpty()) "Has a $pet named $name" else "Has a $pet"
            },
        ),
        // Language patterns
        ExtractionPattern(
            regex = Regex("""(?:i speak|i know)\s+(english|hindi|spanish|french|german|chinese|japanese|korean|arabic|portuguese|bengali|tamil|telugu|marathi|gujarati|urdu|kannada|malayalam|punjabi|odia)""", RegexOption.IGNORE_CASE),
            category = "Facts",
            titleFn = { "Language" },
            contentFn = { m -> "Speaks ${m.groupValues[1].trim()}" },
        ),
    )

    suspend fun extractAndStore(
        messages: List<Message>,
        memoryRepo: MemoryRepository,
    ) {
        if (messages.isEmpty()) return
        _isExtracting.value = true
        try {
            // Only look at USER messages
            val userMessages = messages.filter { it.role == "user" }
            if (userMessages.isEmpty()) return

            val rawMemories = mutableListOf<RawMemory>()

            for (msg in userMessages) {
                val text = msg.content
                for (pattern in patterns) {
                    val match = pattern.regex.find(text) ?: continue
                    val content = pattern.contentFn(match)
                    // Skip very short or garbage matches
                    if (content.length < 2 || content.length > 100) continue

                    rawMemories.add(
                        RawMemory(
                            category = pattern.category,
                            title = pattern.titleFn(match),
                            content = content,
                        )
                    )
                }
            }

            if (rawMemories.isEmpty()) {
                Log.d(TAG, "No memories extracted from conversation (rule-based)")
                return
            }

            Log.i(TAG, "Extracted ${rawMemories.size} memories (rule-based)")
            for (raw in rawMemories) {
                Log.d(TAG, "  → [${raw.category}] ${raw.title}: ${raw.content}")
            }

            // Deduplicate and store
            upsertMemories(rawMemories, memoryRepo)

            // Enforce cap
            memoryRepo.enforceMemoryCap(200)
        } catch (e: Exception) {
            Log.e(TAG, "Memory extraction failed", e)
        } finally {
            _isExtracting.value = false
        }
    }

    private suspend fun upsertMemories(
        rawMemories: List<RawMemory>,
        memoryRepo: MemoryRepository,
    ) {
        for (raw in rawMemories) {
            try {
                val textToEmbed = "${raw.title}. ${raw.content}"
                val embedding = EmbeddingEngine.embed(textToEmbed)

                if (embedding == null) {
                    // Embedding engine not available — insert without embedding
                    Log.w(TAG, "Embedding engine not ready, inserting without embedding: ${raw.title}")
                    memoryRepo.insertMemory(
                        MemoryEntity(
                            category = raw.category,
                            title = raw.title,
                            content = raw.content,
                            confidence = 0.90f,
                            embedding = null,
                            source = "auto",
                        )
                    )
                    continue
                }

                val embeddingBytes = MemoryRepository.floatsToBytes(embedding)

                // Find best match in existing memories
                val bestMatch = memoryRepo.findBestMatch(embedding)

                if (bestMatch != null && bestMatch.second > 0.85f) {
                    // Merge: update existing memory with new content
                    val existing = bestMatch.first
                    memoryRepo.updateMemory(
                        existing.copy(
                            content = raw.content,
                            confidence = (existing.confidence + 0.05f).coerceAtMost(0.99f),
                            embedding = embeddingBytes,
                            updatedAtMillis = System.currentTimeMillis(),
                        )
                    )
                    Log.i(TAG, "Merged memory: ${raw.title} → ${raw.content} (similarity: ${bestMatch.second})")
                } else {
                    // Insert new memory
                    memoryRepo.insertMemory(
                        MemoryEntity(
                            category = raw.category,
                            title = raw.title,
                            content = raw.content,
                            confidence = 0.90f,
                            embedding = embeddingBytes,
                            source = "auto",
                        )
                    )
                    Log.i(TAG, "Inserted new memory: ${raw.title} → ${raw.content}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upsert memory: ${raw.title}", e)
            }
        }
    }
}
