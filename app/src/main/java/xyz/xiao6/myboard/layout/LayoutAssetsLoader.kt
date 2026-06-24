package xyz.xiao6.myboard.layout

import android.content.Context
import android.util.Log
import xyz.xiao6.myboard.contract.layout.LayoutDoc

/**
 * 从 assets/layouts/ 加载 JSONC 布局文件。
 * 优先从 assets 加载，失败时回退到 BuiltInLayouts 中的硬编码布局。
 */
class LayoutAssetsLoader(private val context: Context) {

    private val cache = mutableMapOf<String, LayoutDoc>()

    fun load(layoutId: String): LayoutDoc? {
        cache[layoutId]?.let { return it }

        // 尝试从 assets 加载 JSONC
        val fromAssets = loadFromAssets(layoutId)
        if (fromAssets != null) {
            cache[layoutId] = fromAssets
            return fromAssets
        }

        // 回退到硬编码布局
        val fromBuiltIn = BuiltInLayouts.byId(layoutId)
        if (fromBuiltIn != null) {
            cache[layoutId] = fromBuiltIn
        }
        return fromBuiltIn
    }

    private fun loadFromAssets(layoutId: String): LayoutDoc? {
        return try {
            val jsonc = context.assets.open("layouts/$layoutId.jsonc")
                .bufferedReader().use { it.readText() }
            val doc = LayoutDocParser.parse(jsonc)
            Log.d("LayoutAssetsLoader", "Loaded layout '$layoutId' from assets")
            doc
        } catch (e: Exception) {
            // 文件不存在或解析失败，静默返回 null
            null
        }
    }

    fun invalidateCache(layoutId: String? = null) {
        if (layoutId != null) {
            cache.remove(layoutId)
        } else {
            cache.clear()
        }
    }
}
