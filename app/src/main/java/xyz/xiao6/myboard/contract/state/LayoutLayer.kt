package xyz.xiao6.myboard.contract.state

/**
 * 布局层切换。
 * LayoutLayer 只影响布局查找和显示层，不改变 Locale、Script、Schema。
 */
enum class LayoutLayer {
    NORMAL,
    SHIFTED,
    CAPS_LOCK,
    SYMBOL,
    NUMBER
}
