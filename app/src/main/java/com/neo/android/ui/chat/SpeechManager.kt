package com.neo.android.ui.chat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import java.util.Locale

class SpeechManager(
    context: Context,
    private val onPartialResult: (String) -> Unit,
    private val onFinalResult: (String) -> Unit,
    private val onSttError: (Int) -> Unit,
    private val onTtsDone: () -> Unit,
) {
    private val appContext = context.applicationContext

    // ── STT ──────────────────────────────────────────────────
    private var speechRecognizer: SpeechRecognizer? = null

    private val recognizerIntent: Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                2000L,
            )
        }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}

        override fun onPartialResults(partialResults: Bundle?) {
            val texts = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!texts.isNullOrEmpty()) {
                onPartialResult(texts[0])
            }
        }

        override fun onResults(results: Bundle?) {
            val texts = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!texts.isNullOrEmpty() && texts[0].isNotBlank()) {
                onFinalResult(texts[0])
            } else {
                // No speech detected — treat as error so UI resets
                onSttError(SpeechRecognizer.ERROR_NO_MATCH)
            }
        }

        override fun onError(error: Int) {
            onSttError(error)
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun startListening() {
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(appContext).also {
            it.setRecognitionListener(recognitionListener)
            it.startListening(recognizerIntent)
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
    }

    // ── TTS ──────────────────────────────────────────────────
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    init {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
                configureTtsVoice()
            }
        }
    }

    private fun configureTtsVoice() {
        val engine = tts ?: return

        // Try to pick a female en-US voice
        val femaleVoice: Voice? = try {
            engine.voices
                ?.filter { v ->
                    v.locale.language == "en" &&
                        !v.isNetworkConnectionRequired
                }
                ?.firstOrNull { v ->
                    v.name.contains("female", ignoreCase = true)
                }
        } catch (_: Exception) {
            null
        }

        if (femaleVoice != null) {
            engine.setVoice(femaleVoice)
        } else {
            engine.setLanguage(Locale.US)
            engine.setPitch(0.9f) // slightly higher → softer, feminine tone
        }

        engine.setSpeechRate(0.95f) // gentle pace

        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                onTtsDone()
            }
            @Deprecated("Deprecated in API 21")
            override fun onError(utteranceId: String?) {}
            override fun onError(utteranceId: String?, errorCode: Int) {
                onTtsDone() // reset UI even on error
            }
        })
    }

    fun speak(text: String) {
        if (!ttsReady) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "neo_reply")
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    val isSpeaking: Boolean
        get() = tts?.isSpeaking == true

    // ── Lifecycle ────────────────────────────────────────────
    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        tts?.stop()
        tts?.shutdown()
        tts = null
        ttsReady = false
    }
}
