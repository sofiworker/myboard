package xyz.xiao6.myboard.androidbridge

import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import xyz.xiao6.myboard.contract.theme.*
import xyz.xiao6.myboard.data.repository.SettingsRepository

/**
 * 触觉/声音反馈播放器。
 * 从 SettingsRepository 实时观察 haptic_feedback / sound_feedback 设置。
 */
class FeedbackPlayerImpl(
    private val context: Context,
    private val repo: SettingsRepository,
    private val scope: CoroutineScope
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

    private var hapticEnabled = true
    private var soundEnabled = false

    init {
        scope.launch {
            repo.observeSetting("haptic_feedback").collect { raw ->
                hapticEnabled = raw?.toBooleanStrictOrNull() ?: true
            }
        }
        scope.launch {
            repo.observeSetting("sound_feedback").collect { raw ->
                soundEnabled = raw?.toBooleanStrictOrNull() ?: false
            }
        }
    }

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
        if (!soundEnabled) return
        // TODO: 使用 SoundPool 加载 assets/sounds/ 播放
    }
}
