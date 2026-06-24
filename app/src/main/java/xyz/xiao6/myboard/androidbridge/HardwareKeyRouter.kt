package xyz.xiao6.myboard.androidbridge

import android.view.KeyEvent

/**
 * 硬件键盘路由器。
 * 阶段 01 只定义接口，阶段 06 实现真实逻辑。
 */
interface HardwareKeyRouter {
    fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean
    fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean
    fun onKeyLongPress(keyCode: Int, event: KeyEvent): Boolean
}