# External Language Pack Import Design

**Date:** 2026-07-24

## Goal

Allow users to import, enable, disable, select, upgrade, and uninstall declarative language packs from Android storage without granting broad storage permissions. Imported packages must reuse the existing transactional `PackageStore`, capability registry, resource validation, and settings single-source architecture.

## Package Format

An external package is a ZIP-compatible document selected through Android's Storage Access Framework. The archive root must contain `manifest.json`; every other entry is a package-relative resource declared by the manifest.

The package may contain dictionaries, mappings, finite-state-machine data, and keyboard layouts. Kotlin, Java, DEX, native libraries, scripts, APKs, and other executable content remain forbidden. Existing entry-count, per-entry size, total-size, Unicode normalization, duplicate-path, traversal, hash, dependency, and version checks remain authoritative.

`manifest.json` is strict JSON decoded with `kotlinx.serialization`. Unknown JSON fields are rejected so a misspelled security- or capability-relevant property cannot be silently ignored. Unknown but syntactically valid ISO 15924-style Script identifiers are accepted only when the manifest supplies a valid `ScriptDescriptor`. Manifest and resource validation errors are returned as user-displayable messages and never partially publish a package.

## Architecture

### Manifest decoding

Add a production `JsonLanguagePackManifestDecoder` implementing `LanguagePackManifestDecoder`. It converts UTF-8 JSON bytes into `LanguagePackManifest` and delegates semantic validation to the existing manifest/package registration boundaries. Decoding errors are normalized into stable import failures without exposing stack traces.

### Import coordination

Add an application-scoped language-pack coordinator around:

- `PackageStoreProvider.get(context)` as the only installed-package store;
- `TransactionalLanguagePackImporter` for staging and installation;
- the production JSON decoder;
- package listing, activation, deactivation, upgrade, and uninstall operations.

The coordinator exposes immutable UI models and operation results. It owns no duplicate preference state. Package enablement and provider preferences are persisted through the shared `SettingsRepository`; installed bytes and active versions remain owned by `PackageStore`.

### Settings composition root

`SettingsActivity` constructs the coordinator alongside the single shared `SettingsRepository` and injects both through `LanguageSettingsViewModel.Factory`. No Compose screen creates a store, importer, decoder, database, or repository.

### Android document picker

`LanguageSettingsScreen` uses `rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument())` with ZIP-compatible MIME types. The selected `Uri` is opened through `ContentResolver.openInputStream`. This requires no storage permission on supported Android versions and does not request persistent access because import copies validated content into app-private storage immediately.

The screen passes the selected stream to the ViewModel/coordinator and never interprets archive content itself.

## Settings State

Extend the existing language settings UI state with:

- installed external package summaries;
- enabled/disabled state;
- selected provider per locale/script/schema capability when multiple providers exist;
- one in-progress operation marker;
- one dismissible localized success or error message.

Repository keys store only user choices: enabled package IDs and provider preferences keyed by orthogonal capability identity. They do not duplicate manifests, resource paths, package versions, or registry snapshots. Repository parsing treats malformed stored JSON as an empty preference set and rewrites canonical JSON on the next change.

Disabling or uninstalling a package that supplies the active capability must select an explicitly available fallback before persisting the change. If no fallback exists, the operation is rejected and the current session remains valid. The IME observes the same Room flows and rebuilds capability/session state; resource leases preserve an active old session until replacement completes.

## User Interface

The language settings list continues to show enabled locales and schemas. A package-management section adds:

- an Import language pack action;
- installed external package rows with name, version, enabled state, and validation status;
- enable/disable and uninstall actions;
- provider selection only where more than one valid provider resolves the same orthogonal state;
- localized progress, success, and failure feedback.

Destructive uninstall requires an explicit confirmation dialog. Import and enable operations do not navigate away from the page. UI text is added consistently to default, Simplified Chinese, and Japanese resources, with the existing string-key parity test guarding completeness.

## Error Handling

Failures are grouped into localized categories while retaining a concise technical detail when safe:

- unreadable or unsupported document;
- malformed JSON manifest;
- invalid manifest or undeclared resource;
- archive security/size violation;
- dependency or version conflict;
- no safe fallback for disable/uninstall;
- unexpected I/O failure.

Failed imports leave the previous installed version and active registry snapshot unchanged. Upgrade and uninstall continue to use transactional rollback and resource leases from Task 7.

## Testing

JVM tests cover:

- strict external JSON decoding;
- a valid package containing THAI, ARAB, DEVA, or another valid custom Script descriptor;
- registration, provider resolution, enable/disable, upgrade, and uninstall fallback behavior;
- invalid JSON, missing descriptor, executable entries, hash mismatch, dependency/version conflicts, and unknown fields;
- repository serialization for enabled package IDs and provider preferences;
- no partial store or settings mutation after failure.

Android tests cover document-picker-to-settings-to-IME integration when a device is available. Device tests are reported as not run when no device is connected; JVM lifecycle tests remain the required regression coverage for upgrade, uninstall, and lease behavior.

## Completion Criteria

- Users can import a valid external ZIP through the system picker without storage permission.
- Imported manifests and resources are transactionally installed and exposed to language settings.
- Enablement and provider choices use the shared `SettingsRepository` and are observed by the IME.
- Invalid packages cannot mutate active package, registry, settings, or input-session state.
- Default, Chinese, and Japanese resources remain key-compatible.
- `testDebugUnitTest`, `lintDebug`, and `assembleDebug` pass; connected tests run only when a device is available.
