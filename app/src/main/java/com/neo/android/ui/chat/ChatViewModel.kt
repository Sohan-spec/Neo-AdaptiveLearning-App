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

data class Message(
    val role: String,
    val content: String,
    val isTyping: Boolean = false,
    val visibleChars: Int = content.length,
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
            switchToChat(existing.first().id)
        } else {
            createNewChat()
        }
    }

    fun createNewChat() {
        viewModelScope.launch {
            val chatId = repo.createChat("New Chat")
            switchToChat(chatId)
        }
    }

    fun switchToChat(chatId: Long) {
        inferenceJob?.cancel()
        isThinking.value = false
        currentChatId = chatId
        _messages.clear()

        viewModelScope.launch {
            repo.getMessagesForChat(chatId).collectLatest { entities ->
                _messages.clear()
                _messages.addAll(entities.map { e ->
                    Message(role = e.role, content = e.content)
                })
            }
        }

        // Refresh active state in sidebar
        _chatSessions.value = _chatSessions.value.map { it.copy(isActive = it.id == chatId) }
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
    private fun buildSystemPrompt(): String {
        val usageInfo = if (UsageStatsHelper.hasPermission(context)) {
            val apps = UsageStatsHelper.getTopApps(context, 3)
            UsageStatsHelper.formatForPrompt(apps)
        } else {
            "Usage data not available."
        }
        return "You are Neo, a concise and friendly on-device AI assistant. " +
            "User's top apps today:\n$usageInfo\n" +
            "Use this context to be helpful. Keep responses brief and natural."
    }

    private fun buildPrompt(history: List<Message>): String {
        val sb = StringBuilder()
        sb.append("<|begin_of_text|>")
        sb.append("<|start_header_id|>system<|end_header_id|>\n\n")
        sb.append(buildSystemPrompt())
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
            // Fallback: show error message
            _messages.add(Message("assistant", "Model not loaded. Please restart the app."))
            return
        }

        isThinking.value = true

        // Build prompt from current conversation history (exclude streaming message)
        val history = _messages.toList().filter { !it.isTyping }
        val prompt = buildPrompt(history)

        // Add streaming placeholder
        val streamIdx = _messages.size
        _messages.add(Message("assistant", "", isTyping = true, visibleChars = 0))
        isThinking.value = false // hide typing indicator, show streaming message instead

        inferenceJob = viewModelScope.launch {
            val responseBuilder = StringBuilder()

            LlmEngine.runInference(prompt, maxTokens = 400)
                .catch { e ->
                    Log.e(TAG, "Inference error", e)
                    if (streamIdx < _messages.size) {
                        val errorMsg = "Sorry, something went wrong. Please try again."
                        _messages[streamIdx] = Message("assistant", errorMsg)
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

            // Finalize: mark done + persist
            val finalText = responseBuilder.toString().trim()
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

            // If initiated via mic or live mode, speak the reply
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
        speechManager.destroy()
        viewModelScope.launch { LlmEngine.unloadModel() }
    }
}
