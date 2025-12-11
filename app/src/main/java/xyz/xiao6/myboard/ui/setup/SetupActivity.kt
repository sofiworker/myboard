package xyz.xiao6.myboard.ui.setup

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import xyz.xiao6.myboard.SettingsActivity
import xyz.xiao6.myboard.ui.theme.MyboardTheme
import xyz.xiao6.myboard.util.isImeEnabled
import xyz.xiao6.myboard.util.isImeSelected

class SetupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isImeEnabled(this) && isImeSelected(this)) {
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
            return
        }
        setContent {
            MyboardTheme(themeData = null) {
                SetupScreen()
            }
        }
    }
}
