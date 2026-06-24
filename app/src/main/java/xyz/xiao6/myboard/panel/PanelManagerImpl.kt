package xyz.xiao6.myboard.panel

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
