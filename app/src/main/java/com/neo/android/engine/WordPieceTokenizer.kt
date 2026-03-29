package com.neo.android.engine

import android.content.Context
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Lightweight WordPiece tokenizer for all-MiniLM-L6-v2.
 * Reads vocab from assets/tokenizer_vocab.txt (one token per line).
 */
class WordPieceTokenizer private constructor(
    private val vocab: Map<String, Int>,
    private val idToToken: Map<Int, String>,
) {
    companion object {
        private const val CLS_TOKEN = "[CLS]"
        private const val SEP_TOKEN = "[SEP]"
        private const val UNK_TOKEN = "[UNK]"
        private const val PAD_TOKEN = "[PAD]"
        private const val MAX_SEQ_LENGTH = 128
        private const val MAX_WORD_CHARS = 100

        fun loadFromAssets(context: Context, filename: String = "tokenizer_vocab.txt"): WordPieceTokenizer {
            val vocab = mutableMapOf<String, Int>()
            context.assets.open(filename).use { stream ->
                BufferedReader(InputStreamReader(stream)).use { reader ->
                    var index = 0
                    reader.forEachLine { line ->
                        vocab[line] = index
                        index++
                    }
                }
            }
            val idToToken = vocab.entries.associate { it.value to it.key }
            return WordPieceTokenizer(vocab, idToToken)
        }
    }

    data class TokenizedInput(
        val inputIds: LongArray,
        val attentionMask: LongArray,
        val tokenTypeIds: LongArray,
    )

    fun tokenize(text: String): TokenizedInput {
        val tokens = mutableListOf(CLS_TOKEN)
        val words = basicTokenize(text)

        for (word in words) {
            val subTokens = wordPieceTokenize(word)
            tokens.addAll(subTokens)
            if (tokens.size >= MAX_SEQ_LENGTH - 1) break
        }
        tokens.add(SEP_TOKEN)

        // Truncate to max length
        val truncated = if (tokens.size > MAX_SEQ_LENGTH) {
            tokens.subList(0, MAX_SEQ_LENGTH - 1) + listOf(SEP_TOKEN)
        } else {
            tokens
        }

        val inputIds = LongArray(MAX_SEQ_LENGTH)
        val attentionMask = LongArray(MAX_SEQ_LENGTH)
        val tokenTypeIds = LongArray(MAX_SEQ_LENGTH)

        val padId = vocab[PAD_TOKEN] ?: 0

        for (i in truncated.indices) {
            inputIds[i] = (vocab[truncated[i]] ?: vocab[UNK_TOKEN] ?: 0).toLong()
            attentionMask[i] = 1L
            tokenTypeIds[i] = 0L
        }
        for (i in truncated.size until MAX_SEQ_LENGTH) {
            inputIds[i] = padId.toLong()
            attentionMask[i] = 0L
            tokenTypeIds[i] = 0L
        }

        return TokenizedInput(inputIds, attentionMask, tokenTypeIds)
    }

    fun getTokenCount(text: String): Int {
        val tokens = mutableListOf(CLS_TOKEN)
        val words = basicTokenize(text)
        for (word in words) {
            tokens.addAll(wordPieceTokenize(word))
        }
        tokens.add(SEP_TOKEN)
        return tokens.size.coerceAtMost(MAX_SEQ_LENGTH)
    }

    private fun basicTokenize(text: String): List<String> {
        // Lowercase, strip accents, split on whitespace and punctuation
        val normalized = text.lowercase().trim()
        val tokens = mutableListOf<String>()
        val current = StringBuilder()

        for (c in normalized) {
            when {
                c.isWhitespace() -> {
                    if (current.isNotEmpty()) {
                        tokens.add(current.toString())
                        current.clear()
                    }
                }
                isPunctuation(c) -> {
                    if (current.isNotEmpty()) {
                        tokens.add(current.toString())
                        current.clear()
                    }
                    tokens.add(c.toString())
                }
                else -> current.append(c)
            }
        }
        if (current.isNotEmpty()) tokens.add(current.toString())
        return tokens
    }

    private fun wordPieceTokenize(word: String): List<String> {
        if (word.length > MAX_WORD_CHARS) return listOf(UNK_TOKEN)

        val tokens = mutableListOf<String>()
        var start = 0

        while (start < word.length) {
            var end = word.length
            var found: String? = null

            while (start < end) {
                val substr = if (start == 0) {
                    word.substring(start, end)
                } else {
                    "##" + word.substring(start, end)
                }
                if (vocab.containsKey(substr)) {
                    found = substr
                    break
                }
                end--
            }

            if (found == null) {
                tokens.add(UNK_TOKEN)
                break
            }
            tokens.add(found)
            start = end
        }
        return tokens
    }

    private fun isPunctuation(c: Char): Boolean {
        val cp = c.code
        // ASCII punctuation ranges
        if ((cp in 33..47) || (cp in 58..64) || (cp in 91..96) || (cp in 123..126)) return true
        // Unicode general punctuation
        if (Character.getType(c).toByte() == Character.OTHER_PUNCTUATION.toByte()) return true
        return false
    }
}
