# Language Capability Resource System Status

**Updated:** 2026-07-19

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

## Not Started

- Task 8: Settings single-source production cleanup and settings Flow integration.
- Task 9: Final legacy-reference cleanup, RTL presentation completion, and scoped i18n verification.
- Task 10: External Language Pack import and end-to-end Android regression coverage.

## Verification

- Passed focused Script, Manifest, resource resolver, layout canonical ID, built-in registration, context manager, and base InputPipeline tests during implementation.
- The latest full `testDebugUnitTest` and `assembleDebug` run passed after completing Task 6.
- `lintDebug` and `connectedDebugAndroidTest` have not been completed for this WIP checkpoint.

## Resume Point

1. Execute Task 8 settings single-source cleanup and Flow integration.
2. Execute Tasks 9-10.
3. Run `testDebugUnitTest`, `lintDebug`, `assembleDebug`, and device tests when available.
