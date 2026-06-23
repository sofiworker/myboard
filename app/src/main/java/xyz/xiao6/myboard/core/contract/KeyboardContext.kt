package xyz.xiao6.myboard.core.contract

/**
 * 键盘运行时唯一状态源。
 * UI 组件只观察此对象，不直接修改内部字段。
 */
data class KeyboardContext(
    val orthogonal: OrthogonalState,
    val layoutId: String,
    val layer: LayoutLayer,
    val composingText: String = "",
    val candidates: List<Candidate> = emptyList(),
    val selectedCandidateIndex: Int = -1,
    val activePanel: PanelType = PanelType.NONE
) {
    val isComposing: Boolean get() = composingText.isNotEmpty()
    val hasCandidates: Boolean get() = candidates.isNotEmpty()
}
