# Full Surface Layouts Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add bundled JSONC layouts for candidate, symbol, emoji, and numeric full keyboard pages while keeping all key behavior in `actions.gestures`.

**Architecture:** New layouts are data-only `LayoutDoc` assets. `candidate_words_page`, `symbols_full_surface`, and `emoji_full_surface` use `FULL_SURFACE` plus `CompositeLayout` regions; `number` is a numeric keyboard layout referenced by `LayoutHintResolverImpl`. Built-in registration and tests ensure the assets parse, register, and use standard enum names.

**Tech Stack:** Kotlin, kotlinx.serialization, Android assets JSONC, JUnit, Gradle.

## Global Constraints

- Do not introduce a `specialKey` concept; `KeyDef.actions.gestures` remains the only behavior source.
- JSONC enum fields must use standard uppercase enum names.
- Region `role` and `tags` remain open metadata and cannot drive behavior.
- Every layout/page/panel must use the fixed keyboard page slot controlled by settings.
- After changes, run `.\gradlew.bat test` and `.\gradlew.bat assembleDebug`.

---

### Task 1: Tests for bundled special layouts

**Files:**
- Modify: `app/src/test/java/xyz/xiao6/myboard/layout/LayoutDocParserTest.kt`

**Interfaces:**
- Consumes: `LayoutDocParser.parse`, `LayoutRegistryImpl.register`, `LayoutPresentationMode`, `CompositeLayout`, `GridLayout`.
- Produces: Failing tests that require the four new asset files and numeric hint registration.

- [ ] **Step 1: Add tests**

Add tests that assert:
- `candidate_words_page.jsonc`, `symbols_full_surface.jsonc`, and `emoji_full_surface.jsonc` parse as `FULL_SURFACE` composite layouts.
- `number.jsonc` parses, registers, and has numeric/function keys.
- `LayoutHintResolverImpl().resolve(LayoutHint.NUMBER, "qwerty") == "number"` is backed by an actual bundled asset.

- [ ] **Step 2: Run red test**

Run: `.\gradlew.bat test --tests xyz.xiao6.myboard.layout.LayoutDocParserTest`

Expected: fail because the new JSONC assets do not exist or are not registered.

### Task 2: Add JSONC layout assets

**Files:**
- Create: `app/src/main/assets/layouts/candidate_words_page.jsonc`
- Create: `app/src/main/assets/layouts/symbols_full_surface.jsonc`
- Create: `app/src/main/assets/layouts/emoji_full_surface.jsonc`
- Create: `app/src/main/assets/layouts/number.jsonc`

**Interfaces:**
- Consumes: `LayoutDoc`, `GridLayout`, `CompositeLayout`, `LayoutActionType`, `PanelType`.
- Produces: Parseable built-in layout assets.

- [ ] **Step 1: Candidate page asset**

Create a `FULL_SURFACE` composite with a left rail, weighted candidate grid, and right action rail. Candidate cells use `COMMIT_CANDIDATE` with explicit `index`; fixed keys use `PAGE_PREV`, `PAGE_NEXT`, `DELETE`, and `CLOSE_PANEL`.

- [ ] **Step 2: Symbol page asset**

Create a `FULL_SURFACE` composite with category rail, symbol grid, and action rail. Symbol cells use `PUSH_TOKEN`; category keys use `NOOP`; fixed keys use `DELETE`, `PAGE_PREV`, `PAGE_NEXT`, and `CLOSE_PANEL`.

- [ ] **Step 3: Emoji page asset**

Create a `FULL_SURFACE` composite with top category tabs, emoji grid, and bottom action bar. Emoji cells use `PUSH_TOKEN`; category keys use `NOOP`; fixed keys use `DELETE` and `CLOSE_PANEL`.

- [ ] **Step 4: Number layout asset**

Create `number.jsonc` as a normal numeric grid with digits, decimal/sign keys, delete, enter, and ABC return key. Keys use `PUSH_TOKEN`, `DELETE`, `ENTER`, and `CLOSE_PANEL`.

### Task 3: Register assets and document the contract

**Files:**
- Modify: `app/src/main/java/xyz/xiao6/myboard/app/MyBoardImeService.kt`
- Modify: `docs/layout.md`

**Interfaces:**
- Consumes: built-in asset ids.
- Produces: built-in registration for new layouts and written constraints.

- [ ] **Step 1: Register new asset ids**

Add `candidate_words_page`, `symbols_full_surface`, `emoji_full_surface`, and `number` to the built-in layout id list in `MyBoardImeService.registerBuiltIns`.

- [ ] **Step 2: Update layout docs**

Document that special pages are ordinary layouts and every interactive key must use `actions.gestures`; `id`, `styleRef`, `role`, and `tags` cannot imply actions.

### Task 4: Verify

**Files:**
- Test-only verification.

**Interfaces:**
- Consumes: all changed files.
- Produces: passing unit tests and APK.

- [ ] **Step 1: Run focused tests**

Run: `.\gradlew.bat test --tests xyz.xiao6.myboard.layout.LayoutDocParserTest`

- [ ] **Step 2: Run full verification**

Run: `.\gradlew.bat test`

- [ ] **Step 3: Build APK**

Run: `.\gradlew.bat assembleDebug`
