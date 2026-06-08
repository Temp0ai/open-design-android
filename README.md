# Open Design Mobile

Android companion app for [Open Design](https://github.com/nexu-io/open-design) — the open-source Claude Design alternative.

## Features

- **AI Design Generation** — Create prototypes, dashboards, decks, and images with natural language
- **150+ Design Systems** — Linear, Stripe, Vercel, Airbnb, Apple, Notion, Figma, Supabase and more
- **100+ Skills** — Web prototypes, mobile apps, dashboards, pitch decks, social posts, logos
- **Sandboxed Preview** — View generated HTML artifacts in a secure WebView
- **BYOK Support** — Use your own API keys with Anthropic, OpenAI, Google, or Ollama
- **Offline Design Systems** — Browse and select design systems without internet
- **Export** — Share HTML, copy to clipboard

## Architecture

```
Android App (Kotlin + Jetpack Compose)
  |
  +-- UI Layer (Jetpack Compose + MVVM)
  |     |-- Home: Design system picker, skill browser, recent artifacts
  |     |-- Create: Prompt input, style selection, real-time streaming generation
  |     |-- Gallery: Artifact browser with filters, backed by Room DB
  |     |-- Settings: API config, provider selection, persisted via DataStore
  |     +-- Preview: Sandboxed WebView rendering, share/copy HTML
  |
  +-- ViewModel Layer
  |     |-- HomeViewModel: Projects and recent artifacts
  |     |-- CreateViewModel: Generation state, streaming, skill/design system selection
  |     |-- GalleryViewModel: Filtering, CRUD operations
  |     +-- SettingsViewModel: API config persistence, provider management
  |
  +-- Data Layer
  |     |-- Room SQLite: Projects, artifacts persistence
  |     |-- DataStore: API preferences (provider, key, model, endpoint)
  |     +-- Assets: Design systems (DESIGN.md), skills (SKILL.md)
  |
  +-- API Layer
        |-- OkHttp: HTTP client with SSE streaming
        |-- Anthropic: Claude API (messages endpoint)
        |-- OpenAI: Chat Completions API
        +-- BYOK: Bring Your Own Key for all providers
```

## Setup

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 35

### Build

```bash
# Clone the repository
git clone https://github.com/Temp0ai/open-design-mobile.git
cd open-design-mobile

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```

### Install

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Configuration

1. Open the app and go to Settings
2. Enter your API key for your preferred provider:
   - **Anthropic**: Get key at https://console.anthropic.com
   - **OpenAI**: Get key at https://platform.openai.com
   - **Google**: Get key at https://aistudio.google.com
   - **Ollama**: Run Ollama locally, no key needed
3. Select your preferred model
4. Start creating!

## Project Structure

```
app/src/main/java/com/opendesign/
├── api/
│   └── OpenDesignApi.kt          # BYOK API client (Anthropic, OpenAI)
├── data/
│   ├── db/
│   │   └── AppDatabase.kt        # Room SQLite database (projects, artifacts)
│   ├── model/
│   │   └── Models.kt             # Data classes
│   ├── preferences/
│   │   └── SettingsManager.kt    # DataStore preferences for API config
│   └── repository/
│       └── DesignRepository.kt   # Data access layer (assets, DB, API)
├── ui/
│   ├── MainApp.kt                # Navigation (4 tabs)
│   ├── screens/
│   │   ├── HomeScreen.kt         # Home with design systems, skills
│   │   ├── CreateScreen.kt       # AI generation with streaming
│   │   ├── GalleryScreen.kt      # Artifact browser with filters
│   │   ├── PreviewScreen.kt      # WebView preview + share/copy
│   │   └── SettingsScreen.kt     # API configuration
│   ├── viewmodel/
│   │   ├── HomeViewModel.kt      # Projects and recent artifacts
│   │   ├── CreateViewModel.kt    # Generation state machine
│   │   ├── GalleryViewModel.kt   # Artifact filtering
│   │   └── SettingsViewModel.kt  # API config persistence
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
└── OpenDesignApp.kt              # Application class
```

## Design Systems

Bundled design systems include:
- Linear, Stripe, Vercel, Airbnb, Apple, Notion, Figma, Supabase
- And 140+ more from the Open Design repository

Each design system contains:
- `DESIGN.md` — Brand guidelines, color palette, typography, components

## Skills

Bundled skills include:
- Web Prototype, Mobile App, Dashboard, Pitch Deck
- Social Post, Logo Design
- And 90+ more from the Open Design repository

## Limitations

- **Agent CLIs not available** — This app uses BYOK (direct API calls) instead of local agent CLIs like Claude Code or Codex
- **Video export not supported** — HyperFrames video rendering requires headless Chrome + FFmpeg
- **Limited offline generation** — Requires internet for AI API calls; design systems and skills work offline

## License

Apache-2.0 — Same as Open Design

## Credits

- [Open Design](https://github.com/nexu-io/open-design) by Nexu.io
- Built with Kotlin, Jetpack Compose, Room, OkHttp, DataStore
