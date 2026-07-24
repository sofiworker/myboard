# External Language Pack Import Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add secure external language-pack ZIP import, enablement, provider selection, upgrade/uninstall coordination, and settings-to-IME synchronization without storage permission.

**Architecture:** Decode strict JSON into dedicated wire DTOs, stage archives through the existing secure `PackageArchiveStager`, and install them through `PackageStore`. A coordinator composed in `SettingsActivity` combines installed-package state with Room-backed enablement/provider preferences to produce an effective registry; all cross-store operations use preflight, serialized commit, and compensating restore.

**Tech Stack:** Kotlin, kotlinx.serialization, Coroutines/Flow, Room, Android Storage Access Framework, Jetpack Compose, JUnit, Gradle.

---

## File Structure

- `pack/ExternalManifestDtos.kt`: strict serializable JSON wire model and DTO-to-domain conversion.
- `pack/JsonLanguagePackManifestDecoder.kt`: UTF-8 JSON decoder and normalized decode failures.
- `pack/PackageStore.kt`: checkpoint/restore and installed payload summaries required for coordinated rollback.
- `pack/LanguagePackCoordinator.kt`: effective snapshot filtering and serialized package/settings operations.
- `pack/LanguagePackRuntimeAssembler.kt`: rebuild resource catalogs, external layouts, resolver, and capability registry from an effective package snapshot.
- `engine/LanguageInputRuntimeHolder.kt`: atomic immutable runtime bundle shared by context, transition, pipeline, and Compose consumers.
- `pack/ActiveOrthogonalStateSource.kt`: process-wide optional runtime state bridge updated by the IME and read during settings preflight.
- `pack/LanguagePackDocumentSource.kt`: Android `Uri` to owned `InputStream` boundary; coordinator performs `use` on `Dispatchers.IO`.
- `data/repository/SettingsRepository.kt`: canonical enabled-package/provider-preference persistence and atomic multi-key writes.
- `ui/settings/LanguageSettingsViewModel.kt`: package-management UI state and coordinator commands.
- `ui/settings/LanguageSettingsScreen.kt`: document picker, installed-package controls, provider selection, confirmations, and localized feedback.
- `activity/SettingsActivity.kt`: sole production composition root for repository, store, coordinator, and ViewModel factory.
- `app/MyBoardImeService.kt`: observe package preferences and rebuild from the effective registry snapshot.

## Task 1: Strict External Manifest JSON Decoder

**Files:**
- Create: `app/src/main/java/xyz/xiao6/myboard/pack/ExternalManifestDtos.kt`
- Create: `app/src/main/java/xyz/xiao6/myboard/pack/JsonLanguagePackManifestDecoder.kt`
- Create: `app/src/test/java/xyz/xiao6/myboard/pack/ExternalLanguagePackTest.kt`

- [ ] **Step 1: Write failing canonical-decoding tests**

Create fixtures that encode `PackageIdentity.version` as `"1.2.3"`, value objects as strings, enums as uppercase names, and `VersionRange` as `{ "minimum": "1.0.0", "maximumExclusive": "2.0.0" }`. Assert a complete ARAB package decodes to the expected `LanguagePackManifest`, including its RTL descriptor and resource hashes.

- [ ] **Step 2: Write failing strict-validation tests**

Assert rejection of unknown JSON fields, malformed semantic versions, invalid ranges, missing Script descriptors, descriptor/script mismatch, and non-UTF-8 or malformed JSON. Each failure must expose a stable non-empty message without a stack trace.

- [ ] **Step 3: Run the focused test and verify RED**

Run:
```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:TEMP="$PWD\.gradle\tmp"
$env:TMP=$env:TEMP
.\gradlew.bat --no-daemon --no-problems-report '-Pkotlin.compiler.execution.strategy=in-process' testDebugUnitTest --tests '*ExternalLanguagePackTest'
```
Expected: FAIL because the production decoder and DTOs do not exist.

- [ ] **Step 4: Implement dedicated DTOs and conversion**

Use `@Serializable` DTOs with `Json { ignoreUnknownKeys = false; isLenient = false; explicitNulls = false }`. Parse semantic versions with `Regex("(?:0|[1-9]\\d*)\\.(?:0|[1-9]\\d*)\\.(?:0|[1-9]\\d*)")`; convert structured range bounds to `VersionRange`; construct domain value objects and enums explicitly; call `manifest.validate()` and reject any errors.

- [ ] **Step 5: Run focused tests and verify GREEN**

Run the Step 3 command. Expected: PASS.

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/xyz/xiao6/myboard/pack/ExternalManifestDtos.kt app/src/main/java/xyz/xiao6/myboard/pack/JsonLanguagePackManifestDecoder.kt app/src/test/java/xyz/xiao6/myboard/pack/ExternalLanguagePackTest.kt
git commit -m "feat: decode external language pack manifests"
```

## Task 2: Transaction Checkpoints and Effective Registry Filtering

**Files:**
- Modify: `app/src/main/java/xyz/xiao6/myboard/pack/PackageStore.kt`
- Create: `app/src/main/java/xyz/xiao6/myboard/pack/EffectiveRegistrySnapshot.kt`
- Modify: `app/src/test/java/xyz/xiao6/myboard/pack/PackageLifecycleTest.kt`
- Modify: `app/src/test/java/xyz/xiao6/myboard/pack/ExternalLanguagePackTest.kt`

- [ ] **Step 1: Write failing checkpoint/restore tests**

Install version 1, capture a checkpoint, install version 2, restore, and assert the active identity, manifest, resource catalog, and lease behavior return to version 1. Inject persistence failure during restore and assert the previously durable state remains recoverable.

- [ ] **Step 2: Write failing effective-snapshot tests**

Build a complete snapshot containing built-in and external THAI/ARAB providers. Assert filtering always retains built-ins, includes only enabled external IDs, removes disabled capabilities/providers, and rejects an enabled package whose required dependency is disabled.

- [ ] **Step 3: Run focused tests and verify RED**

Run:
```powershell
.\gradlew.bat --no-daemon --no-problems-report '-Pkotlin.compiler.execution.strategy=in-process' testDebugUnitTest --tests '*PackageLifecycleTest' --tests '*ExternalLanguagePackTest'
```
Expected: FAIL because checkpoint/restore and effective filtering are absent.

- [ ] **Step 4: Implement minimal package transactions**

Add immutable deep-copy `PackageStoreCheckpoint`, `checkpoint()`, `restore(checkpoint)`, and installed-package summary/payload access needed by the coordinator. Restoration must use `PackagePersistence.save` before publishing and must preserve outstanding leases on deactivating versions.

- [ ] **Step 5: Implement effective registry derivation**

Create a pure function that accepts the complete store snapshot, built-in package IDs, enabled external package IDs, and manifests. Return a filtered `RegistrySnapshot` plus validation errors. Provider/capability maps must never retain disabled package IDs.

- [ ] **Step 6: Run focused tests and verify GREEN**

Run the Step 3 command. Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add app/src/main/java/xyz/xiao6/myboard/pack/PackageStore.kt app/src/main/java/xyz/xiao6/myboard/pack/EffectiveRegistrySnapshot.kt app/src/test/java/xyz/xiao6/myboard/pack/PackageLifecycleTest.kt app/src/test/java/xyz/xiao6/myboard/pack/ExternalLanguagePackTest.kt
git commit -m "feat: filter enabled language pack providers"
```

## Task 3: Repository Preferences and Coordinated Operations

**Files:**
- Modify: `app/src/main/java/xyz/xiao6/myboard/data/dao/SettingsDao.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/data/repository/SettingsRepository.kt`
- Create: `app/src/main/java/xyz/xiao6/myboard/pack/LanguagePackCoordinator.kt`
- Create: `app/src/main/java/xyz/xiao6/myboard/pack/ActiveOrthogonalStateSource.kt`
- Create: `app/src/test/java/xyz/xiao6/myboard/data/settings/LanguagePackPreferencesTest.kt`
- Modify: `app/src/test/java/xyz/xiao6/myboard/pack/ExternalLanguagePackTest.kt`

- [ ] **Step 1: Write failing repository serialization tests**

Assert canonical sorted JSON round-trips enabled package IDs and provider preferences keyed by `locale|script|schema`. Malformed stored JSON must read as empty; an atomic update must change enabled IDs and provider preferences together.

- [ ] **Step 2: Write failing coordinator transaction tests**

Cover import-disabled, enable, disable with fallback, reject disable without fallback, upgrade preserving enablement, uninstall clearing invalid provider preferences, repository failure restoring the package checkpoint, and serialized concurrent operations. Use a fake `ActiveOrthogonalStateSource` to prove the active state is checked when the IME is alive and that operations remain valid when it reports no active session.

- [ ] **Step 3: Run focused tests and verify RED**

Run:
```powershell
.\gradlew.bat --no-daemon --no-problems-report '-Pkotlin.compiler.execution.strategy=in-process' testDebugUnitTest --tests '*LanguagePackPreferencesTest' --tests '*ExternalLanguagePackTest'
```
Expected: FAIL because preference APIs and coordinator do not exist.

- [ ] **Step 4: Add atomic Room preference writes**

Add `SettingsDao.upsertSettings(List<SettingsEntity>)` as an `@Transaction` default method. Add `KEY_ENABLED_LANGUAGE_PACKAGES`, `KEY_PROVIDER_PREFERENCES`, observation/parsing methods, and one repository method that writes both canonical values atomically. Do not store manifests, versions, paths, or registry data.

- [ ] **Step 5: Implement the coordinator**

Use a `Mutex`, `Dispatchers.IO`, `PackageStore`, `TransactionalLanguagePackImporter`, the JSON decoder, `SettingsRepository`, and an injected `ActiveOrthogonalStateSource`. Preflight every persisted enabled locale/schema configuration plus the optional live `OrthogonalState`; require the proposed effective snapshot to resolve each affected state or an explicit fallback. Capture checkpoints/preferences, apply store mutation, atomically write preferences, restore on repository failure, and expose immutable `StateFlow<LanguagePackManagementState>`. The process-wide source stores only current runtime state, is updated by `MyBoardImeService`, and is not a second settings source.

- [ ] **Step 6: Run focused tests and verify GREEN**

Run the Step 3 command. Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add app/src/main/java/xyz/xiao6/myboard/data/dao/SettingsDao.kt app/src/main/java/xyz/xiao6/myboard/data/repository/SettingsRepository.kt app/src/main/java/xyz/xiao6/myboard/pack/ActiveOrthogonalStateSource.kt app/src/main/java/xyz/xiao6/myboard/pack/LanguagePackCoordinator.kt app/src/test/java/xyz/xiao6/myboard/data/settings/LanguagePackPreferencesTest.kt app/src/test/java/xyz/xiao6/myboard/pack/ExternalLanguagePackTest.kt
git commit -m "feat: coordinate language pack preferences"
```

## Task 4: Settings Composition Root, ViewModel, and Document Picker UI

**Files:**
- Modify: `app/src/main/java/xyz/xiao6/myboard/activity/SettingsActivity.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/ui/settings/LanguageSettingsViewModel.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/ui/settings/LanguageSettingsScreen.kt`
- Create: `app/src/main/java/xyz/xiao6/myboard/pack/LanguagePackDocumentSource.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh-rCN/strings.xml`
- Modify: `app/src/main/res/values-ja/strings.xml`
- Modify: `app/src/test/java/xyz/xiao6/myboard/data/settings/SettingsRepositorySingleSourceTest.kt`
- Modify: `app/src/test/java/xyz/xiao6/myboard/i18n/StringResourcesTest.kt`

- [ ] **Step 1: Extend static composition-root tests first**

Assert `SettingsActivity` creates `PackageStoreProvider.get`, the decoder/importer/coordinator, and injects one coordinator into `LanguageSettingsViewModel.Factory`. Assert settings screens never construct these production dependencies.

- [ ] **Step 2: Add failing ViewModel state tests**

Test import progress/success/failure, enable/disable, provider selection, uninstall confirmation state, and error dismissal using a fake coordinator. Verify settings state remains separate from temporary dialog/search state.

- [ ] **Step 3: Run tests and verify RED**

Run:
```powershell
.\gradlew.bat --no-daemon --no-problems-report '-Pkotlin.compiler.execution.strategy=in-process' testDebugUnitTest --tests '*SettingsRepositorySingleSourceTest' --tests '*LanguageSettingsViewModelTest' --tests '*StringResourcesTest'
```
Expected: FAIL because the new injection and UI state do not exist.

- [ ] **Step 4: Wire the composition root and ViewModel**

Construct the process store, decoder, importer, `ContentResolverLanguagePackDocumentSource`, active-state source, and coordinator once in `SettingsActivity`; pass the coordinator and shared repository to the ViewModel factory. Keep all package operations in the ViewModel/coordinator. The ViewModel accepts a `Uri` value but no Android `Context`.

- [ ] **Step 5: Add the Storage Access Framework picker**

Use `rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument())`; launch with `arrayOf("application/zip", "application/octet-stream", "application/x-zip-compressed")`; pass the returned `Uri` to the ViewModel. `ContentResolverLanguagePackDocumentSource`, injected from `SettingsActivity`, opens the stream only inside the coordinator's `withContext(Dispatchers.IO) { source.open(uri).use { importer.import(it, decoder) } }` block. This makes the coordinator the sole stream owner and guarantees closure after the suspended import finishes. Do not request runtime storage permissions or persist URI permission.

- [ ] **Step 6: Add package-management Compose UI**

Add import action, external package rows, version/enabled state, enable/disable, provider selection only for collisions, uninstall confirmation, progress, and dismissible messages. Use `stringResource` for every user-visible string.

- [ ] **Step 7: Add all three locale resources**

Add matching default, Simplified Chinese, and Japanese keys for import, status, provider, validation, confirmation, success, and error categories. Run the key-parity test.

- [ ] **Step 8: Run focused tests and verify GREEN**

Run the Step 3 command. Expected: PASS.

- [ ] **Step 9: Commit**

```powershell
git add app/src/main/java/xyz/xiao6/myboard/activity/SettingsActivity.kt app/src/main/java/xyz/xiao6/myboard/pack/LanguagePackDocumentSource.kt app/src/main/java/xyz/xiao6/myboard/ui/settings/LanguageSettingsViewModel.kt app/src/main/java/xyz/xiao6/myboard/ui/settings/LanguageSettingsScreen.kt app/src/main/res/values app/src/main/res/values-zh-rCN app/src/main/res/values-ja app/src/test/java/xyz/xiao6/myboard/data/settings/SettingsRepositorySingleSourceTest.kt app/src/test/java/xyz/xiao6/myboard/ui/settings/LanguageSettingsViewModelTest.kt app/src/test/java/xyz/xiao6/myboard/i18n/StringResourcesTest.kt
git commit -m "feat: manage external language packs in settings"
```

## Task 5: IME Effective Snapshot Synchronization

**Files:**
- Modify: `app/src/main/java/xyz/xiao6/myboard/app/MyBoardImeService.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/state/OrthogonalRegistry.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/state/OrthogonalRegistryImpl.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/engine/EngineResourceResolver.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/engine/EngineResourceResolverImpl.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/engine/InputPipeline.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/engine/InputPipelineImpl.kt`
- Create: `app/src/main/java/xyz/xiao6/myboard/engine/LanguageInputRuntimeHolder.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/layout/LayoutRegistry.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/layout/LayoutRegistryImpl.kt`
- Create: `app/src/main/java/xyz/xiao6/myboard/pack/LanguagePackRuntimeAssembler.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/state/KeyboardContextManagerImpl.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/state/TransitionEngineImpl.kt`
- Modify: `app/src/test/java/xyz/xiao6/myboard/state/KeyboardContextManagerTest.kt`
- Modify: `app/src/test/java/xyz/xiao6/myboard/engine/InputPipelineTest.kt`
- Modify: `app/src/test/java/xyz/xiao6/myboard/pack/ExternalLanguagePackTest.kt`

- [ ] **Step 1: Write failing runtime synchronization tests**

Register an external ARAB/THAI/DEVA package containing an external layout and mapping/dictionary resource, enable it, select its provider, and assert the rebuilt catalog, layout registry, resolver, capability registry, context, and pipeline create the matching session. Add a concurrent input/runtime-replacement test proving an input event observes either the complete old bundle or complete new bundle, never a mixed resolver/registry/context. Upgrade the resource bytes and assert generation changes recreate the session. Disable or uninstall it and assert a valid fallback replaces the session while an old leased resource remains usable until close.

- [ ] **Step 2: Verify RED**

Run:
```powershell
.\gradlew.bat --no-daemon --no-problems-report '-Pkotlin.compiler.execution.strategy=in-process' testDebugUnitTest --tests '*ExternalLanguagePackTest' --tests '*KeyboardContextManagerTest' --tests '*InputPipelineTest'
```
Expected: FAIL because IME initialization still consumes the complete package snapshot.

- [ ] **Step 3: Observe shared preference flows in the IME**

Combine current locale, enabled external IDs, and provider preferences from the shared repository. Update `ActiveOrthogonalStateSource` whenever the keyboard context changes and clear it when the IME runtime is destroyed. Do not write settings from the IME and do not introduce a second in-memory preference source.

- [ ] **Step 4: Apply provider preference during capability resolution**

Create `LanguagePackRuntimeAssembler` to derive the effective snapshot, rebuild `ResolvedResourceCatalog` from enabled installed packages, parse/register their layout resources, construct a fresh `EngineResourceResolverImpl`, and register manifests into a fresh `OrthogonalRegistryImpl`. Return one immutable `LanguageInputRuntime` bundle containing the catalog/resolver/layout/capability registry and generation. Extend registry resolution with an explicit preferred package ID for a state, falling back deterministically only when the preference is absent or invalid. Preserve `ScriptDescriptor` as the sole RTL presentation source.

- [ ] **Step 5: Add a shared atomic runtime holder**

Create `LanguageInputRuntimeHolder` with an `AtomicReference<LanguageInputRuntime>` and matching read-only `StateFlow`. Refactor `KeyboardContextManagerImpl`, `TransitionEngineImpl`, `InputPipelineImpl`, `MyBoardImeService`, and Compose presentation/layout reads to resolve registries/resolvers/layouts through this holder rather than retaining constructor-time registry instances. A holder update publishes exactly one immutable bundle.

- [ ] **Step 6: Replace runtime and session under the pipeline mutex**

Add `InputPipeline.replaceRuntime(runtime, targetState)` and implement it inside the same private mutex used by input dispatch: close/reset the old session, update the holder, resolve or fallback the target state through the new bundle, update keyboard context, then create the new session before releasing the mutex. On package/preference Flow changes, assemble off the main thread and call this API. If assembly fails, retain the previous holder value/session and surface the package as invalid in settings.

- [ ] **Step 7: Run focused tests and verify GREEN**

Run the Step 2 command. Expected: PASS.

- [ ] **Step 8: Commit**

```powershell
git add app/src/main/java/xyz/xiao6/myboard/app/MyBoardImeService.kt app/src/main/java/xyz/xiao6/myboard/engine/EngineResourceResolver.kt app/src/main/java/xyz/xiao6/myboard/engine/EngineResourceResolverImpl.kt app/src/main/java/xyz/xiao6/myboard/engine/InputPipeline.kt app/src/main/java/xyz/xiao6/myboard/engine/InputPipelineImpl.kt app/src/main/java/xyz/xiao6/myboard/engine/LanguageInputRuntimeHolder.kt app/src/main/java/xyz/xiao6/myboard/layout/LayoutRegistry.kt app/src/main/java/xyz/xiao6/myboard/layout/LayoutRegistryImpl.kt app/src/main/java/xyz/xiao6/myboard/pack/LanguagePackRuntimeAssembler.kt app/src/main/java/xyz/xiao6/myboard/state/KeyboardContextManagerImpl.kt app/src/main/java/xyz/xiao6/myboard/state/OrthogonalRegistry.kt app/src/main/java/xyz/xiao6/myboard/state/OrthogonalRegistryImpl.kt app/src/main/java/xyz/xiao6/myboard/state/TransitionEngineImpl.kt app/src/test/java/xyz/xiao6/myboard/state/KeyboardContextManagerTest.kt app/src/test/java/xyz/xiao6/myboard/engine/InputPipelineTest.kt app/src/test/java/xyz/xiao6/myboard/pack/ExternalLanguagePackTest.kt
git commit -m "feat: sync enabled language packs with ime"
```

## Task 6: Android Regression Coverage and Final Verification

**Files:**
- Create or Modify: `app/src/androidTest/java/xyz/xiao6/myboard/GlobalInputFlowTest.kt`
- Modify: `docs/superpowers/plans/2026-07-19-language-capability-resource-system-status.md`

- [ ] **Step 1: Add device test without claiming unavailable execution**

Cover settings launch, document import through a test-provided URI or injected picker boundary, enabling the imported provider, and observing the selected external capability in the IME. Keep upgrade/uninstall/lease regression coverage in JVM tests.

- [ ] **Step 2: Run complete JVM, lint, and APK verification**

Run:
```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:TEMP="$PWD\.gradle\tmp"
$env:TMP=$env:TEMP
.\gradlew.bat --no-daemon --no-problems-report '-Pkotlin.compiler.execution.strategy=in-process' testDebugUnitTest lintDebug assembleDebug
```
Expected: BUILD SUCCESSFUL and `app/build/outputs/apk/debug/app-debug.apk` exists.

- [ ] **Step 3: Check for a connected device**

Run `adb devices`. If an authorized device/emulator is listed, run:
```powershell
.\gradlew.bat --no-daemon --no-problems-report connectedDebugAndroidTest
```
Expected with device: PASS. Without a device: record `not run — no authorized device`; do not claim success.

- [ ] **Step 4: Run final static scans**

Run:
```powershell
rg "LanguageManifest|LocaleCapability|ScriptCapability|SchemaCapability|enumPayload<Script>|\bKATA\b|\bHANGUL\b" app/src
git diff --check
git status --short
```
Expected: legacy scan empty, no whitespace errors, and only the intended changes from this external-language-pack implementation plan before the final commit.

- [ ] **Step 5: Update status documentation**

Mark Task 10 complete, record exact verification commands/results, APK path, and whether connected tests ran.

- [ ] **Step 6: Commit**

```powershell
git add app/src/androidTest/java/xyz/xiao6/myboard/GlobalInputFlowTest.kt docs/superpowers/plans/2026-07-19-language-capability-resource-system-status.md
git commit -m "test: verify external language pack flow"
```
