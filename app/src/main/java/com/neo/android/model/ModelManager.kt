package com.neo.android.model

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

sealed class DownloadState {
    data object Idle : DownloadState()
    data class Downloading(val progress: Float) : DownloadState()
    data object MovingToInternal : DownloadState()
    data object Ready : DownloadState()
    data class Error(val message: String) : DownloadState()
}

object ModelManager {

    private const val TAG = "ModelManager"
    private const val MODEL_FILENAME = "Llama-3.2-1B-Instruct-Q4_K_M.gguf"
    private const val MODEL_URL =
        "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf"

    private const val EMBEDDING_MODEL_FILENAME = "all-MiniLM-L6-v2.onnx"
    private const val EMBEDDING_MODEL_URL =
        "https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model.onnx"

    private const val CONNECT_TIMEOUT_MS = 30_000
    private const val READ_TIMEOUT_MS = 60_000
    private const val BUFFER_SIZE = 8192
    private const val MAX_REDIRECTS = 5

    private fun modelsDir(context: Context): File {
        val dir = File(context.filesDir, "models")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getModelPath(context: Context): String =
        File(modelsDir(context), MODEL_FILENAME).absolutePath

    fun isModelReady(context: Context): Boolean {
        val file = File(modelsDir(context), MODEL_FILENAME)
        return file.exists() && file.length() > 0
    }

    fun getEmbeddingModelPath(context: Context): String =
        File(modelsDir(context), EMBEDDING_MODEL_FILENAME).absolutePath

    fun isEmbeddingModelReady(context: Context): Boolean {
        val file = File(modelsDir(context), EMBEDDING_MODEL_FILENAME)
        return file.exists() && file.length() > 0
    }

    /**
     * Downloads a file from [url] directly to internal storage at [targetFile].
     * Uses HttpURLConnection with manual redirect following for reliability.
     * Supports resuming partial downloads.
     */
    private fun downloadFile(
        url: String,
        targetFile: File,
        label: String,
    ): Flow<DownloadState> = flow {
        // Check if already fully downloaded
        if (targetFile.exists() && targetFile.length() > 0) {
            // Quick size check — if the file seems complete, mark ready
            Log.i(TAG, "$label already exists (${targetFile.length()} bytes)")
            emit(DownloadState.Ready)
            return@flow
        }

        emit(DownloadState.Downloading(0f))

        // Use a temp file for atomic writes
        val tempFile = File(targetFile.parent, "${targetFile.name}.tmp")
        val existingBytes = if (tempFile.exists()) tempFile.length() else 0L

        try {
            var currentUrl = url
            var connection: HttpURLConnection? = null
            var redirectCount = 0

            // Follow redirects manually (HuggingFace redirects to CDN)
            while (redirectCount < MAX_REDIRECTS) {
                val urlObj = URL(currentUrl)
                connection = (urlObj.openConnection() as HttpURLConnection).apply {
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    instanceFollowRedirects = false
                    setRequestProperty("User-Agent", "Neo-Android-App/1.0")
                    // Resume support
                    if (existingBytes > 0) {
                        setRequestProperty("Range", "bytes=$existingBytes-")
                    }
                }

                val responseCode = connection.responseCode

                if (responseCode in 300..399) {
                    val redirectUrl = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (redirectUrl.isNullOrEmpty()) {
                        emit(DownloadState.Error("$label: Empty redirect URL"))
                        return@flow
                    }
                    currentUrl = if (redirectUrl.startsWith("http")) {
                        redirectUrl
                    } else {
                        // Relative redirect
                        URL(URL(currentUrl), redirectUrl).toString()
                    }
                    redirectCount++
                    Log.d(TAG, "$label redirect #$redirectCount → $currentUrl")
                    continue
                }

                if (responseCode != 200 && responseCode != 206) {
                    connection.disconnect()
                    emit(DownloadState.Error("$label: HTTP $responseCode"))
                    return@flow
                }

                // We have a valid connection — start downloading
                val isResuming = responseCode == 206
                val totalBytes = if (isResuming) {
                    val contentRange = connection.getHeaderField("Content-Range")
                    // Format: bytes start-end/total
                    contentRange?.substringAfter("/")?.toLongOrNull()
                        ?: (connection.contentLength.toLong() + existingBytes)
                } else {
                    connection.contentLength.toLong()
                }

                Log.i(TAG, "$label: starting download — total=$totalBytes, resuming=$isResuming, existing=$existingBytes")

                connection.inputStream.use { input ->
                    FileOutputStream(tempFile, isResuming).use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var bytesRead: Int
                        var totalDownloaded = if (isResuming) existingBytes else 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalDownloaded += bytesRead

                            val progress = if (totalBytes > 0) {
                                (totalDownloaded.toFloat() / totalBytes).coerceIn(0f, 1f)
                            } else {
                                0f
                            }
                            emit(DownloadState.Downloading(progress))
                        }
                        output.flush()
                    }
                }

                connection.disconnect()

                // Rename temp file to final location
                if (tempFile.exists()) {
                    if (targetFile.exists()) targetFile.delete()
                    if (tempFile.renameTo(targetFile)) {
                        Log.i(TAG, "$label: download complete — ${targetFile.length()} bytes")
                        emit(DownloadState.Ready)
                    } else {
                        emit(DownloadState.Error("$label: Failed to finalize file"))
                    }
                } else {
                    emit(DownloadState.Error("$label: Temp file missing after download"))
                }
                return@flow
            }

            // If we got here, too many redirects
            emit(DownloadState.Error("$label: Too many redirects"))
        } catch (e: Exception) {
            Log.e(TAG, "$label: Download failed", e)
            // Don't delete temp file — allows resuming next time
            emit(DownloadState.Error("$label: ${e.message ?: "Unknown error"}"))
        }
    }.flowOn(Dispatchers.IO)

    fun downloadModel(context: Context): Flow<DownloadState> {
        val targetFile = File(modelsDir(context), MODEL_FILENAME)
        return downloadFile(MODEL_URL, targetFile, "AI Model")
    }

    fun downloadEmbeddingModel(context: Context): Flow<DownloadState> {
        val targetFile = File(modelsDir(context), EMBEDDING_MODEL_FILENAME)
        return downloadFile(EMBEDDING_MODEL_URL, targetFile, "Embedding Model")
    }
}
