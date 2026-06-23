package xyz.xiao6.myboard.ime

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import xyz.xiao6.myboard.core.androidbridge.*
import xyz.xiao6.myboard.core.contract.*
import xyz.xiao6.myboard.core.engine.*
import xyz.xiao6.myboard.core.engine.builtin.*
import xyz.xiao6.myboard.core.layout.*
import xyz.xiao6.myboard.core.state.*
import xyz.xiao6.myboard.core.theme.*
import xyz.xiao6.myboard.ui.candidate.CandidateBar

/**
 * 重构后的 IME 服务。
 * 使用正交状态管理架构。
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
    
    // 辅助组件
    private lateinit var encoderRegistry: EncoderRegistryImpl
    private lateinit var candidatePolicyRegistry: CandidatePolicyRegistryImpl
    private lateinit var displayPolicyRegistry: DisplayPolicyRegistryImpl
    private lateinit var dictionaryRegistry: DictionaryRegistryImpl
    private lateinit var engineResourceResolver: EngineResourceResolverImpl
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // 当前测量布局
    private var measuredLayout: MeasuredLayout? = null

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
        
        // 6. 初始化 Android 桥接层
        inputConnectionGateway = InputConnectionGatewayImpl()
        feedbackPlayer = FeedbackPlayerImpl(this)
        editorInfoResolver = EditorInfoResolverImpl()
        
        // 7. 初始化输入管线
        inputPipeline = InputPipelineImpl(
            engineRegistry = engineRegistry,
            keyboardContextManager = keyboardContextManager,
            gateway = inputConnectionGateway,
            scope = serviceScope
        )
        
        // 8. 初始化布局测量器
        layoutMeasurer = LayoutMeasurerImpl()
        
        // 9. 初始化主题解析器
        themeResolver = ThemeResolverImpl(BuiltInThemes.light)
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
        val qwertyDoc = BuiltInLayouts.qwerty
        layoutRegistry.register(qwertyDoc, LayoutSource.BUILT_IN)
        
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
                val layoutDoc = layoutRegistry.get(context.layoutId)
                val isDark = themeResolver.isDark()
                
                // 测量布局
                val currentMeasured = remember(context.layoutId, context.layer) {
                    if (layoutDoc != null) {
                        layoutMeasurer.measure(layoutDoc, context.layer, 360, 220)
                    } else {
                        null
                    }
                }
                measuredLayout = currentMeasured
                
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 工具栏/候选栏切换
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
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // 工具栏
                        Toolbar(
                            context = context,
                            onLocaleSwitch = { targetLocale ->
                                serviceScope.launch {
                                    inputPipeline.handle(InputAction.SwitchLocale(targetLocale))
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
                    
                    // 主键盘
                    if (currentMeasured != null) {
                        LayoutRenderer(
                            measuredLayout = currentMeasured,
                            context = context,
                            themeResolver = themeResolver,
                            onAction = { action ->
                                serviceScope.launch {
                                    inputPipeline.handle(action)
                                    feedbackPlayer.playHaptic(
                                        HapticToken(
                                            id = "key_tap",
                                            durationMs = 10,
                                            amplitude = 50
                                        )
                                    )
                                    updateInputView()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        )
                    }
                }
            }
        }
        
        window?.window?.decorView?.let { decorView ->
            decorView.setViewTreeLifecycleOwner(this)
            decorView.setViewTreeSavedStateRegistryOwner(this)
        }
        
        return composeView
    }
    
    private fun updateInputView() {
        // 触发 UI 重绘
        keyboardContextManager.context.value.let { _ ->
            // StateFlow 会自动触发重组
        }
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

/**
 * 工具栏 Composable。
 */
@Composable
private fun Toolbar(
    context: KeyboardContext,
    onLocaleSwitch: (LocaleTag) -> Unit,
    onPanelOpen: (PanelType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(36.dp)
            .background(Color(0xFFF1F3F4)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // 语言切换
        IconButton(
            onClick = {
                // 循环切换：en-US <-> zh-CN <-> ja-JP
                val currentLocale = context.orthogonal.locale
                val nextLocale = when (currentLocale.value) {
                    "en-US" -> LocaleTag("zh-CN")
                    "zh-CN" -> LocaleTag("ja-JP")
                    else -> LocaleTag("en-US")
                }
                onLocaleSwitch(nextLocale)
            },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Language,
                "语言",
                tint = Color(0xFF5F6368),
                modifier = Modifier.size(18.dp)
            )
        }
        
        // Emoji 面板
        IconButton(
            onClick = { onPanelOpen(PanelType.EMOJI) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.EmojiEmotions,
                "Emoji",
                tint = Color(0xFF5F6368),
                modifier = Modifier.size(18.dp)
            )
        }
        
        // 符号面板
        IconButton(
            onClick = { onPanelOpen(PanelType.SYMBOL) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Star,
                "符号",
                tint = Color(0xFF5F6368),
                modifier = Modifier.size(18.dp)
            )
        }
        
        // 剪贴板面板
        IconButton(
            onClick = { onPanelOpen(PanelType.CLIPBOARD) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.ContentPaste,
                "剪贴板",
                tint = Color(0xFF5F6368),
                modifier = Modifier.size(18.dp)
            )
        }
        
        // LLM 面板
        IconButton(
            onClick = { onPanelOpen(PanelType.LLM) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.SmartToy,
                "AI",
                tint = Color(0xFF5F6368),
                modifier = Modifier.size(18.dp)
            )
        }
        
        // STT 面板
        IconButton(
            onClick = { onPanelOpen(PanelType.STT) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Mic,
                "语音",
                tint = Color(0xFF5F6368),
                modifier = Modifier.size(18.dp)
            )
        }
        
        // 设置
        IconButton(
            onClick = { },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Settings,
                "设置",
                tint = Color(0xFF5F6368),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}