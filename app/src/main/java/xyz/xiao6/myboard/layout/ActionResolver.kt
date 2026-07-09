package xyz.xiao6.myboard.layout

import xyz.xiao6.myboard.contract.input.InputAction
import xyz.xiao6.myboard.contract.layout.GestureType
import xyz.xiao6.myboard.contract.layout.KeyDef
import xyz.xiao6.myboard.contract.state.KeyboardContext

/**
 * 新布局模型的动作解析入口。
 *
 * 真实解析逻辑集中在 [ActionDispatcher]，这里保留轻量封装，避免调用方直接依赖
 * dispatcher 的实现细节。
 */
class ActionResolver(
    private val dispatcher: ActionDispatcher = ActionDispatcher()
) {
    fun resolve(key: KeyDef, gesture: GestureType, context: KeyboardContext): InputAction {
        return dispatcher.dispatch(key, gesture, context)
    }
}
