package xyz.xiao6.myboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import xyz.xiao6.myboard.ui.settings.FuzzyPinyinSettingsScreen
import xyz.xiao6.myboard.ui.theme.MyboardTheme

class FuzzyPinyinSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyboardTheme(themeData = null) {
                FuzzyPinyinSettingsScreen()
            }
        }
    }
}
