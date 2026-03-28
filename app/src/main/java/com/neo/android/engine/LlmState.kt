package com.neo.android.engine

sealed class LlmState {
    data object Idle : LlmState()
    data class NativeLibraryMissing(val message: String = "Native llama.cpp library not found") : LlmState()
    data class Loading(val modelPath: String) : LlmState()
    data class Ready(val modelPath: String, val contextSize: Int, val vocabSize: Int) : LlmState()
    data class Generating(val partialResponse: String, val tokenCount: Int) : LlmState()
    data class Error(val message: String, val recoverable: Boolean = true) : LlmState()
}
