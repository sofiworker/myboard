package xyz.xiao6.myboard.app

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.*
import xyz.xiao6.myboard.androidbridge.*
import xyz.xiao6.myboard.contract.input.*
import xyz.xiao6.myboard.contract.layout.*
import xyz.xiao6.myboard.contract.manifest.*
import xyz.xiao6.myboard.contract.theme.*
import xyz.xiao6.myboard.contract.engine.*
import xyz.xiao6.myboard.contract.bridge.*
import xyz.xiao6.myboard.contract.registry.*
import xyz.xiao6.myboard.contract.panel.*
import xyz.xiao6.myboard.contract.language.*
import xyz.xiao6.myboard.contract.state.*
import xyz.xiao6.myboard.data.db.SettingsDatabase
import xyz.xiao6.myboard.data.entity.ToolbarLayoutMode
import xyz.xiao6.myboard.data.migration.SpToRoomMigration
import xyz.xiao6.myboard.data.repository.SettingsRepository
import xyz.xiao6.myboard.data.settings.KeyboardHeightPolicy
import xyz.xiao6.myboard.engine.*
import xyz.xiao6.myboard.engine.builtin.*
import xyz.xiao6.myboard.layout.*
import xyz.xiao6.myboard.state.*
import xyz.xiao6.myboard.theme.*
import xyz.xiao6.myboard.state.BuiltInManifests
import xyz.xiao6.myboard.toolbar.ThemeToggler
import xyz.xiao6.myboard.toolbar.LayoutSwitcher
import xyz.xiao6.myboard.clipboard.ClipboardManagerWrapper
import xyz.xiao6.myboard.ui.keyboard.CandidateBar
import xyz.xiao6.myboard.ui.keyboard.Toolbar
import xyz.xiao6.myboard.ui.panels.*
import kotlin.math.roundToInt

/**
 * 重构后的 IME 服务。
 * 使用正交状态管理架构，设置数据通过 Room + Repository 管理。
 */
class MyBoardImeService : InputMethodService(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    // 核心组件
    private lateinit var keyboardContextManager: KeyboardContextManagerImpl
    private lateinit var inputPipeline: InputPipelineImpl
    private lateinit var inputConnectionGateway: InputConnectionGatewayImpl
    private lateinit var feedbackPlayer: FeedbackPlayerImpl
    private lateinit var editorInfoResolver: EditorInfoResolverImpl
    private lateinit var layoutRegistry: LayoutRegistryImpl
    private lateinit var layoutMeasurer: LayoutMeasurerImpl
    private lateinit var engineRegistry: EngineRegistryImpl
    private lateinit var orthogonalRegistry: OrthogonalRegistryImpl
    private lateinit var transitionEngine: TransitionEngineImpl
    private lateinit var themeResolver: ThemeResolverImpl
    private lateinit var layoutAssetsLoader: LayoutAssetsLoader

    // 辅助组件
    private lateinit var encoderRegistry: EncoderRegistryImpl
    private lateinit var candidatePolicyRegistry: CandidatePolicyRegistryImpl
    private lateinit var displayPolicyRegistry: DisplayPolicyRegistryImpl
    private lateinit var dictionaryRegistry: DictionaryRegistryImpl
    private lateinit var engineResourceResolver: EngineResourceResolverImpl
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var themeToggler: ThemeToggler
    private lateinit var layoutSwitcher: LayoutSwitcher
    private lateinit var clipboardManager: ClipboardManagerWrapper

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // 当前测量布局
    private var measuredLayout: MeasuredLayout? = null

    /** 保存 ComposeView 引用，用于强制触发重组 */
    private var composeViewRef: ComposeView? = null

    /** 单调递增计数器，每次状态变更 +1，Compose 通过 collectAsState 观察触发重组 */
    private val _uiRevision = mutableLongStateOf(0L)

    private companion object {
        const val KEYBOARD_CHROME_HEIGHT_DP = 40
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        initCoreComponents()
        registerBuiltIns()
    }

    private fun initCoreComponents() {
        // 1. 初始化底层注册表
        engineRegistry = EngineRegistryImpl()
        layoutRegistry = LayoutRegistryImpl()
        dictionaryRegistry = DictionaryRegistryImpl()
        encoderRegistry = EncoderRegistryImpl()
        candidatePolicyRegistry = CandidatePolicyRegistryImpl()
        displayPolicyRegistry = DisplayPolicyRegistryImpl()

        // 2. 初始化资源解析器
        engineResourceResolver = EngineResourceResolverImpl(
            encoderRegistry = encoderRegistry,
            dictionaryRegistry = dictionaryRegistry,
            candidatePolicyRegistry = candidatePolicyRegistry,
            displayPolicyRegistry = displayPolicyRegistry
        )

        // 3. 初始化正交注册表
        orthogonalRegistry = OrthogonalRegistryImpl(
            engineRegistry = engineRegistry,
            layoutRegistry = layoutRegistry,
            dictionaryRegistry = dictionaryRegistry,
            engineResourceResolver = engineResourceResolver
        )

        // 4. 初始化状态转移引擎
        transitionEngine = TransitionEngineImpl(orthogonalRegistry)

        // 5. 初始化键盘上下文管理器
        keyboardContextManager = KeyboardContextManagerImpl(
            transitionEngine = transitionEngine,
            registry = orthogonalRegistry,
            scope = serviceScope
        )

        // 6. 初始化设置数据库和仓库（必须在其他组件之前）
        val settingsDatabase = SettingsDatabase.getInstance(this)
        settingsRepository = SettingsRepository(settingsDatabase.settingsDao())

        // 7. 执行 SP → Room 迁移（一次性）
        serviceScope.launch {
            SpToRoomMigration(this@MyBoardImeService, settingsDatabase.settingsDao()).migrateIfNeeded()
        }

        // 8. 初始化 Android 桥接层
        inputConnectionGateway = InputConnectionGatewayImpl()
        feedbackPlayer = FeedbackPlayerImpl(this, settingsRepository, serviceScope)
        editorInfoResolver = EditorInfoResolverImpl()

        // 9. 初始化输入管线
        inputPipeline = InputPipelineImpl(
            engineRegistry = engineRegistry,
            keyboardContextManager = keyboardContextManager,
            gateway = inputConnectionGateway,
            scope = serviceScope
        )

        // 10. 初始化布局测量器
        layoutMeasurer = LayoutMeasurerImpl()

        // 11. 初始化主题解析器（从设置中读取 theme_mode）
        val savedThemeMode = runBlocking {
            settingsRepository.getSetting("theme_mode") ?: "auto"
        }
        val initialTheme = when (savedThemeMode) {
            "dark" -> BuiltInThemes.dark
            else -> BuiltInThemes.light
        }
        themeResolver = ThemeResolverImpl(initialTheme)

        // 12. 初始化布局加载器
        layoutAssetsLoader = LayoutAssetsLoader(this)

        // 13. 初始化 toolbar 组件
        themeToggler = ThemeToggler(settingsRepository, themeResolver)
        layoutSwitcher = LayoutSwitcher(keyboardContextManager, orthogonalRegistry)

        // 14. 初始化剪贴板管理器
        clipboardManager = ClipboardManagerWrapper(this)
        clipboardManager.startListening()
    }

    private fun registerBuiltIns() {
        // 注册内置引擎
        engineRegistry.register(DirectEngine())
        engineRegistry.register(TableComposingEngine())
        engineRegistry.register(TransliterationEngine())

        // 注册内置编码器
        encoderRegistry.register(IdentityEncoder())

        // 注册内置候选策略
        candidatePolicyRegistry.register(ChineseDefaultPolicy())
        candidatePolicyRegistry.register(DirectDefaultPolicy())
        candidatePolicyRegistry.register(JapaneseKanaDefaultPolicy())

        // 注册内置显示策略
        displayPolicyRegistry.register(ShowQueryPolicy())
        displayPolicyRegistry.register(ShowComposingPolicy())
        displayPolicyRegistry.register(HiddenDisplayPolicy())

        // 注册内置布局
        val layoutIds = listOf(
            "qwerty",
            "shuangpin_ziran",
            "t9_chinese",
            "hiragana",
            "qwerty_dvorak",
            "qwerty_colemak",
            "qwerty_abc",
            "phone_dial",
            "number",
            "candidate_words_page",
            "symbols_full_surface",
            "emoji_full_surface"
        )
        layoutIds.forEach { id ->
            val doc = layoutAssetsLoader.load(id)
            if (doc != null) {
                layoutRegistry.register(doc, LayoutSource.BUILT_IN)
            }
        }

        // 注册内置语言包
        BuiltInManifests.all.forEach { manifest ->
            orthogonalRegistry.register(manifest)
        }
    }

    override fun onCreateInputView(): View {
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED

        val composeView = ComposeView(this).apply {
            setContent {
                val context by keyboardContextManager.context.collectAsState()
                // 观察单调递增计数器，确保 updateInputView() 能触发重组
                @Suppress("UNUSED_VARIABLE")
                val uiRevision = _uiRevision.longValue
                val panelLayoutId = PanelLayoutResolver.layoutIdFor(context.activePanel)
                val activeLayoutId = panelLayoutId ?: context.layoutId
                val layoutDoc = layoutAssetsLoader.load(activeLayoutId)
                    ?: layoutRegistry.get(activeLayoutId)
                val isDark = themeResolver.isDark()

                // 观察工具栏配置
                val toolbarItems by settingsRepository.toolbarItems.collectAsState(initial = emptyList())
                val toolbarLayoutMode by settingsRepository.toolbarLayoutMode.collectAsState(
                    initial = ToolbarLayoutMode.SCROLLABLE
                )
                // 观察键盘高度设置
                val allSettings by settingsRepository.settings.collectAsState(initial = emptyMap())
                val dm = this@MyBoardImeService.resources.displayMetrics
                val screenWidthDp = (dm.widthPixels / dm.density).roundToInt()
                val screenHeightDp = (dm.heightPixels / dm.density).roundToInt()
                LaunchedEffect(screenHeightDp, screenWidthDp) {
                    settingsRepository.ensureKeyboardLayoutMetrics(
                        screenHeightDp = screenHeightDp,
                        screenWidthDp = screenWidthDp
                    )
                }
                val keyboardHeightDp = KeyboardHeightPolicy.resolve(
                    storedHeight = allSettings[KeyboardHeightPolicy.KEY_HEIGHT],
                    screenHeightDp = screenHeightDp
                ).heightDp
                val keyboardHorizontalInsetDp = KeyboardHeightPolicy.resolveHorizontalInset(
                    storedInset = allSettings[KeyboardHeightPolicy.KEY_HORIZONTAL_INSET],
                    screenWidthDp = screenWidthDp
                ).insetDp
                val keyboardContentHeightDp = KeyboardHeightPolicy.contentHeightDp(
                    pageHeightDp = keyboardHeightDp,
                    chromeHeightDp = KEYBOARD_CHROME_HEIGHT_DP
                )
                val isFullSurfaceLayout = layoutDoc?.presentationMode == LayoutPresentationMode.FULL_SURFACE
                val layoutMeasureHeightDp = if (isFullSurfaceLayout) keyboardHeightDp else keyboardContentHeightDp

                // 测量布局（使用实际像素尺寸和设备密度）
                val currentMeasured = remember(activeLayoutId, context.layer, layoutMeasureHeightDp, keyboardHorizontalInsetDp) {
                    if (layoutDoc != null) {
                        val horizontalInsetPx = (keyboardHorizontalInsetDp * dm.density).roundToInt()
                        val widthPx = (dm.widthPixels - horizontalInsetPx * 2).coerceAtLeast(1)
                        val keyboardHeightPx = (layoutMeasureHeightDp * dm.density).roundToInt()
                        layoutMeasurer.measure(
                            layoutDoc, context.layer,
                            widthPx, keyboardHeightPx,
                            dm.density
                        )
                    } else {
                        null
                    }
                }
                measuredLayout = currentMeasured

                val chrome = themeResolver.resolveChromeColors()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(keyboardHeightDp.dp)
                        .background(chrome.background)
                        .padding(horizontal = keyboardHorizontalInsetDp.dp)
                ) {
                    // 通用回调
                    val closePanelAndRefresh: () -> Unit = {
                        serviceScope.launch {
                            inputPipeline.handle(InputAction.ClosePanel)
                            updateInputView()
                        }
                    }
                    val hideKeyboard: () -> Unit = { requestHideSelf(0) }

                    // 面板视图（仅在面板激活时显示）
                    when (context.activePanel) {
                        PanelType.SYMBOL -> {
                            if (panelLayoutId == null) {
                                SymbolPanel(
                                onSymbolClick = { symbol ->
                                    serviceScope.launch {
                                        inputPipeline.handle(InputAction.PushToken(symbol))
                                        updateInputView()
                                    }
                                },
                                onBack = closePanelAndRefresh,
                                onHideKeyboard = hideKeyboard
                                )
                            }
                        }
                        PanelType.EMOJI -> {
                            if (panelLayoutId == null) {
                                EmojiPanel(
                                categories = listOf(
                                    Triple("face", "😀", listOf("😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂", "🙂", "😊", "😇", "🥰", "😍", "🤩", "😘", "😗", "😚", "😙", "🥲", "😋", "😛", "😜", "🤪", "😝")),
                                    Triple("heart", "❤️", listOf("❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔", "❤️‍🔥", "💕", "💞", "💓", "💗", "💖", "💘", "💝", "💟")),
                                    Triple("hand", "👋", listOf("👋", "🤚", "🖐️", "✋", "🖖", "👌", "🤌", "🤏", "✌️", "🤞", "🫰", "🤟", "🤘", "🤙", "👈", "👉", "👆", "👇", "☝️", "👍", "👎", "✊", "👊", "🤛", "🤜", "👏", "🙌", "🫶", "👐", "🤲", "🙏"))
                                ),
                                onEmojiClick = { emoji ->
                                    serviceScope.launch {
                                        inputPipeline.handle(InputAction.PushToken(emoji))
                                        updateInputView()
                                    }
                                },
                                onBack = closePanelAndRefresh,
                                onHideKeyboard = hideKeyboard
                                )
                            }
                        }
                        PanelType.CLIPBOARD -> {
                            ClipboardPanel(
                                entries = clipboardManager.getHistory(),
                                onEntryClick = { entry ->
                                    serviceScope.launch {
                                        inputPipeline.handle(InputAction.PushToken(entry.text))
                                        inputPipeline.handle(InputAction.ClosePanel)
                                        updateInputView()
                                    }
                                },
                                onDeleteEntry = { entry ->
                                    clipboardManager.removeEntry(entry)
                                    updateInputView()
                                },
                                onClearAll = {
                                    clipboardManager.clearHistory()
                                    updateInputView()
                                },
                                onBack = closePanelAndRefresh,
                                onHideKeyboard = hideKeyboard
                            )
                        }
                        PanelType.KAOMOJI -> {
                            KaomojiPanel(
                                categories = listOf(
                                    "开心" to listOf("(≧▽≦)", "(◕‿◕)", "٩(◕‿◕)۶", "(ﾉ◕ヮ◕)ﾉ*:・ﾟ✧", "(´▽`ʃ♡ƪ)", "(≧◡≦)"),
                                    "难过" to listOf("(╥_╥)", "(T_T)", "(；д；)", "ಥ_ಥ", "(；ω；)", "(´;ω;`)"),
                                    "愤怒" to listOf("(╬￣皿￣)", "щ(｀Д´щ)", "(°ロ°)!", "(ᗒᗣᗕ)՞", "(ノ｀ダ)ノ", "Σ( ° △ °|||)"),
                                    "惊讶" to listOf("(⊙_⊙)", "(°o°)", "Σ( ° △ °|||)", "(ﾟДﾟ)", "(O_O)", "Σ(ﾟдﾟ)")
                                ),
                                onKaomojiClick = { kaomoji ->
                                    serviceScope.launch {
                                        inputPipeline.handle(InputAction.PushToken(kaomoji))
                                        updateInputView()
                                    }
                                },
                                onBack = closePanelAndRefresh,
                                onHideKeyboard = hideKeyboard
                            )
                        }
                        PanelType.LOCALE_SWITCH, PanelType.LAYOUT_SWITCH -> {
                            val locales = BuiltInManifests.all.mapNotNull { manifest ->
                                orthogonalRegistry.getLocale(manifest.locale)
                            }
                            LocaleLayoutSwitchPanel(
                                locales = locales,
                                currentLocale = context.orthogonal.locale,
                                currentSchema = context.orthogonal.schema,
                                schemasForLocale = { locale ->
                                    val localeCap = orthogonalRegistry.getLocale(locale)
                                    val defaultScript = localeCap?.defaults?.script
                                    val scriptCap = localeCap?.scripts?.get(defaultScript)
                                    scriptCap?.schemas?.keys?.toList() ?: emptyList()
                                },
                                getSchemaDisplayName = layoutSwitcher::getSchemaDisplayName,
                                onLocaleSelected = { targetLocale ->
                                    serviceScope.launch {
                                        inputPipeline.handle(InputAction.SwitchLocale(targetLocale))
                                        updateInputView()
                                    }
                                },
                                onSchemaSelected = { schema ->
                                    serviceScope.launch {
                                        layoutSwitcher.switchToSchema(schema)
                                        updateInputView()
                                    }
                                },
                                onBack = closePanelAndRefresh,
                                onHideKeyboard = hideKeyboard
                            )
                        }
                        PanelType.LLM, PanelType.STT, PanelType.TEXT_EXPANSION -> {
                            PlaceholderPanel(
                                panelType = context.activePanel,
                                onBack = closePanelAndRefresh,
                                onHideKeyboard = hideKeyboard
                            )
                        }
                        PanelType.NONE -> { /* 不显示面板 */ }
                    }

                    // 工具栏/候选栏切换（仅在无面板时显示）
                    if (context.activePanel == PanelType.NONE && !isFullSurfaceLayout) {
                        if (context.hasCandidates || context.isComposing) {
                            // 候选栏
                            CandidateBar(
                                candidates = context.candidates,
                                selectedIndex = context.selectedCandidateIndex,
                                onCandidateClick = { index ->
                                    serviceScope.launch {
                                        inputPipeline.handle(InputAction.CommitCandidate(index))
                                        updateInputView()
                                    }
                                },
                                chrome = chrome,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            // 工具栏（三区布局：左固定 ⚙ / 中间可配置 / 右固定 ↓）
                            Toolbar(
                                items = toolbarItems.filter { it.enabled },
                                layoutMode = toolbarLayoutMode,
                                isDark = isDark,
                                chrome = chrome,
                                onSettingsClick = {
                                    val intent = android.content.Intent(
                                        this@MyBoardImeService,
                                        xyz.xiao6.myboard.activity.SettingsActivity::class.java
                                    )
                                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    startActivity(intent)
                                },
                                onHideKeyboard = {
                                    requestHideSelf(0)
                                },
                                onThemeToggle = {
                                    serviceScope.launch {
                                        themeToggler.toggle()
                                        updateInputView()
                                    }
                                },
                                onPanelOpen = { panelType ->
                                    serviceScope.launch {
                                        inputPipeline.handle(InputAction.OpenPanel(panelType))
                                        updateInputView()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // 主键盘
                    if (currentMeasured != null && (context.activePanel == PanelType.NONE || panelLayoutId != null)) {
                        LayoutRenderer(
                            measuredLayout = currentMeasured,
                            context = context,
                            themeResolver = themeResolver,
                            onAction = { action ->
                                serviceScope.launch {
                                    inputPipeline.handle(action)
                                    feedbackPlayer.playHaptic(
                                        HapticToken(id = "key_tap", durationMs = 10, amplitude = 50)
                                    )
                                    updateInputView()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }
                }
            }
        }

        window?.window?.decorView?.let { decorView ->
            decorView.setViewTreeLifecycleOwner(this)
            decorView.setViewTreeSavedStateRegistryOwner(this)
        }

        composeViewRef = composeView
        return composeView
    }

    private fun updateInputView() {
        // 递增计数器触发 Compose 重组
        _uiRevision.longValue++
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)

        // 解析 EditorInfo
        val profile = editorInfoResolver.resolve(attribute, keyboardContextManager.context.value.orthogonal.locale)
        keyboardContextManager.applyEditorProfile(profile)

        // 更新 InputConnection
        inputConnectionGateway.update(currentInputConnection)

        // 重置组合态
        if (!restarting) {
            serviceScope.launch {
                inputPipeline.reset(ResetReason.InputStarted)
            }
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)

        // 清除组合态
        serviceScope.launch {
            inputPipeline.reset(ResetReason.InputFinished)
        }
    }

    override fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        serviceScope.cancel()
        super.onDestroy()
    }
}
