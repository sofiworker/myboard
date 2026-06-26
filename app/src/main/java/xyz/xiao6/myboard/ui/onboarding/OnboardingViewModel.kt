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

    /** 系统当前语言对应的 LocaleTag，用于预选中。 */
    fun detectSystemLocale(): LocaleTag? {
        val systemLang = java.util.Locale.getDefault().toLanguageTag()
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

    /** 设置当前页。 */
    fun setPage(page: Int) {
        _uiState.value = _uiState.value.copy(currentPage = page.coerceIn(0, _uiState.value.totalPages - 1))
    }

    /** 检查 MyBoard IME 是否已启用。 */
    fun checkImeEnabled(): Boolean {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager ?: return false
        val enabledImes: List<InputMethodInfo> = imm.enabledInputMethodList
        val packageName = context.packageName
        return enabledImes.any { it.packageName == packageName }
    }

    /** 跳转 IME 检测 + 自动轮询。 */
    fun startImeCheck() {
        _uiState.value = _uiState.value.copy(isCheckingIme = true)
        viewModelScope.launch {
            // 立即检查一次
            val enabled = checkImeEnabled()
            _uiState.value = _uiState.value.copy(isImeEnabled = enabled)
            if (enabled) {
                _uiState.value = _uiState.value.copy(isCheckingIme = false)
                setPage(2) // 跳到语言选择
                return@launch
            }
            // 未启用时，每 2 秒轮询一次
            var attempts = 0
            while (attempts < 30) {
                delay(2000)
                val nowEnabled = checkImeEnabled()
                if (nowEnabled) {
                    _uiState.value = _uiState.value.copy(isImeEnabled = true, isCheckingIme = false)
                    setPage(2) // 跳到语言选择
                    return@launch
                }
                attempts++
            }
            _uiState.value = _uiState.value.copy(isCheckingIme = false)
        }
    }

    /** 手动刷新 IME 检测（用户点击按钮）。 */
    fun refreshImeCheck() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCheckingIme = true)
            val enabled = checkImeEnabled()
            _uiState.value = _uiState.value.copy(isImeEnabled = enabled, isCheckingIme = false)
            if (enabled) {
                setPage(2)
            }
        }
    }

    /** 开始编辑某个语言的输入方案。 */
    fun startEditSchemas(locale: LocaleTag) {
        val schemas = _uiState.value.selectedLanguages[locale] ?: emptyList()
        _uiState.value = _uiState.value.copy(
            editingLocale = locale,
            editingSchemas = schemas.toList()
        )
    }

    /** 切换某个输入方案的选中状态。 */
    fun toggleSchema(schema: Schema) {
        val current = _uiState.value.editingSchemas.toMutableList()
        if (schema in current) {
            current.remove(schema)
        } else {
            current.add(schema)
        }
        _uiState.value = _uiState.value.copy(editingSchemas = current)
    }

    /** 确认语言 + 方案编辑，回到语言选择页。 */
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

    /** 取消方案编辑。 */
    fun cancelEditSchemas() {
        _uiState.value = _uiState.value.copy(
            editingLocale = null,
            editingSchemas = emptyList()
        )
    }

    /** 勾选/取消 某语言。 */
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

    /** 确保 en-US 始终在最前。 */
    private fun reorderWithEnglishDefault(map: MutableMap<LocaleTag, List<Schema>>) {
        val en = LocaleTag("en-US")
        if (en in map && map.keys.first() != en) {
            val entries = map.entries.toList()
            map.clear()
            entries.sortedByDescending { it.key == en }.forEach { (k, v) -> map[k] = v }
        }
    }

    /** 跳到布局选择：为当前选中的语言展示方案编辑。 */
    fun goToLayoutSelection() {
        val firstLocale = _uiState.value.selectedLanguages.keys.firstOrNull() ?: return
        startEditSchemas(firstLocale)
    }

    /** 完成当前语言的方案编辑，跳到下一语言或完成页。 */
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

    /** 完成引导，保存配置到数据库。 */
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
