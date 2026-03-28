#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <mutex>
#include <memory>
#include <chrono>
#include <thread>

#include "llama.h"
#include "common.h"

#define LOG_TAG "LlmBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// ─── Global State ───────────────────────────────────────────────

static std::mutex g_mutex;
static llama_model *g_model = nullptr;
static llama_context *g_ctx = nullptr;
static llama_sampler *g_sampler = nullptr;
static bool g_is_generating = false;

// ─── Helper: Clean up all resources ─────────────────────────────

static void cleanup() {
    if (g_sampler) {
        llama_sampler_free(g_sampler);
        g_sampler = nullptr;
    }
    if (g_ctx) {
        llama_free(g_ctx);
        g_ctx = nullptr;
    }
    if (g_model) {
        llama_model_free(g_model);
        g_model = nullptr;
    }
}

// ─── JNI Functions ──────────────────────────────────────────────

extern "C" {

JNIEXPORT jint JNICALL
Java_com_neo_android_engine_LlamaCppBridge_loadModel(
    JNIEnv *env, jobject /* this */,
    jstring path, jint contextSize) {

    std::lock_guard<std::mutex> lock(g_mutex);

    // Clean up any previous model
    cleanup();

    const char *model_path = env->GetStringUTFChars(path, nullptr);
    if (!model_path) {
        LOGE("Failed to get model path string");
        return -1;
    }

    LOGI("Loading model: %s (context: %d)", model_path, contextSize);

    // Initialize llama backend
    llama_backend_init();

    // Load the model
    llama_model_params model_params = llama_model_default_params();
    model_params.use_mmap = true;

    g_model = llama_model_load_from_file(model_path, model_params);
    env->ReleaseStringUTFChars(path, model_path);

    if (!g_model) {
        LOGE("Failed to load model");
        llama_backend_free();
        return -1;
    }

    // Create context
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = contextSize;
    ctx_params.n_batch = 512;
    ctx_params.n_threads = 4;

    g_ctx = llama_init_from_model(g_model, ctx_params);
    if (!g_ctx) {
        LOGE("Failed to create context");
        llama_model_free(g_model);
        g_model = nullptr;
        llama_backend_free();
        return -2;
    }

    // Create sampler chain
    llama_sampler_chain_params sparams = llama_sampler_chain_default_params();
    g_sampler = llama_sampler_chain_init(sparams);
    llama_sampler_chain_add(g_sampler, llama_sampler_init_temp(0.8f));
    llama_sampler_chain_add(g_sampler, llama_sampler_init_top_p(0.95f, 1));
    llama_sampler_chain_add(g_sampler, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));

    LOGI("Model loaded successfully. Context size: %d", contextSize);
    return 0;
}

JNIEXPORT void JNICALL
Java_com_neo_android_engine_LlamaCppBridge_unloadModel(
    JNIEnv *env, jobject /* this */) {

    std::lock_guard<std::mutex> lock(g_mutex);
    LOGI("Unloading model");
    g_is_generating = false;
    cleanup();
    llama_backend_free();
}

JNIEXPORT jboolean JNICALL
Java_com_neo_android_engine_LlamaCppBridge_isModelLoaded(
    JNIEnv *env, jobject /* this */) {
    return g_model != nullptr && g_ctx != nullptr;
}

JNIEXPORT jint JNICALL
Java_com_neo_android_engine_LlamaCppBridge_runInference(
    JNIEnv *env, jobject /* this */,
    jstring prompt, jint maxTokens, jobject callback) {

    std::lock_guard<std::mutex> lock(g_mutex);

    if (!g_model || !g_ctx || !g_sampler) {
        LOGE("No model loaded");
        return -1;
    }

    const char *prompt_cstr = env->GetStringUTFChars(prompt, nullptr);
    if (!prompt_cstr) return -1;

    std::string prompt_str(prompt_cstr);
    env->ReleaseStringUTFChars(prompt, prompt_cstr);

    // Get callback methods
    jclass callbackClass = env->GetObjectClass(callback);
    jmethodID onTokenMethod = env->GetMethodID(callbackClass, "onToken", "(Ljava/lang/String;)V");
    jmethodID onCompleteMethod = env->GetMethodID(callbackClass, "onComplete", "()V");
    jmethodID onErrorMethod = env->GetMethodID(callbackClass, "onError", "(Ljava/lang/String;)V");

    if (!onTokenMethod || !onCompleteMethod || !onErrorMethod) {
        LOGE("Failed to find callback methods");
        return -2;
    }

    g_is_generating = true;

    // Tokenize the prompt
    const llama_vocab *vocab = llama_model_get_vocab(g_model);
    int n_prompt_tokens = -llama_tokenize(vocab, prompt_str.c_str(), prompt_str.length(),
                                           nullptr, 0, true, true);

    std::vector<llama_token> tokens(n_prompt_tokens);
    if (llama_tokenize(vocab, prompt_str.c_str(), prompt_str.length(),
                       tokens.data(), tokens.size(), true, true) < 0) {
        LOGE("Failed to tokenize prompt");
        g_is_generating = false;
        jstring error = env->NewStringUTF("Failed to tokenize prompt");
        env->CallVoidMethod(callback, onErrorMethod, error);
        return -3;
    }

    LOGI("Prompt tokenized: %d tokens", n_prompt_tokens);

    // Prevent context overflow
    if (n_prompt_tokens >= llama_n_ctx(g_ctx) - 16) {
        LOGE("Prompt size (%d) exceeds context window (%d)", n_prompt_tokens, llama_n_ctx(g_ctx));
        g_is_generating = false;
        jstring error = env->NewStringUTF("Prompt too long for current context window.");
        env->CallVoidMethod(callback, onErrorMethod, error);
        return -4;
    }

    // Clear KV cache — wipes all previous conversation tokens from the context window
    llama_kv_self_clear(g_ctx);

    // Reset sampler state (RNG seed etc.) for a clean generation each call
    llama_sampler_reset(g_sampler);

    // Log first 300 chars of prompt for debugging (visible in Logcat with tag LlmBridge)
    LOGI("Prompt (first 300 chars): %.300s", prompt_str.c_str());
    LOGI("Prompt total tokens: %d, max new tokens: %d", n_prompt_tokens, maxTokens);

    // Process prompt in batches
    llama_batch batch = llama_batch_init(512, 0, 1);

    for (int i = 0; i < (int)tokens.size(); i++) {
        common_batch_add(batch, tokens[i], i, {0}, false);

        if (batch.n_tokens >= 512 || i == (int)tokens.size() - 1) {
            if (i == (int)tokens.size() - 1) {
                batch.logits[batch.n_tokens - 1] = true;
            }

            if (llama_decode(g_ctx, batch) != 0) {
                LOGE("Failed to decode prompt batch");
                g_is_generating = false;
                llama_batch_free(batch);
                jstring error = env->NewStringUTF("Failed to process prompt");
                env->CallVoidMethod(callback, onErrorMethod, error);
                return -4;
            }
            common_batch_clear(batch);
        }
    }

    // Generate tokens
    int n_generated = 0;
    int n_pos = tokens.size();

    while (n_generated < maxTokens && g_is_generating) {
        // Sample next token
        llama_token new_token = llama_sampler_sample(g_sampler, g_ctx, -1);

        // Check for end of generation
        if (llama_vocab_is_eog(vocab, new_token)) {
            LOGI("End of generation reached");
            break;
        }

        // Convert token to text
        char buf[256];
        int n = llama_token_to_piece(vocab, new_token, buf, sizeof(buf), 0, false);
        if (n < 0) {
            LOGE("Failed to convert token to text");
            break;
        }

        std::string token_text(buf, n);

        // Send token to callback
        jstring jtoken = env->NewStringUTF(token_text.c_str());
        env->CallVoidMethod(callback, onTokenMethod, jtoken);
        env->DeleteLocalRef(jtoken);

        // Prepare batch for next decode
        common_batch_clear(batch);
        common_batch_add(batch, new_token, n_pos, {0}, true);
        n_pos++;

        if (llama_decode(g_ctx, batch) != 0) {
            LOGE("Failed to decode generated token");
            jstring error = env->NewStringUTF("Failed during generation");
            env->CallVoidMethod(callback, onErrorMethod, error);
            break;
        }

        n_generated++;
    }

    llama_batch_free(batch);
    g_is_generating = false;

    LOGI("Generated %d tokens", n_generated);
    env->CallVoidMethod(callback, onCompleteMethod);

    return 0;
}

JNIEXPORT void JNICALL
Java_com_neo_android_engine_LlamaCppBridge_stopInference(
    JNIEnv *env, jobject /* this */) {
    g_is_generating = false;
    LOGI("Inference stop requested");
}

JNIEXPORT jint JNICALL
Java_com_neo_android_engine_LlamaCppBridge_getVocabSize(
    JNIEnv *env, jobject /* this */) {
    if (!g_model) return 0;
    const llama_vocab *vocab = llama_model_get_vocab(g_model);
    return llama_vocab_n_tokens(vocab);
}

JNIEXPORT jint JNICALL
Java_com_neo_android_engine_LlamaCppBridge_getContextSize(
    JNIEnv *env, jobject /* this */) {
    if (!g_ctx) return 0;
    return llama_n_ctx(g_ctx);
}

} // extern "C"
