package xyz.xiao6.myboard.toolbar

import xyz.xiao6.myboard.contract.state.BuiltInSchemas
import xyz.xiao6.myboard.contract.state.Schema
import xyz.xiao6.myboard.contract.state.TransitionResult
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
        val schemas = localeCap.capabilities
            .filter { it.id.script == current.orthogonal.script }
            .map { it.id.schema }
        if (schemas.size <= 1) return

        val currentIndex = schemas.indexOf(current.orthogonal.schema)
        val nextIndex = (currentIndex + 1) % schemas.size
        contextManager.switchSchema(schemas[nextIndex])
    }

    /**
     * 获取指定 Schema 的显示名称。
     */
    fun getSchemaDisplayName(schema: Schema): String {
        return when (schema) {
            BuiltInSchemas.PINYIN -> "拼音"
            BuiltInSchemas.SHUANGPIN_ZIRAN -> "双拼"
            BuiltInSchemas.T9_PINYIN -> "T9"
            BuiltInSchemas.DOUBLE_PINYIN -> "双拼"
            BuiltInSchemas.LATIN_DIRECT -> "英文"
            BuiltInSchemas.ROMAJI -> "假名"
            else -> schema.value
        }
    }

    /**
     * 获取当前 Schema 的显示名称。
     */
    fun getCurrentSchemaName(): String {
        return getSchemaDisplayName(contextManager.context.value.orthogonal.schema)
    }

    /**
     * 切换到指定 Schema。
     */
    fun switchToSchema(schema: Schema): TransitionResult {
        return contextManager.switchSchema(schema)
    }

    /**
     * 获取当前语言的可用 Schema 列表。
     */
    fun getAvailableSchemas(): List<Schema> {
        val current = contextManager.context.value
        val localeCap = orthogonalRegistry.getLocale(current.orthogonal.locale) ?: return emptyList()
        return localeCap.capabilities
            .filter { it.id.script == current.orthogonal.script }
            .map { it.id.schema }
    }
}
