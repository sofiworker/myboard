package xyz.xiao6.myboard.core.contract

/**
 * 正交动作集合。
 * 所有布局层按键交互最终都转译为正交动作交给 InputPipeline。
 * 
 * 核心动作（来自 orthogonal-state-management.md）：
 * - PUSH_TOKEN：推入编码 token
 * - DELETE / SPACE / ENTER：控制键
 * - SWITCH_LOCALE / SWITCH_SCRIPT / SWITCH_SCHEMA：正交状态切换请求
 * - SWITCH_LAYER：布局层切换
 * - RESTORE_PREVIOUS_SCHEMA：退出特殊 Schema 时恢复
 * - COMMIT_CANDIDATE：候选选择
 * 
 * UI 动作（补充）：
 * - OPEN_PANEL / CLOSE_PANEL：工具面板
 * - PAGE_CANDIDATE：候选栏分页
 */
sealed interface InputAction {
    /** 推入编码 token（拼音 q、罗马音 ka 等），由引擎决定后续行为 */
    data class PushToken(val token: String) : InputAction
    
    /** 删除，组合态中删除 buffer，非组合态删除宿主文本 */
    data object Delete : InputAction
    
    /** 空格，由 CandidatePolicy 决定行为 */
    data object Space : InputAction
    
    /** 回车，由 CandidatePolicy 和 EditorInfo 决定行为 */
    data object Enter : InputAction
    
    /** 切换 Locale（语境/语言区域） */
    data class SwitchLocale(val locale: LocaleTag) : InputAction
    
    /** 切换 Script（目标输出文字系统） */
    data class SwitchScript(val script: Script) : InputAction
    
    /** 切换 Schema（输入方案） */
    data class SwitchSchema(val schema: Schema) : InputAction
    
    /** 切换 LayoutLayer（Shift、符号页、数字页） */
    data class SwitchLayer(val layer: LayoutLayer) : InputAction
    
    /** 退出 VOICE / HANDWRITING 时恢复上一个普通 Schema */
    data object RestorePreviousSchema : InputAction
    
    /** 选择候选，payload 带 index */
    data class CommitCandidate(val index: Int) : InputAction
    
    /** 打开工具面板（Emoji、符号、剪贴板、LLM 等） */
    data class OpenPanel(val panelType: PanelType) : InputAction
    
    /** 关闭当前面板 */
    data object ClosePanel : InputAction
    
    /** 候选栏分页，delta 为正数下一页，负数上一页 */
    data class PageCandidate(val delta: Int) : InputAction
    
    /** 无动作 */
    data object Noop : InputAction
}