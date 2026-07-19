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
import xyz.xiao6.myboard.ui.theme.MyBoardTheme

/**
 * 引导页。4 页流程：
 * 功能展示 → IME 检测 → 语言选择（含方案 Dialog） → 完成
 */
class OnboardingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyBoardTheme {
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

    LaunchedEffect(Unit) {
        viewModel.initializeWithSystemLocale()
    }

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
                        onRefreshCheck = { viewModel.refreshImeCheck() },
                        onSkip = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(2)
                            }
                        }
                    )
                    2 -> LanguageSelectionPage(
                        selectedLanguages = uiState.selectedLanguages,
                        showSchemaDialog = uiState.schemaDialogLocale != null,
                        schemaDialogLocale = uiState.schemaDialogLocale,
                        schemaDialogSchemas = uiState.schemaDialogSchemas,
                        onToggleLanguage = { locale, schema ->
                            viewModel.toggleLanguage(locale, schema)
                        },
                        onOpenSchemaDialog = { viewModel.openSchemaDialog(it) },
                        onToggleDialogSchema = { viewModel.toggleDialogSchema(it) },
                        onConfirmSchemaDialog = { viewModel.confirmSchemaDialog() },
                        onDismissSchemaDialog = { viewModel.dismissSchemaDialog() },
                        onFinish = { viewModel.finishLanguageSelection() }
                    )
                    3 -> CompletionPage(
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

    val currentPage = pagerState.currentPage
    LaunchedEffect(uiState.isImeEnabled) {
        if (uiState.isImeEnabled && currentPage == 1) {
            pagerState.animateScrollToPage(2)
        }
    }

    // 当 ViewModel 切换页面时同步 pager
    LaunchedEffect(uiState.currentPage) {
        if (pagerState.currentPage != uiState.currentPage) {
            pagerState.animateScrollToPage(uiState.currentPage)
        }
    }
}
