package xyz.xiao6.myboard.manager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import xyz.xiao6.myboard.model.DictionarySpec
import xyz.xiao6.myboard.util.MLog
import java.util.Locale

/**
 * RuntimeDictionaryManager：根据“当前 locale + 当前 layoutId”实时筛选可用字典列表。
 *
 * 设计：
 * - locale 是动态的（例如布局上按键切到英文输入），调用 [setLocale] 即可刷新候选字典
 * - layoutId 也是动态的（例如切到笔画/手写布局），调用 [setLayoutId] 即可刷新
 *
 * 筛选顺序：
 * 1) locale 初筛：复用 [DictionaryManager.findByLocale]
 * 2) layout 二筛：若 DictionarySpec.layoutIds 非空，则必须包含当前 layoutId
 *
 * 选中规则（active）：
 * - activeList 默认等于候选列表（支持多词库叠加）
 * - active 仅用于兼容旧逻辑，取 activeList 中的第一个
 */
class RuntimeDictionaryManager(
    private val dictionaryManager: DictionaryManager,
    initialLocale: Locale = Locale.getDefault(),
    initialLayoutId: String = "",
) {
    private val logTag = "RuntimeDicts"
    private val _locale = MutableStateFlow(initialLocale)
    val locale: StateFlow<Locale> = _locale.asStateFlow()

    private val _layoutId = MutableStateFlow(initialLayoutId)
    val layoutId: StateFlow<String> = _layoutId.asStateFlow()

    private val _preferredDictionaryId = MutableStateFlow<String?>(null)
    val preferredDictionaryId: StateFlow<String?> = _preferredDictionaryId.asStateFlow()

    private val _enabledDictionaryIds = MutableStateFlow<Set<String>?>(null)
    val enabledDictionaryIds: StateFlow<Set<String>?> = _enabledDictionaryIds.asStateFlow()

    private val _candidates = MutableStateFlow<List<DictionarySpec>>(emptyList())
    val candidates: StateFlow<List<DictionarySpec>> = _candidates.asStateFlow()

    private val _activeList = MutableStateFlow<List<DictionarySpec>>(emptyList())
    val activeList: StateFlow<List<DictionarySpec>> = _activeList.asStateFlow()

    private val _active = MutableStateFlow<DictionarySpec?>(null)
    val active: StateFlow<DictionarySpec?> = _active.asStateFlow()

    init {
        dictionaryManager.loadAll()
        refresh()
    }

    fun setLocale(locale: Locale) {
        _locale.value = locale
        refresh()
    }

    fun setLayoutId(layoutId: String) {
        _layoutId.value = layoutId
        refresh()
    }

    fun setPreferredDictionaryId(dictionaryId: String?) {
        _preferredDictionaryId.value = dictionaryId?.trim()?.takeIf { it.isNotBlank() }
        refresh()
    }

    fun setEnabledDictionaryIds(dictionaryIds: List<String>?) {
        val normalized = dictionaryIds
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.distinct()
        _enabledDictionaryIds.value = normalized?.toSet()
        refresh()
    }

    private fun refresh() {
        val currentLocale = _locale.value
        val currentLayoutId = _layoutId.value.trim()

        val localeCandidates = dictionaryManager.findByLocale(currentLocale)
        val layoutFiltered = if (currentLayoutId.isBlank()) {
            localeCandidates
        } else {
            localeCandidates.filter { spec ->
                spec.layoutIds.isEmpty() || currentLayoutId in spec.layoutIds
            }
        }

        val enabledFiltered =
            _enabledDictionaryIds.value?.let { allowed ->
                layoutFiltered.filter { it.dictionaryId in allowed }
            } ?: layoutFiltered

        val sorted = enabledFiltered.sortedWith(
            compareByDescending<DictionarySpec> { it.priority }
                .thenBy { it.dictionaryId },
        )
        _candidates.value = sorted

        val preferred = _preferredDictionaryId.value
        val activeList = if (!preferred.isNullOrBlank()) {
            val preferredSpec = sorted.firstOrNull { it.dictionaryId == preferred }
            if (preferredSpec == null) sorted else listOf(preferredSpec) + sorted.filterNot { it.dictionaryId == preferred }
        } else {
            sorted
        }
        _activeList.value = activeList
        _active.value = activeList.firstOrNull()

        MLog.d(
            logTag,
            "refresh locale=${currentLocale.toLanguageTag()} layoutId='$currentLayoutId' " +
                "candidates=${sorted.map { it.dictionaryId }} active=${activeList.map { it.dictionaryId }}",
        )
    }
}
