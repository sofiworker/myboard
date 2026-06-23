package xyz.xiao6.myboard.core.androidbridge

import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.content.Context
import xyz.xiao6.myboard.core.contract.*

/**
 * 触觉反馈播放器真实实现。
 */
class FeedbackPlayerImpl(
    private val context: Context
) : FeedbackPlayer {
    
    private val vibrator: Vibrator by lazy {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    
    var soundEnabled = false
    var hapticEnabled = true
    
    override fun playHaptic(token: HapticToken) {
        if (!hapticEnabled) return
        try {
            if (token.amplitude > 0 && token.durationMs > 0) {
                val effect = VibrationEffect.createOneShot(
                    token.durationMs,
                    token.amplitude.coerceIn(1, 255)
                )
                vibrator.vibrate(effect)
            }
        } catch (_: Exception) {
            // 振动硬件不可用
        }
    }
    
    override fun playSound(token: SoundToken) {
        // TODO: 使用 SoundPool 加载 assets/sounds/ 播放
    }
}
