package xyz.xiao6.myboard.state

import kotlinx.coroutines.flow.StateFlow
import xyz.xiao6.myboard.contract.input.*
import xyz.xiao6.myboard.contract.layout.*
import xyz.xiao6.myboard.contract.manifest.*
import xyz.xiao6.myboard.contract.theme.*
import xyz.xiao6.myboard.contract.engine.*
import xyz.xiao6.myboard.contract.bridge.*
import xyz.xiao6.myboard.contract.registry.*
import xyz.xiao6.myboard.contract.panel.*
import xyz.xiao6.myboard.contract.language.*
import xyz.xiao6.myboard.contract.state.*

/**
 * 键盘上下文管理器。
 * 所有状态变更必须经过 KeyboardContextManager。
 * 阶段 02 实现真实逻辑。
 */
interface KeyboardContextManager {
    val context: StateFlow<KeyboardContext>
    
    fun switchLocale(locale: LocaleTag): TransitionResult
    fun switchScript(script: Script): TransitionResult
    fun switchSchema(schema: Schema): TransitionResult
    fun switchLayer(layer: LayoutLayer): TransitionResult
    fun openPanel(panel: PanelType): TransitionResult
    fun closePanel(): TransitionResult
    fun setComposing(text: String, candidates: List<Candidate>): TransitionResult
    fun clearComposing(): TransitionResult
    fun applyEditorProfile(profile: EditorProfile): TransitionResult
}