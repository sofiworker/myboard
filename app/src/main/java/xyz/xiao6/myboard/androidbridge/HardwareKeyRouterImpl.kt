package xyz.xiao6.myboard.androidbridge

import android.view.KeyEvent

/**
 * 硬件按键路由器真实实现。
 */
class HardwareKeyRouterImpl : HardwareKeyRouter {
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_CAMERA,
            KeyEvent.KEYCODE_BACK -> false // 不消费，传递给系统
            else -> false // 大多数按键不消费
        }
    }
    
    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return false
    }
    
    override fun onKeyLongPress(keyCode: Int, event: KeyEvent): Boolean {
        return false
    }
}
