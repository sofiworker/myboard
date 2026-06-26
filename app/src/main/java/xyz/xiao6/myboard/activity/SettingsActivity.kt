package xyz.xiao6.myboard.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import xyz.xiao6.myboard.data.db.SettingsDatabase
import xyz.xiao6.myboard.data.repository.SettingsRepository
import xyz.xiao6.myboard.ui.settings.*

/**
 * 设置页面 Activity。
 * 使用 Jetpack Navigation 管理多页面导航，所有 ViewModel 共享同一个 SettingsRepository。
 */
class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                val repo = SettingsRepository(SettingsDatabase.getInstance(this@SettingsActivity).settingsDao())

                NavHost(navController = navController, startDestination = "settings") {
                    composable("settings") {
                        SettingsScreen(
                            onBack = { finish() },
                            onNavigate = { route -> navController.navigate(route) },
                            viewModel = viewModel(factory = SettingsViewModel.Factory(repo))
                        )
                    }
                    composable("language") {
                        LanguageSettingsScreen(
                            onBack = { navController.popBackStack() },
                            viewModel = viewModel(factory = LanguageSettingsViewModel.Factory(repo))
                        )
                    }
                    composable("toolbar") {
                        ToolbarSettingsScreen(
                            onBack = { navController.popBackStack() },
                            viewModel = viewModel(factory = ToolbarSettingsViewModel.Factory(repo))
                        )
                    }
                    composable("theme") {
                        ThemeSettingsScreen(
                            onBack = { navController.popBackStack() },
                            viewModel = viewModel(factory = SettingsViewModel.Factory(repo))
                        )
                    }
                    composable("feedback") {
                        FeedbackSettingsScreen(
                            onBack = { navController.popBackStack() },
                            viewModel = viewModel(factory = SettingsViewModel.Factory(repo))
                        )
                    }
                    composable("llm") {
                        LLMSettingsScreen(
                            repo = repo,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("stt") {
                        STTSettingsScreen(
                            repo = repo,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("about") {
                        AboutScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
