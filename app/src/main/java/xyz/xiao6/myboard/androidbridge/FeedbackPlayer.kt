package xyz.xiao6.myboard.androidbridge

import xyz.xiao6.myboard.contract.theme.HapticToken
import xyz.xiao6.myboard.contract.theme.SoundToken

/**
 * 反馈播放器。
 * 阶段 01 只定义接口，阶段 06 实现真实逻辑。
 */
interface FeedbackPlayer {
    fun playHaptic(token: HapticToken)
    fun playSound(token: SoundToken)
}