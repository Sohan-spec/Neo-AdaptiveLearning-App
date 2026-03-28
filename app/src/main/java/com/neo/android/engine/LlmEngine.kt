package com.neo.android.engine

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

object LlmEngine {

    private const val TAG = "LlmEngine"

    private val bridge = LlamaCppBridge()

    private val _state = MutableStateFlow<LlmState>(LlmState.Idle)
    val state: StateFlow<LlmState> = _state.asStateFlow()

    private var nativeAvailable: Boolean = false

    init {
        nativeAvailable = LlamaCppBridge.loadLibrary()
        if (!nativeAvailable) {
            _state.value = LlmState.NativeLibraryMissing()
        }
    }

    suspend fun loadModel(modelPath: String, contextSize: Int = 8192): Boolean {
        if (!nativeAvailable) {
            _state.value = LlmState.NativeLibraryMissing()
            return false
        }

        if (bridge.isModelLoaded()) {
            unloadModel()
        }

        _state.value = LlmState.Loading(modelPath)
        Log.i(TAG, "Loading model: $modelPath (context: $contextSize)")

        return withContext(Dispatchers.IO) {
            try {
                val result = bridge.loadModel(modelPath, contextSize)
                when (result) {
                    0 -> {
                        val vocabSize = bridge.getVocabSize()
                        val actualContextSize = bridge.getContextSize()
                        _state.value = LlmState.Ready(modelPath, actualContextSize, vocabSize)
                        Log.i(TAG, "Model loaded. Vocab: $vocabSize, Context: $actualContextSize")
                        true
                    }
                    -1 -> {
                        _state.value = LlmState.Error("Failed to load model file.")
                        false
                    }
                    -2 -> {
                        _state.value = LlmState.Error("Failed to create context. Try a smaller context window.")
                        false
                    }
                    else -> {
                        _state.value = LlmState.Error("Unknown error loading model (code: $result)")
                        false
                    }
                }
            } catch (e: OutOfMemoryError) {
                _state.value = LlmState.Error("Out of memory. Model too large for this device.", recoverable = false)
                try { bridge.unloadModel() } catch (_: Exception) {}
                System.gc()
                false
            } catch (e: Exception) {
                _state.value = LlmState.Error("Error loading model: ${e.message}")
                false
            }
        }
    }

    suspend fun unloadModel() {
        if (!nativeAvailable) return
        withContext(Dispatchers.IO) {
            try { bridge.unloadModel() } catch (e: Exception) { Log.e(TAG, "Error unloading", e) }
        }
        _state.value = LlmState.Idle
    }

    fun runInference(prompt: String, maxTokens: Int = 400): Flow<String> = callbackFlow {
        if (!nativeAvailable) throw IllegalStateException("Native library not available")
        if (!bridge.isModelLoaded()) throw IllegalStateException("No model loaded")

        var tokenCount = 0
        val responseBuilder = StringBuilder()
        _state.value = LlmState.Generating(partialResponse = "", tokenCount = 0)

        val callback = object : InferenceCallback {
            override fun onToken(token: String) {
                tokenCount++
                responseBuilder.append(token)
                _state.value = LlmState.Generating(responseBuilder.toString(), tokenCount)
                trySend(token)
            }

            override fun onComplete() {
                Log.i(TAG, "Inference complete. Generated $tokenCount tokens.")
                val currentState = _state.value
                if (currentState is LlmState.Generating) {
                    _state.value = LlmState.Ready("", bridge.getContextSize(), bridge.getVocabSize())
                }
                close()
            }

            override fun onError(message: String) {
                Log.e(TAG, "Inference error: $message")
                _state.value = LlmState.Error(message)
                close(RuntimeException(message))
            }
        }

        try {
            val result = bridge.runInference(prompt, maxTokens, callback)
            if (result < 0) {
                _state.value = LlmState.Error("Inference failed (code: $result)")
                close(RuntimeException("Inference failed with code: $result"))
            }
        } catch (e: OutOfMemoryError) {
            _state.value = LlmState.Error("Out of memory during inference.", recoverable = false)
            close(e)
        } catch (e: Exception) {
            _state.value = LlmState.Error("Inference error: ${e.message}")
            close(e)
        }

        awaitClose {
            Log.d(TAG, "Inference flow cancelled")
            stopInference()
        }
    }.flowOn(Dispatchers.IO)

    fun stopInference() {
        if (!nativeAvailable) return
        try { bridge.stopInference() } catch (e: Exception) { Log.e(TAG, "Error stopping", e) }
    }

    fun isReady(): Boolean = _state.value is LlmState.Ready
}
