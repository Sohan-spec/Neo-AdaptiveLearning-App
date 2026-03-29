package com.neo.android.engine

import android.util.Log
import com.neo.android.data.MemoryRepository
import com.neo.android.data.entity.MemoryEntity
import com.neo.android.ui.chat.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect

data class RawMemory(
    val category: String,
    val title: String,
    val content: String,
)

object MemoryExtractor {

    private const val TAG = "MemoryExtractor"

    private val VALID_CATEGORIES = setOf("Preferences", "Facts", "Interests", "Habits", "Context")

    private val EXTRACTION_REGEX =
        """\[(Preferences|Facts|Interests|Habits|Context)]\s*(.+?):\s*(.+)""".toRegex()

    // Secondary: matches "Category: Title: Content" (model omits square brackets)
    private val SECONDARY_REGEX =
        """(Preferences|Facts|Interests|Habits|Context)[:\s]+(.+?):\s*(.+)""".toRegex(RegexOption.IGNORE_CASE)

    private val _isExtracting = MutableStateFlow(false)
    val isExtracting: StateFlow<Boolean> = _isExtracting.asStateFlow()

    suspend fun extractAndStore(
        messages: List<Message>,
        memoryRepo: MemoryRepository,
    ) {
        if (messages.size < 2) return
        _isExtracting.value = true
        try {
            // 1. Decay existing auto-extracted memories
            memoryRepo.decayAndPrune()

            // 2. Extract raw memories from conversation via LLM
            val rawMemories = extractMemories(messages)
            if (rawMemories.isEmpty()) {
                Log.d(TAG, "No memories extracted from conversation")
                return
            }
            Log.i(TAG, "Extracted ${rawMemories.size} raw memories")

            // 3. Deduplicate and store
            upsertMemories(rawMemories, memoryRepo)

            // 4. Enforce cap
            memoryRepo.enforceMemoryCap(200)
        } catch (e: Exception) {
            Log.e(TAG, "Memory extraction failed", e)
        } finally {
            _isExtracting.value = false
        }
    }

    private suspend fun extractMemories(messages: List<Message>): List<RawMemory> {
        val conversationText = buildConversationText(messages)
        val prompt = buildExtractionPrompt(conversationText)

        val responseBuilder = StringBuilder()
        try {
            LlmEngine.runInference(prompt, maxTokens = 300)
                .catch { e -> Log.e(TAG, "Extraction inference error", e) }
                .collect { token -> responseBuilder.append(token) }
        } catch (e: Exception) {
            Log.e(TAG, "Extraction inference failed", e)
            return emptyList()
        }

        val rawOutput = responseBuilder.toString()
        Log.d(TAG, "Raw LLM extraction output: $rawOutput")
        return parseExtractionOutput(rawOutput)
    }

    private fun buildConversationText(messages: List<Message>): String {
        return messages.joinToString("\n") { msg ->
            val role = if (msg.role == "user") "User" else "Assistant"
            "$role: ${msg.content}"
        }
    }

    private fun buildExtractionPrompt(conversation: String): String {
        return buildString {
            append("<|begin_of_text|>")
            append("<|start_header_id|>system<|end_header_id|>\n\n")
            append("Extract key facts about the user from the conversation below.\n")
            append("Use ONLY this exact format, one fact per line:\n")
            append("[Category] Title: Description\n\n")
            append("Valid categories: Preferences, Facts, Interests, Habits, Context\n\n")
            append("Rules:\n")
            append("- Only output facts explicitly stated by the USER, not the assistant\n")
            append("- Output 1-5 facts maximum\n")
            append("- If no clear user facts exist, output nothing at all\n")
            append("- Do NOT add numbers, bullets, explanations, or any other text\n\n")
            append("Example output format:\n")
            append("[Facts] Occupation: Works as an Android developer\n")
            append("[Preferences] Response style: Prefers concise answers\n")
            append("[Interests] Gaming: Enjoys mobile games\n")
            append("<|eot_id|>")
            append("<|start_header_id|>user<|end_header_id|>\n\n")
            append("Conversation:\n")
            append(conversation.take(2000))
            append("<|eot_id|>")
            append("<|start_header_id|>assistant<|end_header_id|>\n\n")
        }
    }

    private fun parseExtractionOutput(output: String): List<RawMemory> {
        val results = mutableListOf<RawMemory>()
        // Skip preamble phrases the model sometimes prepends before facts
        val skipPrefixes = listOf("here", "based on", "i found", "i identified", "note:", "example", "sure")

        for (line in output.lines()) {
            if (results.size >= 5) break
            val trimmed = line.trim()
            if (trimmed.isBlank()) continue
            if (skipPrefixes.any { trimmed.startsWith(it, ignoreCase = true) }) continue

            // Strip leading numbers/bullets: "1. ", "2) ", "- ", "* ", "• "
            val cleaned = trimmed.replace(Regex("""^\d+[.)\]]\s*|^[-*•]\s*"""), "")

            // 1st pass: strict [Category] Title: Content
            val primary = EXTRACTION_REGEX.find(cleaned)
            if (primary != null) {
                val (category, title, content) = primary.destructured
                if (title.isNotBlank() && content.isNotBlank()) {
                    results.add(RawMemory(category = category, title = title.trim(), content = content.trim()))
                    continue
                }
            }

            // 2nd pass: Category: Title: Content (model dropped square brackets)
            val secondary = SECONDARY_REGEX.find(cleaned)
            if (secondary != null) {
                val (category, title, content) = secondary.destructured
                val normalizedCategory = VALID_CATEGORIES.firstOrNull {
                    it.equals(category.trim(), ignoreCase = true)
                } ?: continue
                if (title.isNotBlank() && content.isNotBlank()) {
                    results.add(RawMemory(category = normalizedCategory, title = title.trim(), content = content.trim()))
                }
            }
        }

        Log.d(TAG, "Parsed ${results.size} memories")
        return results
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
                    memoryRepo.insertMemory(
                        MemoryEntity(
                            category = raw.category,
                            title = raw.title,
                            content = raw.content,
                            confidence = 0.70f,
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
                    // Merge: update existing memory
                    val existing = bestMatch.first
                    memoryRepo.updateMemory(
                        existing.copy(
                            content = raw.content,
                            confidence = (existing.confidence + 0.05f).coerceAtMost(0.99f),
                            embedding = embeddingBytes,
                            updatedAtMillis = System.currentTimeMillis(),
                        )
                    )
                    Log.d(TAG, "Merged memory: ${raw.title} (similarity: ${bestMatch.second})")
                } else {
                    // Insert new memory
                    memoryRepo.insertMemory(
                        MemoryEntity(
                            category = raw.category,
                            title = raw.title,
                            content = raw.content,
                            confidence = 0.70f,
                            embedding = embeddingBytes,
                            source = "auto",
                        )
                    )
                    Log.d(TAG, "Inserted new memory: ${raw.title}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upsert memory: ${raw.title}", e)
            }
        }
    }
}
