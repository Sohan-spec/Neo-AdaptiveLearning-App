package com.neo.android.engine

class LlamaCppBridge {

    companion object {
        private var isLoaded = false

        fun loadLibrary(): Boolean {
            return try {
                if (!isLoaded) {
                    System.loadLibrary("llm_bridge")
                    isLoaded = true
                }
                true
            } catch (e: UnsatisfiedLinkError) {
                false
            }
        }

        fun isLibraryLoaded(): Boolean = isLoaded
    }

    external fun loadModel(path: String, contextSize: Int): Int
    external fun unloadModel()
    external fun isModelLoaded(): Boolean
    external fun runInference(prompt: String, maxTokens: Int, callback: InferenceCallback): Int
    external fun stopInference()
    external fun getVocabSize(): Int
    external fun getContextSize(): Int
}

interface InferenceCallback {
    fun onToken(token: String)
    fun onComplete()
    fun onError(message: String)
}
