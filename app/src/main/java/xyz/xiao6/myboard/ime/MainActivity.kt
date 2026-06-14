package xyz.xiao6.myboard.ime

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.xiao6.myboard.core.settings.SettingsManager

/**
 * 主入口页面 - 根据引导状态跳转。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val settings = remember { SettingsManager(context) }

    LaunchedEffect(Unit) {
        if (!settings.onboardingCompleted) {
            context.startActivity(Intent(context, OnboardingActivity::class.java))
            activity?.finish()
        } else {
            context.startActivity(Intent(context, SettingsActivity::class.java))
            activity?.finish()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
