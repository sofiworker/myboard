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

## Not Started

- Task 10: External Language Pack import and end-to-end Android regression coverage.

## Verification

- Passed focused Script, Manifest, resource resolver, layout canonical ID, built-in registration, context manager, and base InputPipeline tests during implementation.
- The latest full `testDebugUnitTest` and `assembleDebug` run passed after completing Task 9.
- `lintDebug` passed after removing the unused legacy `ime_view.xml`; `connectedDebugAndroidTest` has not been run because no device verification was requested or available for this checkpoint.

## Resume Point

1. Execute Task 10 external package import and Android regression coverage.
2. Run `connectedDebugAndroidTest` when a device is available.
3. Complete the final manual input-flow verification checklist.
