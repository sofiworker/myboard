package xyz.xiao6.myboard.ui.handwriting

import android.content.Context
import xyz.xiao6.myboard.util.MLog

/**
 * Provider interface for handwriting layout catalog
 * 手写布局目录提供者接口
 */
interface HandwritingCatalogProvider {
    /**
     * Load handwriting layouts
     * 加载手写布局
     */
    fun load(): HandwritingCatalog
}

/**
 * Asset-based handwriting catalog provider
 * 基于资源的手写目录提供者
 *
 * Loads handwriting configuration from assets/handwriting/handwriting.json
 * 从 assets/handwriting/handwriting.json 加载手写配置
 */
class AssetHandwritingCatalogProvider(
    private val context: Context,
    private val configPath: String = "handwriting/handwriting.json",
    private val fallback: HandwritingCatalogProvider = BuiltInHandwritingCatalogProvider,
) : HandwritingCatalogProvider {

    private val logTag = "HandwritingProvider"

    override fun load(): HandwritingCatalog {
        val file = loadFile(configPath)
        return file?.takeIf { it.layouts.isNotEmpty() }?.let { HandwritingCatalog(it.layouts) }
            ?: fallback.load()
    }

    private fun loadFile(path: String): HandwritingConfigFile? {
        val text = runCatching {
            context.assets.open(path).bufferedReader().use { it.readText() }
        }.getOrNull() ?: return null

        return runCatching {
            HandwritingJsonParser.parseConfig(text)
        }.onFailure { e ->
            MLog.e(logTag, "Failed to parse handwriting config: $path", e)
        }.getOrNull()
    }
}

/**
 * Built-in fallback handwriting catalog provider
 * 内置回退手写目录提供者
 *
 * Provides default layouts when JSON file is not available
 * 当 JSON 文件不可用时提供默认布局
 */
object BuiltInHandwritingCatalogProvider : HandwritingCatalogProvider {
    override fun load(): HandwritingCatalog {
        val defaultLayout = HandwritingLayoutSpec(
            layoutId = "default",
            name = "默认手写",
            mode = xyz.xiao6.myboard.model.HandwritingLayoutMode.HALF_SCREEN,
            position = xyz.xiao6.myboard.model.HandwritingPosition.BOTTOM,
            canvas = HandwritingCanvasSpec(
                backgroundColor = "#F2F2F7",
                strokeColor = "#000000",
                strokeWidth = 12f,
                cornerRadius = 16f,
            ),
            recognition = HandwritingRecognitionSpec(
                autoRecognize = true,
                autoRecognizeDelayMs = 500L,
                maxCandidates = 10,
            ),
        )

        return HandwritingCatalog(
            layouts = listOf(defaultLayout)
        )
    }
}

/**
 * Manager for handwriting layouts
 * 手写布局管理器
 *
 * Similar to ToolbarManager, LayoutManager, etc.
 * 类似于 ToolbarManager、LayoutManager 等
 */
class HandwritingLayoutManager(private val context: Context) {
    private val logTag = "HandwritingLayoutManager"
    private val layouts = LinkedHashMap<String, HandwritingLayoutSpec>()
    private val provider = AssetHandwritingCatalogProvider(context)

    /**
     * Load all layouts from assets
     * 从 assets 加载所有布局
     */
    fun loadAll(): HandwritingLayoutManager {
        layouts.clear()
        val catalog = provider.load()

        for (layout in catalog.layouts) {
            layouts[layout.layoutId] = layout
            MLog.d(logTag, "loaded layoutId=${layout.layoutId} name=${layout.name}")
        }

        return this
    }

    /**
     * Get all layouts
     * 获取所有布局
     */
    fun listAll(): List<HandwritingLayoutSpec> = layouts.values.toList()

    /**
     * Get specific layout by ID
     * 通过 ID 获取特定布局
     */
    fun getLayout(layoutId: String): HandwritingLayoutSpec? = layouts[layoutId]

    /**
     * Get default layout
     * 获取默认布局
     */
    fun getDefaultLayout(): HandwritingLayoutSpec? = layouts.values.firstOrNull()
}
