# Spec: Gemma Inference Engine

**Priority:** P0 — validate before writing any other code  
**Why first:** Only real technical unknown. If Gemma doesn't load on device, everything else is moot.

---

## What It Does
Wraps `LlmInference` (MediaPipe Tasks GenAI) to provide:
1. Single-turn inference (smoke test)
2. Multi-turn inference with full message history
3. Compression inference (separate call, 100 token cap)

---

## Acceptance Criteria
- [ ] Model loads without crash on target device
- [ ] Single-turn response generated in <10s
- [ ] Response content is coherent English, <150 tokens
- [ ] No OOM error (device has sufficient RAM for ~700MB model)
- [ ] Inference runs on background thread — main thread never blocked
- [ ] Multi-turn: system prompt preserved across turns

---

## Interface

```kotlin
interface InferenceEngine {
    suspend fun generate(messages: List<Message>): String
    suspend fun compress(turns: List<Message>): String
    fun close()
}
```

---

## Implementation

```kotlin
class LiteRTInferenceEngine(context: Context) : InferenceEngine {

    private val model: LlmInference = LlmInference.createFromOptions(
        context,
        LlmInference.LlmInferenceOptions.builder()
            .setModelPath("/data/local/tmp/gemma3-1b-it-int4.bin")
            .setMaxTokens(150)
            .setTemperature(0.7f)
            .setTopK(40)
            .build()
    )

    override suspend fun generate(messages: List<Message>): String =
        withContext(Dispatchers.IO) {
            val prompt = messages.toGemmaPrompt()
            model.generateResponse(prompt)
        }

    override suspend fun compress(turns: List<Message>): String =
        withContext(Dispatchers.IO) {
            // Separate options: maxTokens=100 for compression
            val compressionPrompt = buildCompressionPrompt(turns)
            // Note: may need a second LlmInference instance with maxTokens=100
            model.generateResponse(compressionPrompt)
        }

    override fun close() = model.close()
}
```

### Prompt formatting for Gemma 3
```kotlin
fun List<Message>.toGemmaPrompt(): String = buildString {
    forEach { msg ->
        when (msg.role) {
            "system" -> append("<start_of_turn>user\n${msg.content}<end_of_turn>\n")
            "user"   -> append("<start_of_turn>user\n${msg.content}<end_of_turn>\n")
            "assistant" -> append("<start_of_turn>model\n${msg.content}<end_of_turn>\n")
        }
    }
    append("<start_of_turn>model\n")
}
```

---

## Smoke Test (run immediately after model loads)

```kotlin
val engine = LiteRTInferenceEngine(context)
val testMessages = listOf(
    Message("system", "You are Neo, a friendly assistant.", System.currentTimeMillis()),
    Message("user", "Say hello in one sentence.", System.currentTimeMillis())
)
val response = engine.generate(testMessages)
Log.d("NeoTest", "Response: $response")
// Expected: coherent single sentence, <150 tokens
```

---

## Fallback (if model fails to load)

```kotlin
class FallbackInferenceEngine : InferenceEngine {
    override suspend fun generate(messages: List<Message>): String {
        return "Looks like you've had a busy day! You spent the most time on Instagram and Spotify. Want me to help you plan a quieter evening?"
    }
    override suspend fun compress(turns: List<Message>): String {
        return "User asked about their day and received a summary."
    }
    override fun close() {}
}
```

Swap `LiteRTInferenceEngine` for `FallbackInferenceEngine` in the ViewModel constructor if model load fails. Demo still runs.

---

## Tasks

- [ ] Add `com.google.mediapipe:tasks-genai:latest` to `build.gradle.kts`
- [ ] Push model: `adb push gemma3-1b-it-int4.bin /data/local/tmp/`
- [ ] Create `LiteRTInferenceEngine.kt`
- [ ] Create `FallbackInferenceEngine.kt`
- [ ] Run smoke test, log output
- [ ] Confirm device RAM is sufficient (>3GB free recommended)
- [ ] Confirm inference time is acceptable (<10s)
