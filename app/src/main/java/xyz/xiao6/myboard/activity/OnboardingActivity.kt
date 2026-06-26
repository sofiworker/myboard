package xyz.xiao6.myboard.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import xyz.xiao6.myboard.data.db.SettingsDatabase
import xyz.xiao6.myboard.data.repository.SettingsRepository
import xyz.xiao6.myboard.ui.onboarding.*

/**
 * 现代化 Compose 引导页。
 * 5 页流程：功能展示 → IME 检测 → 语言选择 → 布局选择 → 完成
 */
class OnboardingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                OnboardingContent()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OnboardingContent() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repo = remember {
        SettingsRepository(SettingsDatabase.getInstance(context).settingsDao())
    }
    val viewModel: OnboardingViewModel = viewModel(
        factory = OnboardingViewModel.Factory(context, repo)
    )
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { uiState.totalPages }
    )
    val coroutineScope = rememberCoroutineScope()

    // 初始化系统语言预选
    LaunchedEffect(Unit) {
        viewModel.initializeWithSystemLocale()
    }

    // 同步 pager 状态与 ViewModel 状态
    LaunchedEffect(pagerState.currentPage) {
        viewModel.setPage(pagerState.currentPage)
    }

    Scaffold(
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PageIndicator(
                        pageCount = uiState.totalPages,
                        currentPage = uiState.currentPage
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // ---- 方案编辑模式（覆盖在页面之上） ----
            if (uiState.editingLocale != null) {
                SchemaSelectionPage(
                    locale = uiState.editingLocale,
                    selectedSchemas = uiState.editingSchemas,
                    onToggleSchema = { viewModel.toggleSchema(it) },
                    onNext = { viewModel.nextSchemaOrFinish() },
                    onSkip = { viewModel.confirmEditSchemas(); viewModel.setPage(4) }
                )
            } else {
                // ---- 标准引导页 HorizontalPager ----
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = false,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> FeatureShowcasePage(
                            onNext = {
                                coroutineScope.launch {
                                    viewModel.startImeCheck()
                                    pagerState.animateScrollToPage(1)
                                }
                            }
                        )
                        1 -> ImeEnablePage(
                            isImeEnabled = uiState.isImeEnabled,
                            isChecking = uiState.isCheckingIme,
                            onRefreshCheck = { viewModel.refreshImeCheck() }
                        )
                        2 -> LanguageSelectionPage(
                            selectedLanguages = uiState.selectedLanguages,
                            onToggleLanguage = { locale, schema ->
                                viewModel.toggleLanguage(locale, schema)
                            },
                            onNext = {
                                if (uiState.selectedLanguages.isNotEmpty()) {
                                    viewModel.goToLayoutSelection()
                                }
                            }
                        )
                        4 -> CompletionPage(
                            isCompleting = uiState.isCompleting,
                            onComplete = {
                                viewModel.completeOnboarding {
                                    context.startActivity(
                                        Intent(context, SettingsActivity::class.java)
                                    )
                                    (context as? ComponentActivity)?.finish()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // 监听 IME 启用状态 -> 当检测到启用后自动跳页
    LaunchedEffect(uiState.isImeEnabled) {
        if (uiState.isImeEnabled && pagerState.currentPage == 1) {
            pagerState.animateScrollToPage(2)
        }
    }
}
