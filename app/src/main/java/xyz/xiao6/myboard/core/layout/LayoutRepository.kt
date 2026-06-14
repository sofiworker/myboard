package xyz.xiao6.myboard.core.layout

import android.content.Context

/**
 * 布局仓库：管理所有可用布局。
 */
class LayoutRepository(
    private val context: Context
) {
    private val layouts = mutableMapOf<String, KeyboardLayout>()
    private var loaded = false

    @Synchronized
    fun loadAll() {
        if (loaded) return
        val files = context.assets.list("layouts").orEmpty()
            .filter { it.endsWith(".json") }
            .sorted()

        for (file in files) {
            val layout = LayoutParser.parseFromAssets(context, "layouts/$file")
            if (layout != null) {
                layouts[layout.id] = layout
            }
        }
        loaded = true
    }

    fun getLayout(id: String): KeyboardLayout? {
        loadAll()
        return layouts[id]
    }

    fun listLayoutIds(): List<String> {
        loadAll()
        return layouts.keys.toList()
    }

    fun getLayoutsByLocale(locale: String): List<KeyboardLayout> {
        loadAll()
        return layouts.values.filter { it.meta?.locale == locale }
    }
}
