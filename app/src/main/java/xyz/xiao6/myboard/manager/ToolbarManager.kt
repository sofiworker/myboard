package xyz.xiao6.myboard.manager

import android.content.Context
import xyz.xiao6.myboard.model.ToolbarParser
import xyz.xiao6.myboard.model.ToolbarSpec
import xyz.xiao6.myboard.util.MLog

class ToolbarManager(
    private val context: Context,
) {
    private val logTag = "ToolbarManager"
    private val toolbars = LinkedHashMap<String, ToolbarSpec>()

    fun loadAllFromAssets(dir: String = "toolbars"): ToolbarManager {
        toolbars.clear()
        val assetManager = context.assets
        val files = runCatching { assetManager.list(dir)?.toList().orEmpty() }.getOrDefault(emptyList())
        for (file in files) {
            if (!file.endsWith(".json", ignoreCase = true)) continue
            val path = "$dir/$file"
            val spec =
                runCatching {
                    val text = assetManager.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
                    ToolbarParser.parseToolbarSpec(text)
                }.onFailure { t ->
                    MLog.e(logTag, "Failed to load toolbar: $path", t)
                }.getOrNull() ?: continue
            toolbars[spec.toolbarId] = spec
            MLog.d(logTag, "loaded toolbarId=${spec.toolbarId} name=${spec.name ?: ""} path=$path")
        }
        return this
    }

    fun listAll(): List<ToolbarSpec> = toolbars.values.toList()

    fun getToolbar(toolbarId: String): ToolbarSpec? = toolbars[toolbarId]

    fun getDefaultToolbar(): ToolbarSpec? = toolbars.values.firstOrNull()
}

