package xyz.xiao6.myboard.state

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
 * 状态转移引擎。
 * 只处理通用规则，不包含具体语言判断（禁止 `if (locale == "zh-CN")` 等硬编码）。
 */
class TransitionEngineImpl(
    private val registry: OrthogonalRegistry
) : TransitionEngine {
    
    override fun reduce(current: KeyboardContext, event: TransitionEvent): TransitionResult {
        return when (event) {
            is TransitionEvent.SwitchLocale -> handleSwitchLocale(current, event.locale)
            is TransitionEvent.SwitchScript -> handleSwitchScript(current, event.script)
            is TransitionEvent.SwitchSchema -> handleSwitchSchema(current, event.schema)
            is TransitionEvent.SwitchLayer -> handleSwitchLayer(current, event.layer)
            is TransitionEvent.OpenPanel -> handleOpenPanel(current, event.panelType)
            is TransitionEvent.ClosePanel -> handleClosePanel(current)
            is TransitionEvent.ClearComposing -> handleClearComposing(current)
            is TransitionEvent.SetComposing -> handleSetComposing(current, event.text, event.candidates)
        }
    }
    
    private fun handleSwitchLocale(current: KeyboardContext, locale: LocaleTag): TransitionResult {
        // 校验目标 Locale 是否已注册
        val localeCap = registry.getLocale(locale)
            ?: return TransitionResult.Rejected(TransitionRejectReason.UNSUPPORTED_LOCALE)
        
        // 获取默认状态
        val defaultState = registry.defaultState(locale)
            ?: return TransitionResult.Rejected(TransitionRejectReason.CAPABILITY_NOT_REGISTERED)
        
        // 获取默认 Schema 能力
        val schemaCap = registry.schemaCapability(defaultState)
            ?: return TransitionResult.Rejected(TransitionRejectReason.CAPABILITY_NOT_REGISTERED)
        
        // 清空 composing、candidates、关闭面板
        val newContext = KeyboardContext(
            orthogonal = defaultState,
            layoutId = schemaCap.layoutId,
            layer = LayoutLayer.NORMAL,
            composingText = "",
            candidates = emptyList(),
            selectedCandidateIndex = -1,
            activePanel = PanelType.NONE
        )
        
        return TransitionResult.Applied(newContext)
    }
    
    private fun handleSwitchScript(current: KeyboardContext, script: Script): TransitionResult {
        val locale = current.orthogonal.locale
        
        // 校验目标 Script 是否在该 Locale 下可用
        val localeCap = registry.getLocale(locale)
            ?: return TransitionResult.Rejected(TransitionRejectReason.UNSUPPORTED_LOCALE)
        
        val scriptCap = localeCap.scripts[script]
            ?: return TransitionResult.Rejected(TransitionRejectReason.UNSUPPORTED_SCRIPT)
        
        // 获取该 Script 的默认 Schema
        val defaultSchema = scriptCap.defaultSchema
        val newState = OrthogonalState(locale, script, defaultSchema)
        
        // 校验 Schema 是否已注册
        if (!registry.isSupported(newState)) {
            return TransitionResult.Rejected(TransitionRejectReason.UNSUPPORTED_SCHEMA)
        }
        
        // 获取 Schema 能力
        val schemaCap = registry.schemaCapability(newState)
            ?: return TransitionResult.Rejected(TransitionRejectReason.CAPABILITY_NOT_REGISTERED)
        
        val newContext = current.copy(
            orthogonal = newState,
            layoutId = schemaCap.layoutId,
            layer = LayoutLayer.NORMAL,
            composingText = "",
            candidates = emptyList(),
            selectedCandidateIndex = -1,
            activePanel = PanelType.NONE
        )
        
        return TransitionResult.Applied(newContext)
    }
    
    private fun handleSwitchSchema(current: KeyboardContext, schema: Schema): TransitionResult {
        val locale = current.orthogonal.locale
        val script = current.orthogonal.script
        
        // 构造目标状态
        val targetState = OrthogonalState(locale, script, schema)
        
        // 校验 Schema 是否在该 Locale + Script 下可用
        if (!registry.isSupported(targetState)) {
            return TransitionResult.Rejected(TransitionRejectReason.ILLEGAL_COMBINATION)
        }
        
        // 获取 Schema 能力
        val schemaCap = registry.schemaCapability(targetState)
            ?: return TransitionResult.Rejected(TransitionRejectReason.CAPABILITY_NOT_REGISTERED)
        
        // Schema 切换时可能需要切换布局
        val newLayoutId = schemaCap.layoutId
        
        val newContext = current.copy(
            orthogonal = targetState,
            layoutId = newLayoutId,
            layer = LayoutLayer.NORMAL,
            composingText = "",
            candidates = emptyList(),
            selectedCandidateIndex = -1
        )
        
        return TransitionResult.Applied(newContext)
    }
    
    private fun handleSwitchLayer(current: KeyboardContext, layer: LayoutLayer): TransitionResult {
        // 获取当前 Schema 能力
        val schemaCap = registry.schemaCapability(current.orthogonal)
            ?: return TransitionResult.Rejected(TransitionRejectReason.CAPABILITY_NOT_REGISTERED)
        
        // supportsShift = false 时，Shift/Caps 请求返回 Applied 但保持普通层
        if (!schemaCap.supportsShift && (layer == LayoutLayer.SHIFTED || layer == LayoutLayer.CAPS_LOCK)) {
            return TransitionResult.Applied(current)
        }
        
        // LayoutLayer 只影响布局，不改变 OrthogonalState
        val newContext = current.copy(layer = layer)
        return TransitionResult.Applied(newContext)
    }
    
    private fun handleOpenPanel(current: KeyboardContext, panelType: PanelType): TransitionResult {
        // 打开普通面板不改变 OrthogonalState
        val newContext = current.copy(activePanel = panelType)
        return TransitionResult.Applied(newContext)
    }
    
    private fun handleClosePanel(current: KeyboardContext): TransitionResult {
        val newContext = current.copy(activePanel = PanelType.NONE)
        return TransitionResult.Applied(newContext)
    }
    
    private fun handleClearComposing(current: KeyboardContext): TransitionResult {
        val newContext = current.copy(
            composingText = "",
            candidates = emptyList(),
            selectedCandidateIndex = -1
        )
        return TransitionResult.Applied(newContext)
    }
    
    private fun handleSetComposing(current: KeyboardContext, text: String, candidates: List<Candidate>): TransitionResult {
        val newContext = current.copy(
            composingText = text,
            candidates = candidates
        )
        return TransitionResult.Applied(newContext)
    }
}
