package xyz.xiao6.myboard.ui.handwriting

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import xyz.xiao6.myboard.model.HandwritingLayoutMode
import xyz.xiao6.myboard.model.HandwritingPosition

/**
 * Handwriting canvas style specification
 * 手写画布样式规范
 */
@Serializable
data class HandwritingCanvasSpec(
    val backgroundColor: String = "#F2F2F7",
    val strokeColor: String = "#000000",
    val strokeWidth: Float = 12f,
    val cornerRadius: Float = 16f,
)

/**
 * Handwriting recognition configuration
 * 手写识别配置
 */
@Serializable
data class HandwritingRecognitionSpec(
    val autoRecognize: Boolean = true,
    val autoRecognizeDelayMs: Long = 500L,
    val maxCandidates: Int = 10,
)

/**
 * Handwriting layout specification
 * 手写布局规范
 */
@Serializable
data class HandwritingLayoutSpec(
    val layoutId: String,
    val name: String,
    val mode: HandwritingLayoutMode,
    val position: HandwritingPosition = HandwritingPosition.BOTTOM,
    val canvas: HandwritingCanvasSpec = HandwritingCanvasSpec(),
    val recognition: HandwritingRecognitionSpec = HandwritingRecognitionSpec(),
)

/**
 * Handwriting configuration file
 * 手写输入配置文件
 */
@Serializable
data class HandwritingConfigFile(
    val version: Int = 1,
    val layouts: List<HandwritingLayoutSpec> = emptyList(),
)

/**
 * Handwriting catalog containing all layouts
 * 手写布局目录
 */
data class HandwritingCatalog(
    val layouts: List<HandwritingLayoutSpec>,
)

/**
 * JSON parser for handwriting configuration
 * 手写配置 JSON 解析器
 */
@OptIn(ExperimentalSerializationApi::class)
object HandwritingJsonParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        allowTrailingComma = true
        coerceInputValues = true
    }

    fun parseConfig(text: String): HandwritingConfigFile {
        return json.decodeFromString(HandwritingConfigFile.serializer(), text)
    }
}
