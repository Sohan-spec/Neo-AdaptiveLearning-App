package com.neo.android.engine

import android.content.Context
import android.util.Log
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.nio.LongBuffer
import kotlin.math.sqrt

object EmbeddingEngine {

    private const val TAG = "EmbeddingEngine"
    private const val EMBEDDING_DIM = 384

    private var environment: OrtEnvironment? = null
    private var session: OrtSession? = null
    private var tokenizer: WordPieceTokenizer? = null
    private val mutex = Mutex()

    val isReady: Boolean get() = session != null && tokenizer != null

    suspend fun loadModel(context: Context, modelPath: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                if (session != null) return@withContext

                tokenizer = WordPieceTokenizer.loadFromAssets(context)
                environment = OrtEnvironment.getEnvironment()
                session = environment!!.createSession(
                    modelPath,
                    OrtSession.SessionOptions().apply {
                        setIntraOpNumThreads(2)
                    },
                )
                Log.i(TAG, "Embedding model loaded from $modelPath")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load embedding model", e)
                session = null
                tokenizer = null
            }
        }
    }

    suspend fun embed(text: String): FloatArray? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val sess = session ?: return@withContext null
            val tok = tokenizer ?: return@withContext null
            val env = environment ?: return@withContext null

            try {
                val tokenized = tok.tokenize(text)
                val seqLen = tokenized.inputIds.size.toLong()

                val inputIdsTensor = OnnxTensor.createTensor(
                    env,
                    LongBuffer.wrap(tokenized.inputIds),
                    longArrayOf(1, seqLen),
                )
                val attentionMaskTensor = OnnxTensor.createTensor(
                    env,
                    LongBuffer.wrap(tokenized.attentionMask),
                    longArrayOf(1, seqLen),
                )
                val tokenTypeIdsTensor = OnnxTensor.createTensor(
                    env,
                    LongBuffer.wrap(tokenized.tokenTypeIds),
                    longArrayOf(1, seqLen),
                )

                val inputs = mapOf(
                    "input_ids" to inputIdsTensor,
                    "attention_mask" to attentionMaskTensor,
                    "token_type_ids" to tokenTypeIdsTensor,
                )

                val result = sess.run(inputs)

                // Output shape: [1, seq_len, 384] — mean pool over seq_len
                @Suppress("UNCHECKED_CAST")
                val output = result[0].value as Array<Array<FloatArray>>
                val tokenEmbeddings = output[0] // [seq_len][384]

                // Mean pooling with attention mask
                val pooled = FloatArray(EMBEDDING_DIM)
                var validTokens = 0f
                for (i in tokenEmbeddings.indices) {
                    if (tokenized.attentionMask[i] == 1L) {
                        for (j in 0 until EMBEDDING_DIM) {
                            pooled[j] += tokenEmbeddings[i][j]
                        }
                        validTokens++
                    }
                }
                if (validTokens > 0) {
                    for (j in 0 until EMBEDDING_DIM) {
                        pooled[j] /= validTokens
                    }
                }

                // L2 normalize
                val normalized = l2Normalize(pooled)

                // Cleanup tensors
                inputIdsTensor.close()
                attentionMaskTensor.close()
                tokenTypeIdsTensor.close()
                result.close()

                normalized
            } catch (e: Exception) {
                Log.e(TAG, "Embedding failed for text: ${text.take(50)}", e)
                null
            }
        }
    }

    suspend fun unloadModel() = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                session?.close()
                environment?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error unloading embedding model", e)
            }
            session = null
            environment = null
            tokenizer = null
            Log.i(TAG, "Embedding model unloaded")
        }
    }

    private fun l2Normalize(vec: FloatArray): FloatArray {
        var norm = 0f
        for (v in vec) norm += v * v
        norm = sqrt(norm)
        if (norm < 1e-12f) return vec
        return FloatArray(vec.size) { vec[it] / norm }
    }
}
