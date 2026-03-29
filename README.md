# Neo — On-Device AI Assistant for Android

> A fully private, fully offline AI assistant that runs entirely on your phone. No cloud, no subscriptions, no data leaving your device.

---

## Overview

Neo is an Android AI assistant powered by a quantized Llama 3.2 model running locally via llama.cpp. It remembers things about you between conversations using semantic memory backed by a MiniLM sentence embedding model, and it can analyze your app usage to surface personalized insights — all without an internet connection after the initial model download.

---

## Features

### On-Device LLM Inference
- Runs **Llama 3.2 1B Instruct (Q4_K_M GGUF)** via a custom llama.cpp JNI bridge
- 8,192-token context window with streaming token output
- Sampler: temperature 0.8 / top-p 0.95
- Built entirely from C++17 source (no pre-built binaries)

### Semantic Memory (RAG)
- Embeds every conversation with **MiniLM-L6-v2** (384-dim, ONNX Runtime)
- Retrieves the top 5 relevant memories by cosine similarity at each inference call
- Memories are automatically injected into the system prompt — Neo remembers your preferences, habits, and facts without you repeating yourself

### Auto Memory Extraction
- After each conversation, the LLM extracts structured facts in five categories: **Preferences · Facts · Interests · Habits · Context**
- Confidence scores decay 2% per cycle; entries below 40% are pruned automatically
- Hard cap of 200 auto-extracted entries to keep the store focused

### Manual Memory Management
- Full CRUD interface to add, edit, and delete memories
- Manually added memories are pinned at full confidence and never decay

### Multi-Session Chat History
- Persistent Room database stores all sessions and messages
- Sessions are auto-titled from the first user message
- Slide-in sidebar for switching and deleting sessions

### Voice Interaction
- **Speech-to-text** via Android's native `SpeechRecognizer` — partial results stream live into the input bar
- **Text-to-speech** via Android's native `TextToSpeech` — prefers an offline female en-US voice

### Live Mode
- Full-screen hands-free loop: **listen → infer → speak → listen**
- Animated waveform overlay with live transcript display

### App Usage Dashboard
- Reads rolling 24-hour foreground usage via `UsageStatsManager`
- AI-powered analysis in four modes: **Summarise · Insights · Suggestions · Patterns**
- Fully streamed LLM response rendered in real time

### First-Launch Model Downloader
- Downloads both models from HuggingFace on first launch with a progress UI
- Moves models into the app's private `filesDir` — no external storage required

### Spatial Neumorphic UI
- Light-mode spatial aesthetic with `#DDE1EA` background
- Semi-transparent layered surfaces and dual-shadow `neuShadow()` Compose modifier
- Blue accent gradient (`#6B94FF → #4361EE`) and DM Sans typography throughout

---

## Demo Video

<video src="https://github.com/Crisiswastaken/Neo-Rocket-Final/blob/main/project-demo.mp4" width="400" controls></video>

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin + C++17 |
| UI | Jetpack Compose + Material 3 |
| LLM Runtime | llama.cpp (tag b5350, via CMake FetchContent) |
| Embedding Runtime | ONNX Runtime for Android 1.17.0 |
| Embedding Model | all-MiniLM-L6-v2 (384-dim) |
| LLM Model | Llama 3.2 1B Instruct Q4_K_M GGUF |
| Database | Room 2.7.0 |
| Architecture | MVVM · StateFlow · Repository pattern |
| Tokenizer | Pure-Kotlin WordPiece (bundled vocab) |
| STT / TTS | Android native `SpeechRecognizer` / `TextToSpeech` |
| DI / Lifecycle | Lifecycle ViewModel Compose 2.10.0 |
| Build | AGP 9.1.0 · Kotlin 2.2.10 · KSP 2.3.6 |

---

## Requirements

| Requirement | Minimum |
|---|---|
| Android | 9.0 Pie (API 28) |
| Architecture | arm64-v8a |
| RAM | 4 GB recommended |
| Storage | ~750 MB for models (downloaded at first launch) |
| Android Studio | Ladybug or newer |
| NDK | 27.2.12479018 |
| CMake | 3.22.1 |

---

## Building from Source

### 1. Clone the repository

```bash
git clone https://github.com/yourusername/neo.git
cd neo
```

### 2. Install NDK and CMake

Open **Android Studio → SDK Manager → SDK Tools** and install:
- NDK (Side by side) version **27.2.12479018**
- CMake version **3.22.1**

### 3. Build and run

```bash
./gradlew assembleDebug
```

Or open the project in Android Studio and click **Run**.

> **First build will take several minutes** — CMake downloads and compiles llama.cpp (tag b5350) from source.

### 4. First launch

On first launch, Neo downloads two model files:

| File | Source | Size |
|---|---|---|
| `Llama-3.2-1B-Instruct-Q4_K_M.gguf` | [bartowski/Llama-3.2-1B-Instruct-GGUF](https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF) | ~700 MB |
| `all-MiniLM-L6-v2.onnx` | [sentence-transformers/all-MiniLM-L6-v2](https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2) | ~23 MB |

A progress screen tracks both downloads. An internet connection is only required for this step — all subsequent usage is fully offline.

---

## Permissions

| Permission | Why |
|---|---|
| `RECORD_AUDIO` | Voice input via STT |
| `INTERNET` | One-time model download from HuggingFace |
| `PACKAGE_USAGE_STATS` | App usage dashboard (user must grant in Settings) |

---

## Architecture

```
com.neo.android
├── engine/
│   ├── LlamaCppBridge.kt     # JNI bridge → libllm_bridge.so
│   ├── LlmEngine.kt          # Singleton wrapper, streaming Flow<String>
│   ├── LlmState.kt           # Sealed state machine
│   ├── EmbeddingEngine.kt    # ONNX MiniLM inference
│   ├── WordPieceTokenizer.kt # Pure-Kotlin tokenizer
│   └── MemoryExtractor.kt    # Post-chat LLM fact extraction
├── model/
│   └── ModelManager.kt       # DownloadManager + file management
├── data/
│   ├── AppDatabase.kt        # Room DB (chats, messages, memories)
│   ├── ChatRepository.kt
│   ├── MemoryRepository.kt   # Cosine similarity search
│   ├── entity/               # ChatEntity, MessageEntity, MemoryEntity
│   └── dao/                  # ChatDao, MessageDao, MemoryDao
├── usage/
│   └── UsageStatsHelper.kt   # 24h app usage via UsageStatsManager
└── ui/
    ├── chat/                 # ChatScreen, ChatViewModel, InputBar,
    │                         # MessageBubble, NeoHeader, SpeechManager,
    │                         # MicButton, LiveOverlay, TypingIndicator
    ├── loading/              # LoadingScreen (downloader), HelloScreen
    ├── memory/               # MemoryScreen, MemoryViewModel
    ├── dashboard/            # UsageStatsDashboardScreen + ViewModel
    └── theme/                # Color, Theme, Type (DM Sans)

src/main/cpp/
├── CMakeLists.txt            # FetchContent llama.cpp b5350
└── llm_bridge.cpp            # Native inference: load, tokenize, batch, stream
```

---

## Design System

Neo uses a custom **spatial neumorphic** design language — light backgrounds with physically layered surfaces and soft directional shadows, giving the interface a tactile, three-dimensional quality.

| Token | Value |
|---|---|
| Background | `#DDE1EA` |
| Surface | 55% white overlay |
| Surface (raised) | 75% white overlay |
| Surface (glass) | 25% white overlay |
| Accent | `#4D7BFF` |
| Accent gradient | `#6B94FF → #4361EE` |
| Shadow dark | `#99A3B1C6` |
| Shadow light | `#D9FFFFFF` |
| Typeface | DM Sans (Regular / Medium / SemiBold / Bold) |

---

## Privacy

Neo is designed to be completely private by default:

- All inference runs locally on the device CPU
- No usage data, conversations, or memories are transmitted anywhere
- Internet access is only used during the one-time model download
- The `PACKAGE_USAGE_STATS` permission is optional and only used for the in-app dashboard

---

## License

```
MIT License

Copyright (c) 2026

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## Acknowledgements

- [llama.cpp](https://github.com/ggml-org/llama.cpp) — C/C++ inference engine for GGUF models
- [ONNX Runtime](https://onnxruntime.ai/) — Cross-platform ML inference
- [sentence-transformers/all-MiniLM-L6-v2](https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2) — Sentence embedding model
- [bartowski/Llama-3.2-1B-Instruct-GGUF](https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF) — Quantized Llama 3.2 model
- [Meta Llama 3.2](https://www.llama.com/) — Underlying language model
