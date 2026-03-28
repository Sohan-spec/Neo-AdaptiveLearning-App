package com.neo.android.model

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

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

    fun downloadModel(context: Context): Flow<DownloadState> = flow {
        if (isModelReady(context)) {
            emit(DownloadState.Ready)
            return@flow
        }

        emit(DownloadState.Downloading(0f))

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        // Check if there's already a pending/running download for this file
        val downloadId: Long
        try {
            val request = DownloadManager.Request(Uri.parse(MODEL_URL)).apply {
                setTitle("Downloading Llama 3.2 1B")
                setDescription("Preparing Neo AI model…")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, MODEL_FILENAME)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(false)
            }
            downloadId = dm.enqueue(request)
        } catch (e: Exception) {
            emit(DownloadState.Error("Failed to start download: ${e.message}"))
            return@flow
        }

        // Poll progress
        var isComplete = false
        while (!isComplete) {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor: Cursor? = dm.query(query)

            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val statusIdx = c.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val dlIdx = c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val totalIdx = c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

                    val status = c.getInt(statusIdx)
                    val bytesDownloaded = c.getLong(dlIdx)
                    val bytesTotal = c.getLong(totalIdx)

                    when (status) {
                        DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PENDING -> {
                            val progress = if (bytesTotal > 0) {
                                (bytesDownloaded.toFloat() / bytesTotal).coerceIn(0f, 1f)
                            } else 0f
                            emit(DownloadState.Downloading(progress))
                        }
                        DownloadManager.STATUS_SUCCESSFUL -> isComplete = true
                        DownloadManager.STATUS_FAILED -> {
                            val reasonIdx = c.getColumnIndex(DownloadManager.COLUMN_REASON)
                            val reason = c.getInt(reasonIdx)
                            emit(DownloadState.Error("Download failed (code: $reason)"))
                            return@flow
                        }
                        DownloadManager.STATUS_PAUSED -> { /* keep polling */ }
                    }
                }
            }

            if (!isComplete) delay(500)
        }

        // Move from Downloads to internal storage
        val downloadedFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            MODEL_FILENAME
        )

        if (!downloadedFile.exists()) {
            emit(DownloadState.Error("Downloaded file not found"))
            return@flow
        }

        emit(DownloadState.MovingToInternal)

        val targetFile = File(modelsDir(context), MODEL_FILENAME)

        try {
            FileInputStream(downloadedFile).use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                    output.flush()
                }
            }
            downloadedFile.delete()
            Log.i(TAG, "Model moved to ${targetFile.absolutePath}")
            emit(DownloadState.Ready)
        } catch (e: Exception) {
            targetFile.delete()
            emit(DownloadState.Error("Failed to move model: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
}
