package xyz.xiao6.myboard.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import xyz.xiao6.myboard.contract.state.LocaleTag
import xyz.xiao6.myboard.contract.state.Schema
import xyz.xiao6.myboard.data.repository.SettingsRepository
import android.net.Uri
import xyz.xiao6.myboard.pack.InstalledLanguagePack
import xyz.xiao6.myboard.pack.LanguagePackCoordinator

/**
 * 语言设置 ViewModel。
 * 管理语言配置状态，支持添加/移除语言、编辑每语言的输入方案。
 */
class LanguageSettingsViewModel(
    private val repo: SettingsRepository,
    private val packageCoordinator: LanguagePackCoordinator
) : ViewModel() {

    data class UiState(
        /** locale → 启用的 schema 列表 */
        val localeConfigs: Map<LocaleTag, List<Schema>> = emptyMap(),
        /** 当前活跃语言 */
        val currentLocale: LocaleTag = LocaleTag("en-US"),
        /** 是否处于编辑模式 */
        val isEditing: Boolean = false,
        /** 当前正在编辑方案的语言（null 表示不在方案选择页面） */
        val editingLocale: LocaleTag? = null,
        /** 方案编辑中的临时选中列表 */
        val editingSchemas: List<Schema>? = null,
        /** 当前正在添加新语言 */
        val isAddingLanguage: Boolean = false,
        val installedPackages: List<InstalledLanguagePack> = emptyList(),
        val packageOperationInProgress: Boolean = false,
        val packageMessage: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val configsRaw = repo.getSetting(SettingsRepository.KEY_ENABLED_LOCALE_CONFIGS)
            val configs = if (configsRaw != null) {
                SettingsRepository.parseLocaleConfigs(configsRaw)
            } else {
                repo.getEnabledLocaleConfigs()
            }
            val localeStr = repo.getSetting("current_locale") ?: "en-US"
            _uiState.value = UiState(
                localeConfigs = configs,
                currentLocale = LocaleTag(localeStr)
            )
        }
        viewModelScope.launch {
            packageCoordinator.refresh()
            packageCoordinator.state.collect { packageState ->
                _uiState.value = _uiState.value.copy(
                    installedPackages = packageState.installed,
                    packageOperationInProgress = packageState.isWorking,
                    packageMessage = packageState.message
                )
            }
        }
    }

    fun toggleEditMode() {
        _uiState.value = _uiState.value.copy(isEditing = !_uiState.value.isEditing)
    }

    fun startAddLanguage() {
        _uiState.value = _uiState.value.copy(isAddingLanguage = true, isEditing = false)
    }

    fun cancelAddLanguage() {
        _uiState.value = _uiState.value.copy(isAddingLanguage = false)
    }

    fun selectNewLanguage(locale: LocaleTag, defaultSchema: Schema) {
        // 进入方案选择页面，预选默认方案
        _uiState.value = _uiState.value.copy(
            isAddingLanguage = false,
            editingLocale = locale,
            editingSchemas = listOf(defaultSchema)
        )
    }

    fun removeLocale(locale: LocaleTag) {
        val configs = _uiState.value.localeConfigs.toMutableMap()
        configs.remove(locale)
        _uiState.value = _uiState.value.copy(localeConfigs = configs)
        persist(configs)
        // 如果删除的是当前语言，切换到第一个
        if (locale == _uiState.value.currentLocale && configs.isNotEmpty()) {
            val newCurrent = configs.keys.first()
            _uiState.value = _uiState.value.copy(currentLocale = newCurrent)
            persistCurrentLocale(newCurrent)
        }
    }

    fun setCurrentLocale(locale: LocaleTag) {
        _uiState.value = _uiState.value.copy(currentLocale = locale)
        persistCurrentLocale(locale)
    }

    /** 打开某语言的方案选择页面 */
    fun startEditSchemas(locale: LocaleTag) {
        val currentSchemas = _uiState.value.localeConfigs[locale] ?: emptyList()
        _uiState.value = _uiState.value.copy(
            editingLocale = locale,
            editingSchemas = currentSchemas
        )
    }

    /** 取消方案编辑 */
    fun cancelEditSchemas() {
        _uiState.value = _uiState.value.copy(editingLocale = null, editingSchemas = null)
    }

    /** 切换方案选中状态（仅更新临时列表，不保存） */
    fun toggleSchema(schema: Schema) {
        val current = _uiState.value.editingSchemas ?: emptyList()
        val newSchemas = if (schema in current) current - schema else current + schema
        _uiState.value = _uiState.value.copy(editingSchemas = newSchemas)
    }

    /** 保存方案编辑结果并返回列表页 */
    fun confirmEditSchemas() {
        val locale = _uiState.value.editingLocale ?: return
        val schemas = _uiState.value.editingSchemas ?: return
        val isExisting = _uiState.value.localeConfigs.containsKey(locale)
        val configs = _uiState.value.localeConfigs.toMutableMap()
        configs[locale] = schemas
        _uiState.value = _uiState.value.copy(
            localeConfigs = configs,
            editingLocale = null,
            editingSchemas = null
        )
        persist(configs)
        if (!isExisting) {
            _uiState.value = _uiState.value.copy(currentLocale = locale)
            persistCurrentLocale(locale)
        }
    }

    fun importPackage(uri: Uri) {
        viewModelScope.launch { packageCoordinator.import(uri) }
    }

    fun setPackageEnabled(packageId: String, enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) packageCoordinator.enable(packageId) else packageCoordinator.disable(packageId)
        }
    }

    fun uninstallPackage(packageId: String) {
        viewModelScope.launch { packageCoordinator.uninstall(packageId) }
    }

    private fun persist(configs: Map<LocaleTag, List<Schema>>) {
        viewModelScope.launch {
            repo.setEnabledLocaleConfigs(configs)
        }
    }

    private fun persistCurrentLocale(locale: LocaleTag) {
        viewModelScope.launch {
            repo.updateSetting("current_locale", locale.value)
        }
    }

    class Factory(
        private val repo: SettingsRepository,
        private val packageCoordinator: LanguagePackCoordinator
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LanguageSettingsViewModel(repo, packageCoordinator) as T
        }
    }
}
