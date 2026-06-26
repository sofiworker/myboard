package xyz.xiao6.myboard.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xyz.xiao6.myboard.data.db.SettingsDatabase
import xyz.xiao6.myboard.data.repository.SettingsRepository

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
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? ComponentActivity
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val repo = SettingsRepository(SettingsDatabase.getInstance(context).settingsDao())
        val completed = withContext(Dispatchers.IO) {
            repo.getSetting("onboarding_completed")?.toBooleanStrictOrNull() ?: false
        }
        if (!completed) {
            context.startActivity(Intent(context, OnboardingActivity::class.java))
        } else {
            context.startActivity(Intent(context, SettingsActivity::class.java))
        }
        activity?.finish()
        isLoading = false
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
