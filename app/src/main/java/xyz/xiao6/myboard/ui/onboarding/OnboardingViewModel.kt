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
    val totalPages: Int = 4,
    val isImeEnabled: Boolean = false,
    val isCheckingIme: Boolean = false,
    val selectedLanguages: Map<LocaleTag, List<Schema>> = emptyMap(),
    /** 正在弹方案选择 Dialog 的语言，null 表示不显示 Dialog */
    val schemaDialogLocale: LocaleTag? = null,
    /** Dialog 内临时编辑的方案列表 */
    val schemaDialogSchemas: List<Schema> = emptyList(),
    val isCompleting: Boolean = false
)

/**
 * 引导页 ViewModel。
 * 流程：功能展示 → IME 检测 → 语言选择（含行内方案配置） → 完成
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
     * 预填充语言：系统语言默认选中 + 始终有 en-US。
     */
    fun initializeWithSystemLocale() {
        val systemLocale = detectSystemLocale()
        val selected = mutableMapOf<LocaleTag, List<Schema>>()
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
            val enabled = checkImeEnabled()
            _uiState.value = _uiState.value.copy(isImeEnabled = enabled)
            if (enabled) {
                _uiState.value = _uiState.value.copy(isCheckingIme = false)
                setPage(2)
                return@launch
            }
            var attempts = 0
            while (attempts < 30) {
                delay(2000)
                val nowEnabled = checkImeEnabled()
                if (nowEnabled) {
                    _uiState.value = _uiState.value.copy(isImeEnabled = true, isCheckingIme = false)
                    setPage(2)
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

    // ---- 语言 & 方案选择 ----

    /** 勾选/取消 某语言。 */
    fun toggleLanguage(locale: LocaleTag, defaultSchema: Schema) {
        val current = _uiState.value.selectedLanguages.toMutableMap()
        if (locale in current) {
            current.remove(locale)
        } else {
            current[locale] = listOf(defaultSchema)
        }
        _uiState.value = _uiState.value.copy(selectedLanguages = current)
    }

    /** 打开方案选择 Dialog。 */
    fun openSchemaDialog(locale: LocaleTag) {
        val schemas = _uiState.value.selectedLanguages[locale] ?: emptyList()
        _uiState.value = _uiState.value.copy(
            schemaDialogLocale = locale,
            schemaDialogSchemas = schemas.toList()
        )
    }

    /** 关闭方案选择 Dialog（不保存）。 */
    fun dismissSchemaDialog() {
        _uiState.value = _uiState.value.copy(
            schemaDialogLocale = null,
            schemaDialogSchemas = emptyList()
        )
    }

    /** 切换某个输入方案的选中状态。 */
    fun toggleDialogSchema(schema: Schema) {
        val current = _uiState.value.schemaDialogSchemas.toMutableList()
        if (schema in current) {
            current.remove(schema)
        } else {
            current.add(schema)
        }
        _uiState.value = _uiState.value.copy(schemaDialogSchemas = current)
    }

    /** 确认方案选择，保存到语言配置。 */
    fun confirmSchemaDialog() {
        val locale = _uiState.value.schemaDialogLocale ?: return
        var schemas = _uiState.value.schemaDialogSchemas
        if (schemas.isEmpty()) {
            // 至少保留一个默认方案
            val manifest = BuiltInManifests.all.find { it.locale == locale }
            schemas = listOf(manifest?.defaults?.schema ?: Schema("LATIN_DIRECT"))
        }
        val updated = _uiState.value.selectedLanguages.toMutableMap()
        updated[locale] = schemas
        _uiState.value = _uiState.value.copy(
            selectedLanguages = updated,
            schemaDialogLocale = null,
            schemaDialogSchemas = emptyList()
        )
    }

    /** 完成语言选择，跳到完成页。 */
    fun finishLanguageSelection() {
        setPage(3)
    }

    // ---- 引导完成 ----

    /** 完成引导，保存配置到数据库。 */
    fun completeOnboarding(onDone: () -> Unit) {
        _uiState.value = _uiState.value.copy(isCompleting = true)
        viewModelScope.launch {
            try {
                val configs = _uiState.value.selectedLanguages
                if (configs.isNotEmpty()) {
                    repository.setEnabledLocaleConfigs(configs)
                    val firstLocale = configs.keys.first()
                    repository.updateSetting("current_locale", firstLocale.value)
                }
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
