package xyz.xiao6.myboard.core.panel

import xyz.xiao6.myboard.core.contract.*

/**
 * 扩展面板管理器真实实现。
 * 管理工具面板状态。
 */
class PanelManagerImpl : PanelManager {
    
    private var _currentPanel: PanelType = PanelType.NONE
    override val currentPanel: PanelType get() = _currentPanel
    
    override fun openPanel(panelType: PanelType) {
        _currentPanel = panelType
    }
    
    override fun closePanel() {
        _currentPanel = PanelType.NONE
    }
}
