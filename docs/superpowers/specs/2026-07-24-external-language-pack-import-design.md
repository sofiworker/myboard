# External Language Pack Import Design

**Date:** 2026-07-24

## Goal

Allow users to import, enable, disable, select, upgrade, and uninstall declarative language packs from Android storage without granting broad storage permissions. Imported packages must reuse the existing transactional `PackageStore`, capability registry, resource validation, and settings single-source architecture.

## Package Format

An external package is a ZIP-compatible document selected through Android's Storage Access Framework. The archive root must contain `manifest.json`; every other entry is a package-relative resource declared by the manifest.

The package may contain dictionaries, mappings, finite-state-machine data, and keyboard layouts. Kotlin, Java, DEX, native libraries, scripts, APKs, and other executable content remain forbidden. Existing entry-count, per-entry size, total-size, Unicode normalization, duplicate-path, traversal, hash, dependency, and version checks remain authoritative.

`manifest.json` is strict JSON decoded with `kotlinx.serialization`. Unknown JSON fields are rejected so a misspelled security- or capability-relevant property cannot be silently ignored. Unknown but syntactically valid ISO 15924-style Script identifiers are accepted only when the manifest supplies a valid `ScriptDescriptor`. Manifest and resource validation errors are returned as user-displayable messages and never partially publish a package.

The wire format uses dedicated serializable DTOs rather than adding serialization behavior to domain objects. JSON names match the domain property names. Value objects (`SemVer`, `LocaleTag`, `Script`, and `Schema`) are JSON strings. Enum values use their existing uppercase Kotlin names. `LocalizedText` and descriptor display names are locale-tag-to-string objects. `PackageIdentity` is `{ "packageId": string, "version": string }`. `VersionRange` is the structured object `{ "minimum": string|null, "maximumExclusive": string|null }`; each bound defaults to `null`, minimum is inclusive, maximum is exclusive, and both null means any version. Dependencies contain `packageId`, structured `versionRange`, and optional `optional` (default `false`). `ResourceRef` contains `packageId`, `path`, uppercase `kind`, optional structured `versionRange`, optional lowercase/uppercase 64-hex `sha256`, and optional uppercase `onMissing` (default `REJECT_PACKAGE`). Capability IDs, engine bindings, dictionary bindings, locale defaults, script manifests, subtype information, and fallback IDs are nested objects with the same field names and list/null/default semantics as the domain contracts. All domain-required fields are required on the wire; only fields with explicit domain defaults may be omitted.

The decoder converts DTOs through existing parsing/constructors and then calls `LanguagePackManifest.validate()`. Semantic versions must contain exactly three non-negative decimal components (`major.minor.patch`) with no sign, suffix, or leading/trailing whitespace. Range DTOs convert directly to the existing inclusive-minimum/exclusive-maximum `VersionRange` domain object. Decoder tests include one complete canonical JSON fixture that is also used to build end-to-end ZIP packages.

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

`PackageStore.snapshot()` remains the complete snapshot of every installed package. The coordinator derives an `EffectiveRegistrySnapshot` by filtering manifests, capabilities, and resource resolution to built-in packages plus external package IDs present in the repository's enabled-package set. Provider selection and IME capability resolution always use this effective snapshot, never the complete installed snapshot. An enabled package cannot depend on a disabled required dependency; disabling a required dependency is rejected during preflight.

### Settings composition root

`SettingsActivity` constructs the coordinator alongside the single shared `SettingsRepository` and injects both through `LanguageSettingsViewModel.Factory`. No Compose screen creates a store, importer, decoder, database, or repository.

### Android document picker

`LanguageSettingsScreen` uses `rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument())` with `application/zip`, `application/octet-stream`, and `application/x-zip-compressed`. The selected `Uri` is opened through `ContentResolver.openInputStream`. This requires no storage permission on supported Android versions and does not request persistent access because import copies validated content into app-private storage immediately.

The screen passes the selected stream to the ViewModel/coordinator and never interprets archive content itself. Opening, reading, decoding, hashing, and staging archives run on `Dispatchers.IO`; UI state changes return to the ViewModel scope.

## Settings State

Extend the existing language settings UI state with:

- installed external package summaries;
- enabled/disabled state;
- selected provider per locale/script/schema capability when multiple providers exist;
- one in-progress operation marker;
- one dismissible localized success or error message.

Repository keys store only user choices: enabled package IDs and provider preferences keyed by orthogonal capability identity. They do not duplicate manifests, resource paths, package versions, or registry snapshots. Repository parsing treats malformed stored JSON as an empty preference set and rewrites canonical JSON on the next change.

Disabling or uninstalling a package that supplies the active capability must select an explicitly available fallback before persisting the change. If no fallback exists, the operation is rejected and the current session remains valid. The IME observes the same Room flows and rebuilds capability/session state; resource leases preserve an active old session until replacement completes.

### Cross-store operation protocol

The coordinator is the only component allowed to combine `PackageStore` mutations with repository preference mutations. Every combined operation is serialized with a mutex and follows this protocol:

1. Capture the previous package checkpoint and previous repository preference values.
2. Build and validate the proposed package state, enabled-package set, provider preferences, dependency graph, effective registry, and fallback for the active orthogonal state without publishing UI success.
3. Apply the `PackageStore` mutation. `PackageStore` gains an internal checkpoint/restore transaction boundary that persists restoration through the same staging/current/backup mechanism.
4. Persist all affected repository keys in one Room transaction.
5. Publish the new effective registry/session state only after both writes succeed.

If step 3 fails, preferences are untouched. If step 4 fails, the coordinator restores the package checkpoint before reporting failure. A restore failure is reported as a recovery error and startup recovery uses the last durable package backup. Provider preferences that reference a removed capability or provider are removed in the same Room transaction; a valid explicit fallback preference is written when required. Import installs a package disabled first, so a successful import never requires a cross-store preference write; enabling it is a separate combined operation. Upgrade preserves the previous enabled flag and drops only preferences that are invalid under the candidate version during preflight/commit.

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
- no partial store or settings mutation after failure, including an injected repository failure that restores the package checkpoint.

Android tests cover document-picker-to-settings-to-IME integration when a device is available. Device tests are reported as not run when no device is connected; JVM lifecycle tests remain the required regression coverage for upgrade, uninstall, and lease behavior.

## Completion Criteria

- Users can import a valid external ZIP through the system picker without storage permission.
- Imported manifests and resources are transactionally installed and exposed to language settings.
- Enablement and provider choices use the shared `SettingsRepository` and are observed by the IME.
- Invalid packages cannot mutate active package, registry, settings, or input-session state.
- Default, Chinese, and Japanese resources remain key-compatible.
- `testDebugUnitTest`, `lintDebug`, and `assembleDebug` pass; connected tests run only when a device is available.
