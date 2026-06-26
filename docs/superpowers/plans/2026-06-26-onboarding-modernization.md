# Onboarding Activity Modernization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the legacy AppIntro-based OnboardingActivity with a modern Jetpack Compose + Material3 5-page onboarding flow (feature showcase → IME enable detection → language selection → layout selection → complete), with system language auto-detection.

**Architecture:** Single `OnboardingActivity` using `ComponentActivity` + `setContent`, with `HorizontalPager` for page swiping. A new `OnboardingViewModel` manages page state, IME detection, and language selection. IME detection uses `InputMethodManager` to query enabled IMEs matching the app's package. Language/layout selection saves to `SettingsRepository`.

**Tech Stack:** Jetpack Compose, Material3, HorizontalPager (foundation), SettingsRepository, BuiltInManifests

**Deprecates:** AppIntro library (to be removed from dependencies)

---

### Task 1: Remove AppIntro dependency and add HorizontalPager

**Files:**
- Modify: `app/build.gradle.kts:86` — Remove AppIntro dependency
- Modify: `gradle/libs.versions.toml` — Remove any AppIntro version reference (none exists; just the direct dependency)
- Verify: HorizontalPager is available via `foundation.pager.horizontalPager` in Compose Foundation (already included via compose-bom)

- [ ] **Step 1: Remove AppIntro dependency from build.gradle.kts**

Open `app/build.gradle.kts`, find and remove lines 84-86:
```
    // AppIntro - 引导页面
    implementation("com.github.AppIntro:AppIntro:6.3.1")
```

Expected: The `implementation("com.github.AppIntro:AppIntro:6.3.1")` line is removed. HorizontalPager is available through the existing Compose BOM (`androidx.compose.foundation:foundation`).

- [ ] **Step 2: Sync project to verify removal + foundation pager availability**

Run:
```bash
cd C:\Users\xiao6\StudioProjects\myboard && ./gradlew app:dependencies --configuration debugRuntimeClasspath 2>&1 | grep -i "appintro" || echo "AppIntro removed OK"
```

Expected: `AppIntro removed OK` (no AppIntro dependency found)

- [ ] **Step 3: Commit**

```bash
git add app/build.gradle.kts
git commit -m "chore: remove AppIntro dependency for onboarding rewrite"
```

---

### Task 2: Create OnboardingViewModel

**Files:**
- Create: `app/src/main/java/xyz/xiao6/myboard/ui/onboarding/OnboardingViewModel.kt`

This ViewModel owns all state for the 5-page flow: current page, IME enabled status, selected languages and schemas.

- [ ] **Step 1: Write OnboardingViewModel**

Create the file with the following content:

```kotlin
package xyz.xiao6.myboard.ui.onboarding

import android.content.Context
import android.view.inputmethod.InputMethodInfo
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import xyz.xiao6.myboard.contract.state.LocaleTag
import xyz.xiao6.myboard.contract.state.Schema
import xyz.xiao6.myboard.data.repository.SettingsRepository
import xyz.xiao6.myboard.state.BuiltInManifests

/**
 * 引导页 UI 状态。
 */
data class OnboardingUiState(
    val currentPage: Int = 0,
    val totalPages: Int = 5,
    val isImeEnabled: Boolean = false,
    val isCheckingIme: Boolean = false,
    val selectedLanguages: Map<LocaleTag, List<Schema>> = emptyMap(),
    val editingLocale: LocaleTag? = null,
    val editingSchemas: List<Schema> = emptyList(),
    val isCompleting: Boolean = false
)

/**
 * 引导页 ViewModel。
 * 管理 5 页引导流程的状态：
 * 1. 功能展示
 * 2. IME 启用检测
 * 3. 语言选择
 * 4. 布局（方案）选择
 * 5. 完成
 */
class OnboardingViewModel(
    private val context: Context,
    private val repository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    /** 系统当前语言对应的 LocaleTag，用于预选中 */
    fun detectSystemLocale(): LocaleTag? {
        val systemLang = java.util.Locale.getDefault().toLanguageTag()
        // 匹配支持的语言：精确匹配，或用前缀匹配（如 zh-Hans-CN -> zh-CN）
        val supported = BuiltInManifests.all.map { it.locale }
        // 精确匹配
        supported.find { it.value == systemLang }?.let { return it }
        // 前缀匹配 (zh-Hans-CN -> zh-CN)
        supported.find { systemLang.startsWith(it.value.take(2)) }?.let { return it }
        return null
    }

    /**
     * 预填充语言：系统语言默认选中。
     */
    fun initializeWithSystemLocale() {
        val systemLocale = detectSystemLocale()
        val selected = mutableMapOf<LocaleTag, List<Schema>>()
        // 默认始终添加 en-US
        val enManifest = BuiltInManifests.all.find { it.locale.value == "en-US" }
        if (enManifest != null) {
            selected[enManifest.locale] = listOf(enManifest.defaults.schema)
        }
        if (systemLocale != null && systemLocale.value != "en-US") {
            val manifest = BuiltInManifests.all.find { it.locale == systemLocale }
            if (manifest != null && systemLocale !in selected) {
                selected[systemLocale] = listOf(manifest.defaults.schema)
            }
        }
        _uiState.value = _uiState.value.copy(selectedLanguages = selected)
    }

    /** 设置当前页 */
    fun setPage(page: Int) {
        _uiState.value = _uiState.value.copy(currentPage = page.coerceIn(0, _uiState.value.totalPages - 1))
    }

    /** 检查 MyBoard IME 是否已启用 */
    fun checkImeEnabled(): Boolean {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager ?: return false
        val enabledImes: List<InputMethodInfo> = imm.enabledInputMethodList
        val packageName = context.packageName
        return enabledImes.any { it.packageName == packageName }
    }

    /** 跳转 IME 检测 + 自动轮询 */
    fun startImeCheck() {
        _uiState.value = _uiState.value.copy(isCheckingIme = true)
        viewModelScope.launch {
            // 立即检查一次
            val enabled = checkImeEnabled()
            _uiState.value = _uiState.value.copy(isImeEnabled = enabled)
            if (enabled) {
                _uiState.value = _uiState.value.copy(isCheckingIme = false)
                setPage(3) // 跳到语言选择
                return@launch
            }
            // 未启用时，每 2 秒轮询一次
            var attempts = 0
            while (attempts < 30) {
                delay(2000)
                val nowEnabled = checkImeEnabled()
                if (nowEnabled) {
                    _uiState.value = _uiState.value.copy(isImeEnabled = true, isCheckingIme = false)
                    setPage(3) // 跳到语言选择
                    return@launch
                }
                attempts++
            }
            _uiState.value = _uiState.value.copy(isCheckingIme = false)
        }
    }

    /** 手动刷新 IME 检测（用户点击按钮） */
    fun refreshImeCheck() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCheckingIme = true)
            val enabled = checkImeEnabled()
            _uiState.value = _uiState.value.copy(isImeEnabled = enabled, isCheckingIme = false)
            if (enabled) {
                setPage(3)
            }
        }
    }

    /** 开始编辑某个语言的输入方案 */
    fun startEditSchemas(locale: LocaleTag) {
        val schemas = _uiState.value.selectedLanguages[locale] ?: emptyList()
        _uiState.value = _uiState.value.copy(
            editingLocale = locale,
            editingSchemas = schemas.toList()
        )
    }

    /** 切换某个输入方案的选中状态 */
    fun toggleSchema(schema: Schema) {
        val current = _uiState.value.editingSchemas.toMutableList()
        if (schema in current) {
            current.remove(schema)
        } else {
            current.add(schema)
        }
        _uiState.value = _uiState.value.copy(editingSchemas = current)
    }

    /** 确认语言 + 方案编辑，回到语言选择页 */
    fun confirmEditSchemas() {
        val locale = _uiState.value.editingLocale ?: return
        val updated = _uiState.value.selectedLanguages.toMutableMap()
        val validSchemas = _uiState.value.editingSchemas.ifEmpty {
            // 如果全部取消，给一个默认方案
            val manifest = BuiltInManifests.all.find { it.locale == locale }
            listOf(manifest?.defaults?.schema ?: Schema("LATIN_DIRECT"))
        }
        updated[locale] = validSchemas
        _uiState.value = _uiState.value.copy(
            selectedLanguages = updated,
            editingLocale = null,
            editingSchemas = emptyList()
        )
    }

    /** 取消方案编辑 */
    fun cancelEditSchemas() {
        _uiState.value = _uiState.value.copy(
            editingLocale = null,
            editingSchemas = emptyList()
        )
    }

    /** 勾选/取消 某语言 */
    fun toggleLanguage(locale: LocaleTag, defaultSchema: Schema) {
        val current = _uiState.value.selectedLanguages.toMutableMap()
        if (locale in current) {
            current.remove(locale)
        } else {
            current[locale] = listOf(defaultSchema)
        }
        reorderWithEnglishDefault(current)
        _uiState.value = _uiState.value.copy(selectedLanguages = current)
    }

    /** 确保 en-US 始终在最前 */
    private fun reorderWithEnglishDefault(map: MutableMap<LocaleTag, List<Schema>>) {
        val en = LocaleTag("en-US")
        if (en in map && map.keys.first() != en) {
            val entries = map.entries.toList()
            map.clear()
            map.putAll(entries.sortedByDescending { it.key == en })
        }
    }

    /** 跳到布局选择：为当前选中的语言展示方案选择 */
    fun goToLayoutSelection() {
        val firstLocale = _uiState.value.selectedLanguages.keys.firstOrNull() ?: return
        startEditSchemas(firstLocale)
    }

    /** 从方案选择页完成，跳到该语言的方案选择，或到下一语言，或到完成 */
    fun nextSchemaOrFinish() {
        val selected = _uiState.value.selectedLanguages.keys.toList()
        val currentEditing = _uiState.value.editingLocale
        val currentIndex = selected.indexOf(currentEditing)
        val nextIndex = currentIndex + 1

        // 先保存当前编辑
        confirmEditSchemas()

        if (nextIndex < selected.size) {
            // 还有下一个语言，编辑它的方案
            startEditSchemas(selected[nextIndex])
        } else {
            // 所有语言方案确认完毕，跳到完成页
            setPage(4)
        }
    }

    /** 完成引导，保存配置到数据库 */
    fun completeOnboarding(onDone: () -> Unit) {
        _uiState.value = _uiState.value.copy(isCompleting = true)
        viewModelScope.launch {
            try {
                // 保存语言配置
                val configs = _uiState.value.selectedLanguages
                if (configs.isNotEmpty()) {
                    repository.setEnabledLocaleConfigs(configs)
                    val firstLocale = configs.keys.first()
                    repository.updateSetting("current_locale", firstLocale.value)
                }
                // 标记引导完成
                repository.updateSetting("onboarding_completed", "true")
                onDone()
            } finally {
                _uiState.value = _uiState.value.copy(isCompleting = false)
            }
        }
    }

    class Factory(
        private val context: Context,
        private val repository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OnboardingViewModel(context.applicationContext, repository) as T
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/xyz/xiao6/myboard/ui/onboarding/OnboardingViewModel.kt
git commit -m "feat: add OnboardingViewModel for 5-page onboarding flow"
```

---

### Task 3: Create Page 1 - Feature Showcase page

**Files:**
- Create: `app/src/main/java/xyz/xiao6/myboard/ui/onboarding/FeatureShowcasePage.kt`

This is the first page showing AI联想, 语音输入, 多语言&布局 as three feature cards.

- [ ] **Step 1: Write FeatureShowcasePage**

```kotlin
package xyz.xiao6.myboard.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 第 1 页：功能展示。
 * 展示 MyBoard 的三个核心亮点：AI 联想、语音输入、多语言 & 布局。
 */
@Composable
fun FeatureShowcasePage(
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // 标题
        Text(
            text = "欢迎使用 MyBoard",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "智能输入，随心所欲",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(40.dp))

        // 功能卡片列表
        FeatureCard(
            icon = Icons.Outlined.Psychology,
            title = "AI 智能联想",
            description = "基于 AI 的精准词句预测，输入更高效",
            color = MaterialTheme.colorScheme.primary,
            delayMs = 0
        )

        Spacer(modifier = Modifier.height(16.dp))

        FeatureCard(
            icon = Icons.Outlined.Mic,
            title = "语音输入",
            description = "离线语音识别，说话即输入",
            color = MaterialTheme.colorScheme.tertiary,
            delayMs = 100
        )

        Spacer(modifier = Modifier.height(16.dp))

        FeatureCard(
            icon = Icons.Outlined.Language,
            title = "多语言 & 布局",
            description = "支持中文拼音/双拼、英文、日文假名等",
            color = MaterialTheme.colorScheme.secondary,
            delayMs = 200
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("下一步", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun FeatureCard(
    icon: ImageVector,
    title: String,
    description: String,
    color: androidx.compose.ui.graphics.Color,
    delayMs: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon container
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/xyz/xiao6/myboard/ui/onboarding/FeatureShowcasePage.kt
git commit -m "feat: add feature showcase page for onboarding"
```

---

### Task 4: Create Page 2 - IME Enable Detection page

**Files:**
- Create: `app/src/main/java/xyz/xiao6/myboard/ui/onboarding/ImeEnablePage.kt`

This page shows guide text, a button to open system IME settings, and continuously checks IME status. Once enabled, auto-advances.

- [ ] **Step 1: Write ImeEnablePage**

```kotlin
package xyz.xiao6.myboard.ui.onboarding

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 第 2 页：启用 MyBoard 输入法。
 * 引导用户到系统设置启用 IME，并自动检测启用状态。
 */
@Composable
fun ImeEnablePage(
    isImeEnabled: Boolean,
    isChecking: Boolean,
    onRefreshCheck: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Icon
        Surface(
            modifier = Modifier.size(80.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Keyboard,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = if (isImeEnabled) "输入法已启用 ✓"
                   else "启用 MyBoard 输入法",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isImeEnabled) "已检测到 MyBoard 输入法，即将进入下一步..."
                   else "请在系统设置中启用 MyBoard 输入法，然后返回此页面。",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (!isImeEnabled) {
            // 打开设置按钮
            Button(
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("打开输入法设置", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 刷新检测按钮
            OutlinedButton(
                onClick = onRefreshCheck,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isChecking
            ) {
                if (isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    if (isChecking) "检测中..." else "已启用，检查状态",
                    fontSize = 16.sp
                )
            }
        } else {
            // 已启用，显示成功状态
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "提示：启用后无需重新打开此页面，系统会自动检测到",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        // 跳过按钮（已启用时隐藏）
        if (!isImeEnabled) {
            TextButton(
                onClick = onRefreshCheck
            ) {
                Text("我已启用，下一步")
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/xyz/xiao6/myboard/ui/onboarding/ImeEnablePage.kt
git commit -m "feat: add IME enable detection page for onboarding"
```

---

### Task 5: Create Page 3 - Language Selection page

**Files:**
- Create: `app/src/main/java/xyz/xiao6/myboard/ui/onboarding/LanguageSelectionPage.kt`

Shows all available languages from BuiltInManifests, with system language pre-selected. Users can toggle languages on/off.

- [ ] **Step 1: Write LanguageSelectionPage**

```kotlin
package xyz.xiao6.myboard.ui.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.xiao6.myboard.contract.state.LocaleTag
import xyz.xiao6.myboard.contract.state.Schema
import xyz.xiao6.myboard.state.BuiltInManifests

/**
 * 第 3 页：语言选择。
 * 列出所有可用语言，系统语言默认预选中。
 */
@Composable
fun LanguageSelectionPage(
    selectedLanguages: Map<LocaleTag, List<Schema>>,
    onToggleLanguage: (LocaleTag, Schema) -> Unit,
    onNext: () -> Unit
) {
    val manifests = remember { BuiltInManifests.all }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "选择输入语言",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "选择您要使用的输入语言，后续可在设置中更改",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(manifests, key = { it.locale.value }) { manifest ->
                val isSelected = manifest.locale in selectedLanguages
                val displayName = manifest.displayName["zh-CN"]
                    ?: manifest.displayName["en-US"]
                    ?: manifest.locale.value

                LanguageItem(
                    displayName = displayName,
                    isSelected = isSelected,
                    onClick = {
                        onToggleLanguage(manifest.locale, manifest.defaults.schema)
                    }
                )
            }
        }

        if (selectedLanguages.isEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "请至少选择一种语言",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("下一步", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun LanguageItem(
    displayName: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 2.dp else 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 语言首字母标识
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = displayName.take(1),
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )

            if (isSelected) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/xyz/xiao6/myboard/ui/onboarding/LanguageSelectionPage.kt
git commit -m "feat: add language selection page for onboarding"
```

---

### Task 6: Create Page 4 - Layout/Schema Selection page

**Files:**
- Create: `app/src/main/java/xyz/xiao6/myboard/ui/onboarding/SchemaSelectionPage.kt`

Shows available input schemas for the selected language. Users pick one or more layouts.

- [ ] **Step 1: Write SchemaSelectionPage**

```kotlin
package xyz.xiao6.myboard.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.xiao6.myboard.contract.state.LocaleTag
import xyz.xiao6.myboard.contract.state.Schema
import xyz.xiao6.myboard.contract.state.BuiltInSchemas
import xyz.xiao6.myboard.state.BuiltInManifests

/**
 * 第 4 页：输入方案（布局）选择。
 * 根据选中的语言，显示可用的输入方案（拼音、双拼、QWERTY 等）。
 * 如果当前编辑的语言还未确认，显示当前语言的方案编辑；
 * 确认后如果有多个语言，跳转到下一语言的方案编辑；
 * 全部确认后到完成页（由 ViewModel 控制页面跳转）。
 */
@Composable
fun SchemaSelectionPage(
    locale: LocaleTag?,
    selectedSchemas: List<Schema>,
    onToggleSchema: (Schema) -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    val manifest = remember(locale) {
        locale?.let { BuiltInManifests.all.find { m -> m.locale == it } }
    }
    val displayName = manifest?.displayName?.get("zh-CN")
        ?: manifest?.displayName?.get("en-US")
        ?: locale?.value ?: ""

    val availableSchemas = remember(manifest) {
        manifest?.scripts?.values?.flatMap { it.schemas.keys }?.distinct() ?: emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "选择输入方案",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "$displayName 的输入方案",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "选择一个或多个输入方式",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(availableSchemas, key = { it.value }) { schema ->
                val isSelected = schema in selectedSchemas
                val name = schemaDisplayName(schema)
                val description = schemaDescription(schema)

                SchemaItem(
                    name = name,
                    description = description,
                    isSelected = isSelected,
                    onClick = { onToggleSchema(schema) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("确认", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("使用默认方案")
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SchemaItem(
    name: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 2.dp else 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() }
            )
        }
    }
}

private fun schemaDisplayName(schema: Schema): String = when (schema) {
    BuiltInSchemas.PINYIN -> "拼音"
    BuiltInSchemas.SHUANGPIN_ZIRAN -> "双拼（自然码）"
    BuiltInSchemas.T9_PINYIN -> "T9 拼音"
    BuiltInSchemas.DOUBLE_PINYIN -> "双拼"
    BuiltInSchemas.LATIN_DIRECT -> "QWERTY"
    BuiltInSchemas.ROMAJI -> "假名（Romaji）"
    else -> schema.value
}

private fun schemaDescription(schema: Schema): String = when (schema) {
    BuiltInSchemas.PINYIN -> "标准拼音输入，支持全拼"
    BuiltInSchemas.SHUANGPIN_ZIRAN -> "自然码双拼方案，按键更少"
    BuiltInSchemas.T9_PINYIN -> "九宫格拼音输入"
    BuiltInSchemas.DOUBLE_PINYIN -> "双拼方案"
    BuiltInSchemas.LATIN_DIRECT -> "直接输入英文"
    BuiltInSchemas.ROMAJI -> "罗马音输入日文假名"
    else -> ""
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/xyz/xiao6/myboard/ui/onboarding/SchemaSelectionPage.kt
git commit -m "feat: add schema selection page for onboarding"
```

---

### Task 7: Create Page 5 - Completion page

**Files:**
- Create: `app/src/main/java/xyz/xiao6/myboard/ui/onboarding/CompletionPage.kt`

Final page with success indicator and "开始使用" button.

- [ ] **Step 1: Write CompletionPage**

```kotlin
package xyz.xiao6.myboard.ui.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 第 5 页：完成页。
 * 显示成功状态和"开始使用"按钮。
 */
@Composable
fun CompletionPage(
    isCompleting: Boolean,
    onComplete: () -> Unit
) {
    // 缩放动画
    val infiniteTransition = rememberInfiniteTransition(label = "scale")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale_anim"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(64.dp))

        // 成功图标（带缩放动画）
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier
                .size(96.dp)
                .scale(scale),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "一切就绪！",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "您已准备好使用 MyBoard",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "点击任意输入框即可切换输入法开始输入",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onComplete,
            enabled = !isCompleting,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isCompleting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("开始使用", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/xyz/xiao6/myboard/ui/onboarding/CompletionPage.kt
git commit -m "feat: add completion page for onboarding"
```

---

### Task 8: Create Page Indicator composable

**Files:**
- Create: `app/src/main/java/xyz/xiao6/myboard/ui/onboarding/PageIndicator.kt`

A simple dots indicator for HorizontalPager.

- [ ] **Step 1: Write PageIndicator**

```kotlin
package xyz.xiao6.myboard.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * HorizontalPager 圆形页面指示器。
 */
@Composable
fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            val color by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
                animationSpec = tween(300),
                label = "dot_color"
            )
            Box(
                modifier = Modifier
                    .size(if (isSelected) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/xyz/xiao6/myboard/ui/onboarding/PageIndicator.kt
git commit -m "feat: add page indicator composable for onboarding"
```

---

### Task 9: Rewrite OnboardingActivity with Compose

**Files:**
- Overwrite: `app/src/main/java/xyz/xiao6/myboard/activity/OnboardingActivity.kt`

Replace the entire OnboardingActivity with a Compose-based implementation using HorizontalPager and the new pages.

- [ ] **Step 1: Rewrite OnboardingActivity**

```kotlin
package xyz.xiao6.myboard.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import xyz.xiao6.myboard.data.db.SettingsDatabase
import xyz.xiao6.myboard.data.repository.SettingsRepository
import xyz.xiao6.myboard.ui.onboarding.*

/**
 * 现代化 Compose 引导页。
 * 5 页流程：功能展示 → IME 检测 → 语言选择 → 布局选择 → 完成
 */
class OnboardingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                OnboardingContent()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingContent() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repo = remember {
        SettingsRepository(SettingsDatabase.getInstance(context).settingsDao())
    }
    val viewModel: OnboardingViewModel = viewModel(
        factory = OnboardingViewModel.Factory(context, repo)
    )
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { uiState.totalPages }
    )
    val coroutineScope = rememberCoroutineScope()

    // 初始化系统语言预选
    LaunchedEffect(Unit) {
        viewModel.initializeWithSystemLocale()
    }

    // 同步 pager 状态与 ViewModel 状态
    LaunchedEffect(pagerState.currentPage) {
        viewModel.setPage(pagerState.currentPage)
    }

    // IME 检测自动启动
    LaunchedEffect(Unit) {
        if (pagerState.currentPage == 1) {
            viewModel.startImeCheck()
        }
    }

    Scaffold(
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PageIndicator(
                        pageCount = uiState.totalPages,
                        currentPage = uiState.currentPage
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // ---- 方案编辑模式（覆盖在页面之上） ----
            if (uiState.editingLocale != null) {
                SchemaSelectionPage(
                    locale = uiState.editingLocale,
                    selectedSchemas = uiState.editingSchemas,
                    onToggleSchema = { viewModel.toggleSchema(it) },
                    onNext = { viewModel.nextSchemaOrFinish() },
                    onSkip = { viewModel.confirmEditSchemas(); viewModel.setPage(4) }
                )
            } else {
                // ---- 标准引导页 HorizontalPager ----
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = false, // 禁止手动滑动，由按钮控制
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> FeatureShowcasePage(
                            onNext = {
                                coroutineScope.launch {
                                    viewModel.startImeCheck()
                                    pagerState.animateScrollToPage(1)
                                }
                            }
                        )
                        1 -> ImeEnablePage(
                            isImeEnabled = uiState.isImeEnabled,
                            isChecking = uiState.isCheckingIme,
                            onRefreshCheck = { viewModel.refreshImeCheck() }
                        )
                        2 -> LanguageSelectionPage(
                            selectedLanguages = uiState.selectedLanguages,
                            onToggleLanguage = { locale, schema ->
                                viewModel.toggleLanguage(locale, schema)
                            },
                            onNext = {
                                if (uiState.selectedLanguages.isNotEmpty()) {
                                    viewModel.goToLayoutSelection()
                                }
                            }
                        )
                        4 -> CompletionPage(
                            isCompleting = uiState.isCompleting,
                            onComplete = {
                                viewModel.completeOnboarding {
                                    context.startActivity(
                                        Intent(context, SettingsActivity::class.java)
                                    )
                                    (context as? ComponentActivity)?.finish()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // 监听 IME 启用状态 -> 当检测到启用后自动跳页
    LaunchedEffect(uiState.isImeEnabled) {
        if (uiState.isImeEnabled && pagerState.currentPage == 1) {
            pagerState.animateScrollToPage(2)
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/xyz/xiao6/myboard/activity/OnboardingActivity.kt
git commit -m "feat: rewrite OnboardingActivity with Compose 5-page flow"
```

---

### Task 10: Update MainActivity for new flow

**Files:**
- Modify: `app/src/main/java/xyz/xiao6/myboard/activity/MainActivity.kt`

- [ ] **Step 1: Verify MainActivity is compatible**

The current MainActivity already uses ComponentActivity + Compose and checks `onboarding_completed` — no changes needed. The new OnboardingActivity saves `onboarding_completed = "true"` via the ViewModel + Repository, same key, so the existing jump logic at `app/src/main/java/xyz/xiao6/myboard/activity/MainActivity.kt:40` works exactly as-is.

No file modifications needed for this task.

- [ ] **Step 2: No-op commit (verification)**

Run build to verify:
```bash
cd C:\Users\xiao6\StudioProjects\myboard && ./gradlew app:compileDebugKotlin 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

---

### Task 11: Update AndroidManifest theme if needed

**Files:**
- Verify: `app/src/main/AndroidManifest.xml:27` — OnboardingActivity theme

The manifest uses `@style/Theme.AppCompat.NoActionBar`. Since we switched from AppCompat Activity to ComponentActivity, check if the theme reference is still valid. If it references an AppCompat theme and the app hasn't removed AppCompat dependency, it's fine.

- [ ] **Step 1: Verify theme works with ComponentActivity**

The app still depends on `androidx.appcompat:appcompat` (in build.gradle.kts line 72), so `Theme.AppCompat.NoActionBar` is available. ComponentActivity (from `androidx.activity:activity-compose`) works fine with AppCompat themes — no change needed.

No file modifications needed.

[Verification] Run:
```bash
cd C:\Users\xiao6\StudioProjects\myboard && ./gradlew app:compileDebugKotlin 2>&1 | tail -20
```
Expected: `BUILD SUCCESSFUL`

---

### Task 12: Build and verify

**Files:**
- Build check

- [ ] **Step 1: Full build**

```bash
cd C:\Users\xiao6\StudioProjects\myboard && ./gradlew app:assembleDebug 2>&1 | tail -30
```

Expected: `BUILD SUCCESSFUL in Xs`

Verify APK was produced:
```bash
ls -la app/build/outputs/apk/debug/*.apk
```

Expected: `app-debug.apk` exists

- [ ] **Step 2: Commit all remaining files**

```bash
git add app/src/main/java/xyz/xiao6/myboard/ui/onboarding/
git add -u
git status
```

Expected: All new and modified files staged.

```bash
git commit -m "feat: modernize onboarding flow with Compose + Material3"
```

---

### Spec Coverage Check

| Spec Requirement | Task(s) |
|---|---|
| 5-page flow (功能展示 → IME 检测 → 语言选择 → 布局选择 → 完成) | Task 3-9 |
| AppIntro removed, Compose-only | Task 1, Task 9 |
| System language auto-detect + pre-select | Task 2 (initializeWithSystemLocale), Task 5 |
| IME enable detection + auto polling | Task 2 (startImeCheck), Task 4, Task 9 |
| Layout selection per language | Task 6, Task 2 (toggleSchema) |
| 干净简约白底 Material3 style | Task 3-9 (all use MaterialTheme colors) |
| Completion → SettingsActivity | Task 9 (completeOnboarding → onDone) |
| onboarding_completed saved to DB | Task 2 (completeOnboarding) |
