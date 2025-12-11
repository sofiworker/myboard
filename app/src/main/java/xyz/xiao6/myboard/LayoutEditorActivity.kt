package xyz.xiao6.myboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import xyz.xiao6.myboard.ui.settings.LayoutEditorScreen

class LayoutEditorActivity : ComponentActivity() {

    companion object {
        const val EXTRA_LAYOUT_NAME = "layout_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layoutName = intent.getStringExtra(EXTRA_LAYOUT_NAME)
        if (layoutName == null) {
            finish()
            return
        }
        setContent {
            LayoutEditorScreen(layoutName = layoutName)
        }
    }
}
