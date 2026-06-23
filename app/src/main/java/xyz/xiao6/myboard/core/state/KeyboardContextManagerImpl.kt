package xyz.xiao6.myboard.core.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import xyz.xiao6.myboard.core.contract.*

/**
 * 键盘上下文管理器。
 * 所有状态变更必须经过此管理器，确保状态一致性和可追踪性。
 */
class KeyboardContextManagerImpl(
    private val transitionEngine: TransitionEngine,
    private val registry: OrthogonalRegistry,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) : KeyboardContextManager {
    
    private val _context = MutableStateFlow(createInitialContext())
    override val context: StateFlow<KeyboardContext> = _context.asStateFlow()
    
    override fun switchLocale(locale: LocaleTag): TransitionResult {
        return applyTransition(TransitionEvent.SwitchLocale(locale))
    }
    
    override fun switchScript(script: Script): TransitionResult {
        return applyTransition(TransitionEvent.SwitchScript(script))
    }
    
    override fun switchSchema(schema: Schema): TransitionResult {
        return applyTransition(TransitionEvent.SwitchSchema(schema))
    }
    
    override fun switchLayer(layer: LayoutLayer): TransitionResult {
        return applyTransition(TransitionEvent.SwitchLayer(layer))
    }
    
    override fun openPanel(panel: PanelType): TransitionResult {
        return applyTransition(TransitionEvent.OpenPanel(panel))
    }
    
    override fun closePanel(): TransitionResult {
        return applyTransition(TransitionEvent.ClosePanel)
    }
    
    override fun setComposing(text: String, candidates: List<Candidate>): TransitionResult {
        return applyTransition(TransitionEvent.SetComposing(text, candidates))
    }
    
    override fun clearComposing(): TransitionResult {
        return applyTransition(TransitionEvent.ClearComposing)
    }
    
    override fun applyEditorProfile(profile: EditorProfile): TransitionResult {
        val current = _context.value
        
        // 处理回车键标签
        val updatedContext = when (profile.enterAction) {
            EnterAction.SEARCH, EnterAction.GO, EnterAction.SEND -> current
            else -> current
        }
        
        // 如果组合态被禁用，清除 composing
        val finalContext = if (profile.composingDisabled && current.isComposing) {
            updatedContext.copy(
                composingText = "",
                candidates = emptyList(),
                selectedCandidateIndex = -1
            )
        } else {
            updatedContext
        }
        
        if (finalContext != current) {
            _context.value = finalContext
        }
        
        return TransitionResult.Applied(finalContext)
    }
    
    /**
     * 强制设置上下文（仅用于初始化和外部恢复）。
     */
    fun forceSet(newContext: KeyboardContext) {
        _context.value = newContext
    }
    
    private fun applyTransition(event: TransitionEvent): TransitionResult {
        val current = _context.value
        val result = transitionEngine.reduce(current, event)
        
        if (result is TransitionResult.Applied) {
            _context.value = result.context
        }
        
        return result
    }
    
    private fun createInitialContext(): KeyboardContext {
        // 默认使用 en-US / LATN / LATIN_DIRECT
        val defaultState = registry.defaultState(LocaleTag("en-US"))
            ?: OrthogonalState(
                locale = LocaleTag("en-US"),
                script = Script.LATN,
                schema = BuiltInSchemas.LATIN_DIRECT
            )
        
        val schemaCap = registry.schemaCapability(defaultState)
        val layoutId = schemaCap?.layoutId ?: "qwerty"
        
        return KeyboardContext(
            orthogonal = defaultState,
            layoutId = layoutId,
            layer = LayoutLayer.NORMAL
        )
    }
}
