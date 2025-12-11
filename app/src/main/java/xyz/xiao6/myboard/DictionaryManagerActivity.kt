package xyz.xiao6.myboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import xyz.xiao6.myboard.ui.settings.DictionaryManagerScreen
import xyz.xiao6.myboard.ui.theme.MyboardTheme

class DictionaryManagerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyboardTheme(themeData = null) {
                DictionaryManagerScreen()
            }
        }
    }
}
