package xyz.xiao6.myboard.core.extension

import android.net.Uri
import xyz.xiao6.myboard.core.state.OrthogonalRegistry
import xyz.xiao6.myboard.core.contract.ImportResult

/**
 * 语言包导入器。
 * 阶段 01 只定义接口，阶段 09 实现真实逻辑。
 */
interface LanguagePackImporter {
    suspend fun import(zipFile: Uri, registry: OrthogonalRegistry): ImportResult
}