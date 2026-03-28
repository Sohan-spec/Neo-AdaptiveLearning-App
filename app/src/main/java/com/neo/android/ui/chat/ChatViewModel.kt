package com.neo.android.ui.chat

import android.app.Application
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neo.android.data.AppDatabase
import com.neo.android.data.ChatRepository
import com.neo.android.data.entity.ChatEntity
import com.neo.android.data.entity.MessageEntity
import com.neo.android.engine.LlmEngine
import com.neo.android.model.ModelManager
import com.neo.android.usage.UsageStatsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class Message(
    val role: String,
    val content: String,
    val isTyping: Boolean = false,
    val visibleChars: Int = content.length,
    val isDebugOnly: Boolean = false,  // shown in UI but never sent to LLM or persisted to DB
)

data class ChatSession(
    val id: Long,
    val title: String,
    val preview: String,
    val timestamp: String,
    val isActive: Boolean = false,
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "ChatViewModel"
    private val context get() = getApplication<Application>()

    // ── Data layer ───────────────────────────────────────────
    private val db = AppDatabase.getInstance(application)
    private val repo = ChatRepository(db.chatDao(), db.messageDao())

    // ── Display messages (Room + streaming overlay) ──────────
    private val _messages = mutableStateListOf<Message>()
    val messages: List<Message> get() = _messages

    val isThinking = mutableStateOf(false)

    // ── Current chat tracking ───────────────────────────────
    private var currentChatId: Long? = null
    private var inferenceJob: Job? = null
    private var loadMessagesJob: Job? = null
    private var modelLoaded = false

    // ── Chat sessions ────────────────────────────────────────
    private val _chatSessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val chatSessions: StateFlow<List<ChatSession>> = _chatSessions.asStateFlow()

    // ── Voice state (mic button) ────────────────────────────
    val micState = mutableStateOf(MicState.IDLE)
    val partialText = mutableStateOf("")
    private var isVoiceMode = false

    // ── Live mode state ──────────────────────────────────────
    val isLiveMode = mutableStateOf(false)
    val liveMicState = mutableStateOf(MicState.IDLE)
    val livePartialText = mutableStateOf("")

    private val speechManager = SpeechManager(
        context = application,
        onPartialResult = { text ->
            if (isLiveMode.value) {
                livePartialText.value = text
            } else {
                partialText.value = text
            }
        },
        onFinalResult = { text ->
            if (isLiveMode.value) {
                livePartialText.value = ""
                liveMicState.value = MicState.IDLE
                sendMessage(text)
            } else {
                partialText.value = ""
                micState.value = MicState.IDLE
                sendMessage(text)
            }
        },
        onSttError = { errorCode ->
            if (isLiveMode.value) {
                livePartialText.value = ""
                val fatal = errorCode == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS
                if (!fatal && liveMicState.value == MicState.LISTENING) {
                    viewModelScope.launch(Dispatchers.Main) {
                        delay(400L)
                        if (isLiveMode.value && liveMicState.value == MicState.LISTENING) {
                            liveStartListening()
                        }
                    }
                } else if (fatal) {
                    liveMicState.value = MicState.IDLE
                    isLiveMode.value = false
                }
            } else {
                partialText.value = ""
                micState.value = MicState.IDLE
                isVoiceMode = false
            }
        },
        onTtsDone = {
            if (isLiveMode.value) {
                viewModelScope.launch(Dispatchers.Main) {
                    delay(500L)
                    if (isLiveMode.value) liveStartListening()
                }
            } else {
                micState.value = MicState.IDLE
                isVoiceMode = false
            }
        },
    )

    init {
        // Observe chat sessions from Room
        viewModelScope.launch {
            repo.getAllChatsFlow().collectLatest { entities ->
                _chatSessions.value = entities.map { entity ->
                    ChatSession(
                        id = entity.id,
                        title = entity.title,
                        preview = "",
                        timestamp = formatTimestamp(entity.updatedAtMillis),
                        isActive = entity.id == currentChatId,
                    )
                }
            }
        }
        // Load model + create or restore initial chat
        viewModelScope.launch {
            loadLlmModel()
            ensureCurrentChat()
        }
    }

    // ── LLM Model ────────────────────────────────────────────
    private suspend fun loadLlmModel() {
        val modelPath = ModelManager.getModelPath(context)
        if (!ModelManager.isModelReady(context)) {
            Log.w(TAG, "Model file not found at $modelPath")
            return
        }
        modelLoaded = LlmEngine.loadModel(modelPath, contextSize = 8192)
        Log.i(TAG, "Model loaded: $modelLoaded")
    }

    // ── Chat management ──────────────────────────────────────
    private suspend fun ensureCurrentChat() {
        if (currentChatId != null) return
        val existing = repo.getAllChatsFlow().first()
        if (existing.isNotEmpty()) {
            switchToChat(existing.first().id, showDebugBubble = false)
        } else {
            // Brand new install — treat exactly like tapping New Chat
            val chatId = repo.createChat("New Chat")
            switchToChat(chatId, showDebugBubble = true)
        }
    }

    fun createNewChat() {
        viewModelScope.launch {
            val chatId = repo.createChat("New Chat")
            switchToChat(chatId, showDebugBubble = true)
        }
    }

    fun switchToChat(chatId: Long, showDebugBubble: Boolean = false) {
        inferenceJob?.cancel()
        loadMessagesJob?.cancel()
        isThinking.value = false
        currentChatId = chatId
        _messages.clear()
        // Update sidebar active state immediately
        _chatSessions.value = _chatSessions.value.map { it.copy(isActive = it.id == chatId) }

        loadMessagesJob = viewModelScope.launch(Dispatchers.IO) {
            val entities = repo.getMessagesForChat(chatId).first()
            val mapped = entities.map { e -> Message(role = e.role, content = e.content) }
            withContext(Dispatchers.Main) {
                _messages.clear()
                _messages.addAll(mapped)
            }
            // Only inject debug bubble into genuinely new (empty) chats
            if (showDebugBubble && entities.isEmpty()) {
                val debugText = buildDebugUsageBubble()
                withContext(Dispatchers.Main) {
                    _messages.add(Message(role = "assistant", content = debugText, isDebugOnly = true))
                }
            }
        }
    }

    /** Fetches usage stats on IO and returns a formatted debug string shown as the first bubble. */
    private suspend fun buildDebugUsageBubble(): String = withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        sb.appendLine("📊 Debug — Usage Stats")
        sb.appendLine()

        val hasPermission = UsageStatsHelper.hasPermission(context)
        sb.appendLine("Permission granted: $hasPermission")

        if (!hasPermission) {
            sb.appendLine()
            sb.appendLine("⚠️ Usage access NOT granted.")
            sb.appendLine("Go to Settings → Apps → Special app access → Usage access → enable Neo.")
            return@withContext sb.toString().trimEnd()
        }

        // Raw unfiltered results — shows exactly what OS returned
        val raw = UsageStatsHelper.getRawStatsForDebug(context)
        sb.appendLine("Raw entries with foreground time (last 24h): ${raw.size}")
        if (raw.isEmpty()) {
            sb.appendLine()
            sb.appendLine("⚠️ OS returned 0 entries with foreground time.")
            sb.appendLine("Make sure you have used other apps in the last 24 hours.")
        } else {
            sb.appendLine()
            sb.appendLine("Top raw entries (unfiltered):")
            raw.take(10).forEach { (name, pkg, mins) ->
                sb.appendLine("  • $name ($pkg) — ${mins}min")
            }
        }

        // Filtered results — what actually goes into the system prompt
        val filtered = UsageStatsHelper.getTopApps(context, 3)
        sb.appendLine()
        sb.appendLine("Filtered top-3 sent to LLM: ${filtered.size}")
        if (filtered.isNotEmpty()) {
            filtered.forEach { app ->
                sb.appendLine("  ✓ ${app.appName} — ${app.totalMinutes}min")
            }
        }

        sb.appendLine()
        sb.appendLine("─────────────────────")
        sb.appendLine("Normal chat starts now. Ask me what apps you've used!")
        sb.toString().trimEnd()
    }

    fun deleteChat(chatId: Long) {
        viewModelScope.launch {
            repo.deleteChat(chatId)
            if (currentChatId == chatId) {
                currentChatId = null
                _messages.clear()
                ensureCurrentChat()
            }
        }
    }

    // ── Prompt building ──────────────────────────────────────

    // Called on IO dispatcher — UsageStatsManager.queryAndAggregateUsageStats() can
    // return empty results if invoked on the main thread on some Android versions.
    private suspend fun buildSystemPromptOnIo(): String = withContext(Dispatchers.IO) {
        val usageSection: String? = if (UsageStatsHelper.hasPermission(context)) {
            val apps = UsageStatsHelper.getTopApps(context, 3)
            if (apps.isNotEmpty()) {
                UsageStatsHelper.formatForPrompt(apps).also {
                    Log.d(TAG, "Usage stats fetched: $it")
                }
            } else {
                Log.w(TAG, "Usage permission granted but no app data returned (too early in day?)")
                null
            }
        } else {
            Log.w(TAG, "Usage stats permission NOT granted — omitting from prompt")
            null
        }

        val sb = StringBuilder()
        sb.append("You are Neo, a concise personal AI assistant running fully on-device.\n\n")

        if (usageSection != null) {
            // Only claim knowledge when we actually have real data
            sb.append("REAL DATA — The user's most-used Android apps in the last 24 hours (actual device screen time):\n")
            sb.append(usageSection)
            sb.append("\n\n")
            sb.append("Rules:\n")
            sb.append("- When asked about app usage, quote the EXACT app names and times from the data above. NEVER invent numbers.\n")
            sb.append("- Use this context to give personalized responses where relevant.\n")
        } else {
            sb.append("Rules:\n")
            sb.append("- You do NOT have access to this user's app usage data. If asked about usage or screen time, say so clearly. Do NOT make up numbers.\n")
        }
        sb.append("- Answer all questions directly and accurately.\n")
        sb.append("- Keep responses brief — 1-3 sentences unless the user asks for more detail.")

        sb.toString()
    }

    private fun buildPrompt(history: List<Message>, systemPrompt: String): String {
        val sb = StringBuilder()
        sb.append("<|begin_of_text|>")
        sb.append("<|start_header_id|>system<|end_header_id|>\n\n")
        sb.append(systemPrompt)
        sb.append("<|eot_id|>")
        for (msg in history) {
            sb.append("<|start_header_id|>${msg.role}<|end_header_id|>\n\n")
            sb.append(msg.content)
            sb.append("<|eot_id|>")
        }
        sb.append("<|start_header_id|>assistant<|end_header_id|>\n\n")
        return sb.toString()
    }

    // ── Mic button handlers ──────────────────────────────────
    fun onMicClick() {
        when (micState.value) {
            MicState.IDLE -> startListening()
            MicState.LISTENING -> {
                speechManager.stopListening()
                partialText.value = ""
                micState.value = MicState.IDLE
                isVoiceMode = false
            }
            MicState.SPEAKING -> {
                speechManager.stopSpeaking()
                startListening()
            }
        }
    }

    private fun startListening() {
        if (speechManager.isSpeaking) speechManager.stopSpeaking()
        isVoiceMode = true
        partialText.value = ""
        micState.value = MicState.LISTENING
        speechManager.startListening()
    }

    // ── Live mode handlers ───────────────────────────────────
    fun onLiveOpen() {
        isLiveMode.value = true
        livePartialText.value = ""
        liveStartListening()
    }

    fun onLiveClose() {
        isLiveMode.value = false
        speechManager.stopListening()
        speechManager.stopSpeaking()
        liveMicState.value = MicState.IDLE
        livePartialText.value = ""
    }

    private fun liveStartListening() {
        if (speechManager.isSpeaking) speechManager.stopSpeaking()
        livePartialText.value = ""
        liveMicState.value = MicState.LISTENING
        speechManager.startListening()
    }

    // ── Send message ─────────────────────────────────────────
    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        val chatId = currentChatId ?: return

        // Add user message to display + persist
        _messages.add(Message("user", trimmed))

        viewModelScope.launch {
            repo.insertMessage(
                MessageEntity(
                    chatId = chatId,
                    role = "user",
                    content = trimmed,
                    isComplete = true,
                    createdAtMillis = System.currentTimeMillis(),
                )
            )
            repo.updateChatTimestamp(chatId)

            // Auto-title from first user message
            val sessions = _chatSessions.value
            val current = sessions.find { it.id == chatId }
            if (current?.title == "New Chat") {
                val title = trimmed.take(30) + if (trimmed.length > 30) "…" else ""
                viewModelScope.launch(Dispatchers.IO) {
                    db.chatDao().updateTitle(chatId, title)
                }
            }

            runInference(chatId)
        }
    }

    private fun runInference(chatId: Long) {
        if (!modelLoaded) {
            _messages.add(Message("assistant", "Model not loaded. Please restart the app."))
            return
        }

        isThinking.value = true

        // Snapshot history on Main before any suspension — this is the source of truth
        // for what the model sees. Excludes streaming placeholders and debug-only bubbles.
        val historySnapshot = _messages.toList().filter { !it.isTyping && !it.isDebugOnly }

        // Add streaming placeholder immediately so UI shows activity
        val streamIdx = _messages.size
        _messages.add(Message("assistant", "", isTyping = true, visibleChars = 0))
        isThinking.value = false

        inferenceJob = viewModelScope.launch {
            // Build system prompt on IO — UsageStatsManager can block or return empty on Main
            val systemPrompt = buildSystemPromptOnIo()
            val prompt = buildPrompt(historySnapshot, systemPrompt)

            // Log the full prompt so you can verify in Logcat what the model actually receives
            Log.d(TAG, "════ PROMPT SENT TO LLM [chat=$chatId, turns=${historySnapshot.size}] ════")
            Log.d(TAG, prompt)
            Log.d(TAG, "════ END PROMPT ════")

            val responseBuilder = StringBuilder()

            LlmEngine.runInference(prompt, maxTokens = 400)
                .catch { e ->
                    Log.e(TAG, "Inference error", e)
                    if (streamIdx < _messages.size) {
                        _messages[streamIdx] = Message("assistant", "Sorry, something went wrong. Please try again.")
                    }
                }
                .collect { token ->
                    responseBuilder.append(token)
                    val currentText = responseBuilder.toString()
                    if (streamIdx < _messages.size) {
                        _messages[streamIdx] = Message(
                            "assistant",
                            currentText,
                            isTyping = true,
                            visibleChars = currentText.length,
                        )
                    }
                }

            val finalText = responseBuilder.toString().trim()
            Log.d(TAG, "LLM response: $finalText")

            if (streamIdx < _messages.size) {
                _messages[streamIdx] = Message("assistant", finalText)
            }

            if (finalText.isNotEmpty()) {
                repo.insertMessage(
                    MessageEntity(
                        chatId = chatId,
                        role = "assistant",
                        content = finalText,
                        isComplete = true,
                        createdAtMillis = System.currentTimeMillis(),
                    )
                )
                repo.updateChatTimestamp(chatId)
            }

            if (isVoiceMode || isLiveMode.value) {
                if (isLiveMode.value) {
                    liveMicState.value = MicState.SPEAKING
                } else {
                    micState.value = MicState.SPEAKING
                }
                speechManager.speak(finalText)
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────
    private fun formatTimestamp(millis: Long): String {
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

    override fun onCleared() {
        super.onCleared()
        inferenceJob?.cancel()
        loadMessagesJob?.cancel()
        speechManager.destroy()
        viewModelScope.launch { LlmEngine.unloadModel() }
    }
}
