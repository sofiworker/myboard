package xyz.xiao6.myboard.toolbar

import xyz.xiao6.myboard.contract.state.BuiltInSchemas
import xyz.xiao6.myboard.contract.state.Schema
import xyz.xiao6.myboard.state.KeyboardContextManager
import xyz.xiao6.myboard.state.OrthogonalRegistry

/**
 * 布局切换器。
 * 在当前语言的可用 Schema 之间循环切换。
 */
class LayoutSwitcher(
    private val contextManager: KeyboardContextManager,
    private val orthogonalRegistry: OrthogonalRegistry
) {
    /**
     * 循环切换当前语言的 Schema。
     */
    fun cycleLayout() {
        val current = contextManager.context.value
        val localeCap = orthogonalRegistry.getLocale(current.orthogonal.locale) ?: return
        val scriptCap = localeCap.scripts[current.orthogonal.script] ?: return
        val schemas = scriptCap.schemas.keys.toList()
        if (schemas.size <= 1) return

        val currentIndex = schemas.indexOf(current.orthogonal.schema)
        val nextIndex = (currentIndex + 1) % schemas.size
        contextManager.switchSchema(schemas[nextIndex])
    }

    /**
     * 获取当前 Schema 的显示名称。
     */
    fun getCurrentSchemaName(): String {
        val current = contextManager.context.value
        return when (current.orthogonal.schema) {
            BuiltInSchemas.PINYIN -> "拼音"
            BuiltInSchemas.SHUANGPIN_ZIRAN -> "双拼"
            BuiltInSchemas.T9_PINYIN -> "T9"
            BuiltInSchemas.DOUBLE_PINYIN -> "双拼"
            BuiltInSchemas.LATIN_DIRECT -> "英文"
            BuiltInSchemas.ROMAJI -> "假名"
            else -> current.orthogonal.schema.value
        }
    }

    /**
     * 获取当前语言的可用 Schema 列表。
     */
    fun getAvailableSchemas(): List<Schema> {
        val current = contextManager.context.value
        val localeCap = orthogonalRegistry.getLocale(current.orthogonal.locale) ?: return emptyList()
        val scriptCap = localeCap.scripts[current.orthogonal.script] ?: return emptyList()
        return scriptCap.schemas.keys.toList()
    }
}
