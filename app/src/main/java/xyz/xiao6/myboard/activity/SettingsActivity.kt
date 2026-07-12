package xyz.xiao6.myboard.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import xyz.xiao6.myboard.data.db.SettingsDatabase
import xyz.xiao6.myboard.data.repository.SettingsRepository
import xyz.xiao6.myboard.ui.settings.*
import xyz.xiao6.myboard.ui.theme.MyBoardTheme

/**
 * 设置页面 Activity。
 * 使用 Jetpack Navigation 管理多页面导航，所有 ViewModel 共享同一个 SettingsRepository。
 */
class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MyBoardTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val repo = remember {
                        SettingsRepository(SettingsDatabase.getInstance(this@SettingsActivity).settingsDao())
                    }
                    val configuration = LocalConfiguration.current
                    LaunchedEffect(configuration.screenHeightDp, configuration.screenWidthDp) {
                        repo.ensureKeyboardLayoutMetrics(
                            screenHeightDp = configuration.screenHeightDp,
                            screenWidthDp = configuration.screenWidthDp
                        )
                    }

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
}
