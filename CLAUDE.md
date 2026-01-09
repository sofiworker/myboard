# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

MyBoard is a customizable Android input method editor (IME) built with modern Android technologies. It's a single-module application implementing a keyboard with multi-language support, custom layouts, theme switching, and advanced features like handwriting recognition and emoji input.

**Package**: `xyz.xiao6.myboard`
**Min SDK**: 24 (Android 7.0)
**Target SDK**: 34 (Android 14)

## Build Commands

### Build and Verification
```bash
# Full debug build (includes asset generation)
./gradlew :app:assembleDebug

# Fast Kotlin compilation check (faster for development)
./gradlew :app:compileDebugKotlin

# Clean build
./gradlew clean

# Run unit tests
./gradlew :app:testDebugUnitTest
```

### Custom Build Tasks
The project has custom Gradle tasks that run automatically during `preBuild`:

1. **`convertDictionaries`**: Converts external dictionary formats (Rime YAML) to custom `.mybdict` binary format
   - Reads: `src/main/assets/dictionary/*.dict.yaml`
   - Writes: `src/main/assets/dictionary/base.mybdict`
   - Also generates draft metadata in `build/generated/dictionaryAssets/dictionary/`

2. **`generateSubtypes`**: Generates keyboard subtype configurations
   - Reads: `src/main/assets/layouts/*.json` and `src/main/assets/dictionary/meta/*.json`
   - Writes: `src/main/assets/subtypes/generated.json`

### Asset Generation Workflow
Since the project is unreleased, generated assets are written directly to `src/main/assets/` to ensure APK packaging includes them:
- Generated dictionaries go to `src/main/assets/dictionary/`
- Generated subtypes go to `src/main/assets/subtypes/`

## Architecture

### High-Level Structure

The codebase follows **clean architecture** with clear separation of concerns:

```
xyz.xiao6.myboard/
├── ime/                    # IME service (Android entry point)
├── controller/             # Input coordination and state management
├── ui/                     # Jetpack Compose UI components
│   ├── keyboard/           # Main keyboard rendering
│   ├── candidate/          # Candidate word display
│   ├── emoji/              # Emoji panel
│   ├── symbols/            # Symbol/special character panel
│   ├── clipboard/          # Clipboard history
│   ├── toolbar/            # Toolbar actions
│   ├── popup/              # Floating popups (candidates, composing, preview)
│   ├── settings/           # Settings UI
│   └── theme/              # Theme system and runtime switching
├── decoder/                # Input decoding logic (Pinyin, token-based)
├── composer/               # Text composition rules
├── suggest/                # Candidate suggestion algorithms
├── manager/                # Central managers (Layout, Dictionary, Theme, Toolbar, Subtype)
├── dictionary/             # Dictionary system with custom binary format
│   └── format/             # Format parsers and converters
├── model/                  # Data models and parsers (JSON schemas)
├── store/                  # Settings persistence (Room database)
└── util/                   # Utilities (logging, sizing, etc.)
```

### Core Components

#### 1. Input Method Service
- **`MyBoardImeService`**: Main Android IME service, handles lifecycle and input events

#### 2. Controller Layer
- **`InputMethodController`**: Central coordinator between UI and input logic
- **`KeyboardStateMachine`**: Manages keyboard state transitions and mode switches
- **`LayoutState`**: Tracks current layout, layer, and mode state

#### 3. Decoder System
- **`Decoder` interface**: Base for all decoder implementations
- **`DecoderFactory`**: Creates appropriate decoder based on dictionary/mode
- **`TokenPinyinDecoder`**: Pinyin input with token processing
- **`PassthroughDecoder`**: Direct key passthrough for non-compositional layouts
- **`PinyinKeyNormalizer`**: Normalizes Pinyin input according to canonical scheme

#### 4. Dictionary System
The project uses a **custom binary dictionary format** (`.mybdict`) for performance:

- **Container**: `MYBDF001` (MyBoard Dictionary Format v1)
- **Payload**: `MYBDICT1` (code → candidates index)
- Features: metadata + compressed payload in single file
- Supports: exact match and prefix queries for composition

**Runtime Dictionary Loading**:
- `MyBoardDictionary`: Main dictionary interface for queries
- `DictionaryManager`: Manages built-in and user dictionaries
- `RuntimeDictionaryManager`: Runtime dictionary resolution based on locale/layout/mode

**Dictionary Import**:
- `DictionaryImporter`: Entry point for user-uploaded dictionaries
- `ExternalDictionaryConverter`: Converts external formats to `.mybdict`
- `RimeDictYamlExternalParser`: Parses Rime `.dict.yaml` files

**Canonical Code Scheme**:
- All dictionary lookups use **canonical codes** (normalized form)
- External format compatibility is handled **only at conversion time**
- Current scheme: `PINYIN_FULL` (lowercase, no spaces, no apostrophes)
- Example: `ni hao` → `nihao`, `xi'an` → `xian`

See `docs/dictionary_format.md` for complete binary format specification.

#### 5. Layout System
**JSON-based keyboard layouts** with extensible action system:

- Layout definitions: `src/main/assets/layouts/*.json`
- **KeyAction system**: `actions` + `cases` + `doActions` pattern
  - `default`: Default action when no cases match
  - `fallback`: Built-in fallback (e.g., `PRIMARY_CODE_AS_TOKEN`)
  - `cases`: Conditional actions based on state (layer, mode, etc.)

See `docs/layout_actions.md` for action system details.

**Layout Managers**:
- `LayoutManager`: Loads and manages keyboard layouts
- `SubtypeManager`: Manages language subtypes (locale → layoutIds[] mapping)

#### 6. UI Layer (Jetpack Compose)
All UI is built with **Jetpack Compose** and **Material 3**:

- **`KeyboardSurfaceView`**: Main keyboard rendering surface
- **`CandidateView`**: Candidate word display with paging
- **FloatingComposingPopup**: Shows current composition text
- **FloatingCandidatePopup**: Floating candidate suggestions
- **Theme System**: Runtime theme switching via `ThemeManager`

#### 7. Suggestion System
- **`SuggestionManager`**: Coordinates suggestion providers
- **`SuggestionPipeline`**: Multi-stage suggestion processing
- **`NextWordStore`**: Next word prediction
- **`UserLexiconStore`**: User-specific word learning

#### 8. Settings System
**Single source of truth** for settings (per AGENTS.md requirement):

- **`SettingsStore`**: Main settings data store (Room database)
- **`SettingsActivity`**: Centralized settings UI with all settings
- All settings must be accessible through SettingsActivity
- No duplicate state across different components

### Key Architectural Patterns

1. **Manager Pattern**: Centralized managers for major concerns (Layout, Dictionary, Theme, etc.)
2. **Factory Pattern**: DecoderFactory creates decoders based on configuration
3. **Observer Pattern**: Kotlin Coroutines Flow for reactive state updates
4. **Asset-Based Configuration**: JSON-driven layouts, themes, toolbars for easy customization
5. **Custom Binary Format**: Optimized `.mybdict` for fast dictionary lookups

## Language and Locale Switching

When implementing language switching (from AGENTS.md):

1. Get current language layout
2. Check if target language has the same layout:
   - **Yes**: Redraw keyboard with symbol substitution (no layout change)
   - **No**: Check user's LayoutManager for target language layouts:
     - **Found**: Switch to that layout
     - **Not found**: Use target language's default layout

## Development Guidelines (from AGENTS.md)

### Key Principles
- **No backward compatibility needed** (unreleased project)
- **Prioritize extensibility** over backward compatibility
- **Use Kotlin language features extensively**
- **Maximize Jetpack and Compose usage**
- **Minimize permissions** with proper runtime handling
- **Single source of truth for settings** (SettingsActivity)
- **Separate data and view layers**
- **Ensure i18n support**

### Code Quality
- All modifications must compile successfully
- Use `./gradlew :app:compileDebugKotlin` for fast verification
- Settings must be centralized in SettingsActivity
- No duplicate state variables causing inconsistencies

### Verification (Required)
Must be verifiable with `./gradlew`:

1. Generate subtypes and compile debug:
   ```bash
   ./gradlew :app:assembleDebug
   ```
2. Fast Kotlin compilation check:
   ```bash
   ./gradlew :app:compileDebugKotlin
   ```
3. Verify generated files exist:
   - Check: `app/build/generated/subtypesAssets/subtypes/generated.json`
4. Manual testing:
   - Switch layouts and modes, verify candidates/decoder update correctly

## Technologies

- **Language**: Kotlin 2.0.0 with Coroutines
- **UI**: Jetpack Compose with Material 3
- **Database**: Room 2.6.1 for settings persistence
- **Serialization**: Kotlinx Serialization for JSON
- **Build**: Gradle with Kotlin DSL, version catalog for dependencies
- **Annotation Processing**: KSP (Kotlin Symbol Processing)
- **Handwriting**: ML Kit Digital Ink Recognition
- **Pagination**: Google Accompanist Pager

## Asset Organization

```
src/main/assets/
├── layouts/              # Keyboard layout JSON definitions
├── dictionary/
│   ├── base.dict.yaml   # Source dictionary (Rime format)
│   ├── base.mybdict     # Generated binary dictionary
│   ├── 8105.dict.yaml   # Additional dictionary source
│   └── meta/            # Dictionary metadata JSON
├── themes/               # Theme JSON definitions
├── toolbars/             # Toolbar layout JSON
├── subtypes/
│   └── generated.json   # Generated subtype mappings
└── emojis/               # Emoji category definitions
```

## Important Files

- `AGENTS.md`: Development workflow and coding guidelines (Chinese)
- `docs/dictionary_format.md`: Custom binary dictionary format specification
- `docs/layout_actions.md`: Layout action system documentation
- `SKILL.md`: Subagent-driven development workflow
- `app/build.gradle.kts`: Build configuration with custom tasks
- `scripts/dict_tool.py`: Dictionary conversion tool
- `scripts/generate_subtypes.py`: Subtype generation script

## Common Patterns

### Adding a New Keyboard Layout
1. Create JSON definition in `src/main/assets/layouts/`
2. Define layout metadata (id, name, locale, etc.)
3. Run build to regenerate subtypes
4. Test layout switching

### Adding a New Dictionary
1. Place source dictionary (Rime YAML) in `src/main/assets/dictionary/`
2. Create metadata JSON in `src/main/assets/dictionary/meta/`
3. Run `convertDictionaries` task to generate `.mybdict`
4. Update subtype mappings if needed

### Adding a New Theme
1. Create theme JSON in `src/main/assets/themes/`
2. Define color palette and component styles
3. Theme is automatically loaded by ThemeManager

### Adding Settings
1. Add field to `SettingsStore` (Room database)
2. Add UI control in `SettingsActivity`
3. Ensure single source of truth (no duplicate state)
4. Implement i18n strings

## Testing Strategy

- **Unit Tests**: `./gradlew :app:testDebugUnitTest`
- **Instrumentation Tests**: AndroidX Test framework
- **UI Testing**: Compose testing APIs
- **Manual Testing**: Use `BenchmarkInputActivity` for latency testing

## Notes

- Project uses **unreleased workflow** - generated assets go directly to `src/main/assets/`
- Dictionary format is **not compatible** with external formats at runtime - conversion required
- Canonical code scheme ensures stable, comparable byte sequences for binary search
- All permission requests must be minimized and handled at runtime
- Settings must always flow through SettingsActivity to prevent state inconsistencies
