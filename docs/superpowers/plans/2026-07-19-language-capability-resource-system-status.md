# Language Capability Resource System Status

**Updated:** 2026-07-24

**Overall:** WIP. The implementation plan is not complete and this checkpoint must not be treated as release-ready.

## Completed

- Task 1: Open `Script` value type, standard Script IDs, catalog metadata, layout action parsing, and focused tests.
- Task 2: Flat `LanguagePackManifest` / `LanguageCapability` contracts, resource bindings, validation, and removal of the old nested Manifest runtime model.
- Task 3: Versioned resource identity, canonical layout IDs, resource resolution policies, and focused resolver/layout tests.
- Task 4: `BuiltInLanguagePacks`, real built-in assets, resource catalog hashing, built-in registration, provider snapshot foundation, and registration tests.
- Task 5: Capability-first IME initialization, no empty-registry hard-coded context fallback, and provider backup test fixtures.
- Task 6 core path: Pipeline resolves a capability, selects `capability.engine.engineId`, builds an engine context/session, and serializes session replacement.
- Task 6 completion: Mapping/FSM resources are parsed from verified package bytes, resolved resource generations are retained in `ResolvedCapabilityKey`, rejected transitions preserve the active session, resource generation changes recreate it, and late results from replaced sessions are discarded.
- Task 7: Package lifecycle is transactional and persistent. It includes upgrade/uninstall leases, dependency-cycle validation, Unicode/path/hash checks, atomic staging/current/backup recovery, safe ZIP staging with executable and size limits, a transactional importer boundary, process-wide app-private storage, and restored resource/Manifest registration during IME startup.
- Task 8: `SettingsActivity` is the production settings composition root. Settings screens require explicit ViewModel/Repository injection, the Input route is reachable, a static regression test prevents screen-local database creation, and the IME observes the shared Room Flow for locale changes.
- Task 9: Legacy capability references are cleared. Manifest `ScriptDescriptor` metadata is the single source for candidate direction and keyboard mirroring, Arabic RTL presentation is covered by tests, default/Chinese/Japanese string keys are kept in sync by a static regression test, and the obsolete View-based IME layout was removed.
- Task 10 core: External ZIP packages use a strict JSON DTO decoder, transactional staging/install, enabled-package filtering, provider preferences, rollback checkpoints, and app-private persistence. `SettingsActivity` composes the process store/coordinator/document source, the language settings page imports through the Storage Access Framework without storage permission, and users can enable, disable, select a provider, or uninstall external packages. The IME observes the same Room preferences, rebuilds external layouts/resources/capabilities, applies provider preferences through an atomic registry delegate, and recreates the input session safely.

## Pending Device Verification

- Task 10 connected Android regression and final manual input-flow verification remain pending because `adb` is not available in the current environment.

## Verification

- Passed focused Script, Manifest, resource resolver, layout canonical ID, built-in registration, context manager, and base InputPipeline tests during implementation.
- The latest full `testDebugUnitTest`, `lintDebug`, and `assembleDebug` run passed after the Task 10 implementation.
- APK: `app/build/outputs/apk/debug/app-debug.apk`.
- `connectedDebugAndroidTest` was not run: the `adb` executable is unavailable in the current environment. No device-test success is claimed.

## Resume Point

1. Make Android SDK platform tools / `adb` available and run `connectedDebugAndroidTest` on an authorized device or emulator.
2. Complete the final manual import, provider switch, RTL layout, upgrade, disable, and uninstall input-flow checklist.
