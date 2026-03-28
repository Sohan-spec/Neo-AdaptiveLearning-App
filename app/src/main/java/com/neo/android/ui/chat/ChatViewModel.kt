package com.neo.android.ui.chat

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class Message(
    val role: String,
    val content: String,
    val isTyping: Boolean = false,   // true while typewriter is running
    val visibleChars: Int = content.length, // how many chars to show
)

private val cannedResponses = listOf(
    "Got it. I'll keep that in mind.",
    "That's a good point. Want me to help break that down?",
    "Sure, I can help with that.",
    "Interesting — tell me more.",
    "On it. Is there anything else you want me to consider?",
    "Makes sense. What would you like to do next?",
)

class ChatViewModel : ViewModel() {

    private val _messages = mutableStateListOf(
        Message("assistant", "Hello!")
    )
    val messages: List<Message> get() = _messages

    val isThinking = mutableStateOf(false)

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        _messages.add(Message("user", trimmed))
        isThinking.value = true

        viewModelScope.launch {
            delay(1500L)
            val response = cannedResponses[(_messages.size - 1) % cannedResponses.size]
            isThinking.value = false

            // Insert message with 0 visible chars (typewriter start)
            val idx = _messages.size
            _messages.add(Message("assistant", response, isTyping = true, visibleChars = 0))

            // Reveal chars one by one
            for (i in 1..response.length) {
                delay(22L) // ~45 chars/sec — natural reading speed
                _messages[idx] = _messages[idx].copy(visibleChars = i)
            }
            // Done typing
            _messages[idx] = _messages[idx].copy(isTyping = false, visibleChars = response.length)
        }
    }
}
