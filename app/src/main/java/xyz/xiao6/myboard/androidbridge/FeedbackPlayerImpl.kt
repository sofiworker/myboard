package xyz.xiao6.myboard.androidbridge

import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import android.content.Context
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
 * 触觉反馈播放器真实实现。
 */
class FeedbackPlayerImpl(
    private val context: Context
) : FeedbackPlayer {

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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
                @Suppress("DEPRECATION")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = android.os.VibrationEffect.createOneShot(
                        token.durationMs,
                        token.amplitude.coerceIn(1, 255)
                    )
                    vibrator.vibrate(effect)
                } else {
                    vibrator.vibrate(token.durationMs)
                }
            }
        } catch (_: Exception) {
            // 振动硬件不可用
        }
    }

    override fun playSound(token: SoundToken) {
        // TODO: 使用 SoundPool 加载 assets/sounds/ 播放
    }
}
