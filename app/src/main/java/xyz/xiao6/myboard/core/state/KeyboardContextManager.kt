package xyz.xiao6.myboard.core.state

import kotlinx.coroutines.flow.StateFlow
import xyz.xiao6.myboard.core.contract.*

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