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
 * - 优先命中用户偏好 preferredDictionaryId（若在候选中）
 * - 否则取候选列表第一个（已按 priority 排序）
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

    private val _candidates = MutableStateFlow<List<DictionarySpec>>(emptyList())
    val candidates: StateFlow<List<DictionarySpec>> = _candidates.asStateFlow()

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

        val sorted = layoutFiltered.sortedWith(
            compareByDescending<DictionarySpec> { it.priority }
                .thenBy { it.dictionaryId },
        )
        _candidates.value = sorted

        val preferred = _preferredDictionaryId.value
        val nextActive = when {
            preferred.isNullOrBlank() -> sorted.firstOrNull()
            else -> sorted.firstOrNull { it.dictionaryId == preferred } ?: sorted.firstOrNull()
        }
        _active.value = nextActive

        MLog.d(
            logTag,
            "refresh locale=${currentLocale.toLanguageTag()} layoutId='$currentLayoutId' " +
                "candidates=${sorted.map { it.dictionaryId }} active=${nextActive?.dictionaryId}",
        )
    }
}
