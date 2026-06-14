package xyz.xiao6.myboard.ime

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.activity.compose.setContent
import xyz.xiao6.myboard.ui.panels.SettingsScreen

/**
 * 设置页面 Activity。
 */
class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                SettingsScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}
