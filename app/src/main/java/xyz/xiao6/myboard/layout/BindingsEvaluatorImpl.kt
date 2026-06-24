package xyz.xiao6.myboard.layout

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
 * Bindings 评估器真实实现。
 * 评估 region 的 visible 和 enabled 条件。
 */
class BindingsEvaluatorImpl : BindingsEvaluator {
    
    override fun evaluate(bindings: Bindings?, context: KeyboardContext): Pair<Boolean, Boolean> {
        if (bindings == null) {
            return Pair(true, true)
        }
        
        val visible = evaluateCondition(bindings.visibleWhen, context)
        val enabled = evaluateCondition(bindings.enabledWhen, context)
        
        return Pair(visible, enabled)
    }
    
    private fun evaluateCondition(condition: String?, context: KeyboardContext): Boolean {
        if (condition == null) return true
        
        return when (condition) {
            "isComposing" -> context.isComposing
            "!isComposing" -> !context.isComposing
            "hasCandidates" -> context.candidates.isNotEmpty()
            "!hasCandidates" -> context.candidates.isEmpty()
            "layer=normal" -> context.layer == LayoutLayer.NORMAL
            "layer=shifted" -> context.layer == LayoutLayer.SHIFTED
            "layer=caps" -> context.layer == LayoutLayer.CAPS_LOCK
            "panel=none" -> context.activePanel == PanelType.NONE
            "panel=emoji" -> context.activePanel == PanelType.EMOJI
            "panel=symbol" -> context.activePanel == PanelType.SYMBOL
            "panel=clipboard" -> context.activePanel == PanelType.CLIPBOARD
            "panel=llm" -> context.activePanel == PanelType.LLM
            "panel=stt" -> context.activePanel == PanelType.STT
            else -> true
        }
    }
}