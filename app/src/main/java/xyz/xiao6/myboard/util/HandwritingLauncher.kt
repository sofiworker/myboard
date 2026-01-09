package xyz.xiao6.myboard.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import xyz.xiao6.myboard.model.HandwritingLayoutMode
import xyz.xiao6.myboard.model.HandwritingPosition
import xyz.xiao6.myboard.ui.handwriting.HandwritingActivity

/**
 * Contract for launching handwriting input and receiving result
 * 启动手写输入并接收结果的Contract
 */
class HandwritingInputContract : ActivityResultContract<HandwritingInputContract.Input, String?>() {
    data class Input(
        val layoutMode: HandwritingLayoutMode? = null,
        val position: HandwritingPosition? = null,
    )

    override fun createIntent(context: Context, input: Input): Intent {
        return HandwritingActivity.createIntent(
            activity = context as Activity,
            layoutMode = input.layoutMode,
            position = input.position,
        )
    }

    override fun parseResult(resultCode: Int, intent: Intent?): String? {
        if (resultCode != Activity.RESULT_OK || intent == null) return null
        return intent.getStringExtra(HandwritingActivity.EXTRA_COMMIT_TEXT)
    }
}

/**
 * Helper to launch handwriting input from IME service
 * 从IME服务启动手写输入的助手类
 */
object HandwritingLauncher {
    /**
     * Launch handwriting input activity
     * 启动手写输入Activity
     */
    fun launch(
        context: Context,
        layoutMode: HandwritingLayoutMode? = null,
        position: HandwritingPosition? = null,
        onResult: (String?) -> Unit,
    ) {
        val intent = HandwritingActivity.createIntent(
            activity = context as Activity,
            layoutMode = layoutMode,
            position = position,
        )

        // For IME service, we need to use a different approach
        // This is a simplified version - in production, you'd use proper activity launching
        context.startActivity(intent)
    }
}
