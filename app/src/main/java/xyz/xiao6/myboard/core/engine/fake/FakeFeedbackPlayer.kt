package xyz.xiao6.myboard.core.engine.fake

import xyz.xiao6.myboard.core.contract.HapticToken
import xyz.xiao6.myboard.core.contract.SoundToken
import xyz.xiao6.myboard.core.androidbridge.FeedbackPlayer

/**
 * Fake FeedbackPlayer - 保留供测试使用
 * 
 * 记录调用的 token，用于阶段 04/05 测试。
 */
class FakeFeedbackPlayer : FeedbackPlayer {
    data class HapticCall(val token: HapticToken)
    data class SoundCall(val token: SoundToken)
    
    val hapticCalls = mutableListOf<HapticCall>()
    val soundCalls = mutableListOf<SoundCall>()
    
    override fun playHaptic(token: HapticToken) {
        hapticCalls.add(HapticCall(token))
    }
    
    override fun playSound(token: SoundToken) {
        soundCalls.add(SoundCall(token))
    }
    
    fun clear() {
        hapticCalls.clear()
        soundCalls.clear()
    }
}
