package xyz.xiao6.myboard.ime

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.provider.Settings
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
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
import xyz.xiao6.myboard.core.dictionary.DictionaryImporter
import xyz.xiao6.myboard.core.dictionary.SuggestionEngine
import xyz.xiao6.myboard.core.input.CompositionInputEngine
import xyz.xiao6.myboard.core.input.ComposingResolver
import xyz.xiao6.myboard.core.input.ComposingResult
import xyz.xiao6.myboard.core.input.DirectInputEngine
import xyz.xiao6.myboard.core.input.InputEngine
import xyz.xiao6.myboard.core.input.InputMethodConfig
import xyz.xiao6.myboard.core.input.LanguageInfo
import xyz.xiao6.myboard.core.input.LanguageRegistry
import xyz.xiao6.myboard.core.input.LanguageSwitchManager
import xyz.xiao6.myboard.core.input.SwitchRule
import xyz.xiao6.myboard.core.keyboard.ActionDispatcher
import xyz.xiao6.myboard.core.keyboard.EngineResult
import xyz.xiao6.myboard.core.keyboard.InputAction
import xyz.xiao6.myboard.core.keyboard.KeyboardStateManager
import xyz.xiao6.myboard.core.layout.KeyboardLayout
import xyz.xiao6.myboard.core.layout.LayoutParser
import xyz.xiao6.myboard.core.settings.SettingsManager
import xyz.xiao6.myboard.ui.candidate.CandidateBar
import xyz.xiao6.myboard.ui.keyboard.ComposableInputView

/**
 * 重构后的 IME 服务。
 */
class MyBoardImeService : InputMethodService(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private lateinit var stateManager: KeyboardStateManager
    private lateinit var actionDispatcher: ActionDispatcher
    private lateinit var suggestionEngine: SuggestionEngine
    private lateinit var settings: SettingsManager
    private lateinit var languageRegistry: LanguageRegistry
    private lateinit var languageSwitchManager: LanguageSwitchManager

    private var currentLayout: KeyboardLayout? = null
    private var currentEngine: InputEngine? = null
    private val engines = mutableMapOf<String, InputEngine>()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        stateManager = KeyboardStateManager()
        actionDispatcher = ActionDispatcher(stateManager)
        suggestionEngine = SuggestionEngine()
        settings = SettingsManager(this)

        // 加载词典
        val importer = DictionaryImporter()
        val entries = importer.importFromAssets(this, "dictionary/base.dict.txt")
        suggestionEngine.loadDictionary(entries.map { it.word to it.frequency })

        // 初始化语言注册表
        languageRegistry = LanguageRegistry()
        languageRegistry.register(LanguageInfo("en_us", "English", "DIRECT_LTR", "LTR", "alpha", "en_qwerty"))
        languageRegistry.register(LanguageInfo("zh_cn", "中文", "COMPOSITION", "LTR", "pinyin", "zh_pinyin"))

        val rules = listOf(
            SwitchRule("COMPOSITION", "DIRECT_LTR"),
            SwitchRule("DIRECT_LTR", "COMPOSITION"),
            SwitchRule("*", "*")
        )
        languageSwitchManager = LanguageSwitchManager(rules, languageRegistry)

        initEngines()
        loadLayout("qwerty")
    }

    private fun initEngines() {
        val enConfig = InputMethodConfig(
            id = "en_qwerty",
            name = "English",
            engine = "direct",
            language = "en-US",
            shift = xyz.xiao6.myboard.core.input.ShiftConfig(mode = "autoOff"),
            enter = xyz.xiao6.myboard.core.input.EnterConfig(),
            space = xyz.xiao6.myboard.core.input.SpaceConfig(),
            backspace = xyz.xiao6.myboard.core.input.BackspaceConfig()
        )
        engines["en_qwerty"] = DirectInputEngine(enConfig, suggestionEngine)

        val zhConfig = InputMethodConfig(
            id = "zh_pinyin",
            name = "中文 (拼音)",
            engine = "composition",
            language = "zh-CN",
            engineParams = mapOf("autoCommitOnSpace" to "true", "composingType" to "pinyin"),
            shift = xyz.xiao6.myboard.core.input.ShiftConfig(mode = "disabled"),
            enter = xyz.xiao6.myboard.core.input.EnterConfig(composing = "commitThenAction"),
            space = xyz.xiao6.myboard.core.input.SpaceConfig(composing = "commitComposition", hasCandidates = "selectFirst"),
            backspace = xyz.xiao6.myboard.core.input.BackspaceConfig(composing = "deleteComposition")
        )
        engines["zh_pinyin"] = CompositionInputEngine(zhConfig, suggestionEngine, PinyinComposingResolver())

        currentEngine = engines["en_qwerty"]
    }

    private fun loadLayout(layoutId: String) {
        try {
            val text = assets.open("layouts/$layoutId.json").bufferedReader().readText()
            currentLayout = LayoutParser.parse(text)
        } catch (_: Exception) {
            // Fallback to test layout
            val text = assets.open("layouts/test_qwerty.json").bufferedReader().readText()
            currentLayout = LayoutParser.parse(text)
        }
    }

    override fun onCreateInputView(): View {
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED

        val composeView = ComposeView(this).apply {
            setContent {
                val state by stateManager.state.collectAsState()
                val layout = currentLayout

                if (layout != null) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // 工具栏/候选栏切换：有候选时显示候选栏，否则显示工具栏
                        if (state.hasCandidates || state.isComposing) {
                            // 候选栏
                            CandidateBar(
                                candidates = state.candidates,
                                selectedIndex = state.selectedCandidateIndex,
                                onCandidateClick = { index ->
                                    serviceScope.launch {
                                        currentEngine?.onCandidateSelected(index)
                                        stateManager.clearComposing()
                                        updateInputView()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            // 工具栏
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                                    .background(Color(0xFFF1F3F4)),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                IconButton(onClick = {
                                    serviceScope.launch {
                                        val from = stateManager.state.value.languageId
                                        val to = if (from == "en_us") "zh_cn" else "en_us"
                                        val switchAction = languageSwitchManager.switch(from, to)
                                        stateManager.update {
                                            it.copy(
                                                languageId = switchAction.targetLanguage,
                                                shiftState = switchAction.shiftState
                                            )
                                        }
                                        currentEngine = engines[languageRegistry.get(to)?.inputMethodId]
                                        updateInputView()
                                    }
                                }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Language, "语言", tint = Color(0xFF5F6368), modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.EmojiEmotions, "Emoji", tint = Color(0xFF5F6368), modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Star, "符号", tint = Color(0xFF5F6368), modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.ContentPaste, "剪贴板", tint = Color(0xFF5F6368), modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Mic, "语音", tint = Color(0xFF5F6368), modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Settings, "设置", tint = Color(0xFF5F6368), modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        // 主键盘
                        ComposableInputView(
                            layout = layout,
                            state = state,
                            engine = currentEngine,
                            onAction = { action ->
                                serviceScope.launch {
                                    handleAction(action)
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

    private suspend fun handleAction(action: InputAction) {
        val engine = currentEngine ?: return
        val ic = currentInputConnection ?: return

        actionDispatcher.setInputConnection(ic)

        when (action) {
            is InputAction.CommitText -> {
                ic.commitText(action.text, 1)
                stateManager.clearComposing()
            }
            is InputAction.Delete -> {
                ic.deleteSurroundingText(action.count, 0)
            }
            is InputAction.ToggleShift -> {
                val result = engine.onShift()
                handleEngineResult(result)
            }
            is InputAction.ToggleCapsLock -> {
                val result = engine.onDoubleShift()
                handleEngineResult(result)
            }
            is InputAction.SwitchArrangement -> {
                stateManager.update { it.copy(arrangement = action.id) }
            }
            is InputAction.SwitchLanguage -> {
                val from = stateManager.state.value.languageId
                val switchAction = languageSwitchManager.switch(from, action.id)
                stateManager.update {
                    it.copy(
                        languageId = switchAction.targetLanguage,
                        arrangement = switchAction.arrangement,
                        shiftState = switchAction.shiftState
                    )
                }
                currentEngine = engines[languageRegistry.get(action.id)?.inputMethodId]
            }
            is InputAction.SelectCandidate -> {
                val result = engine.onCandidateSelected(action.index)
                handleEngineResult(result)
            }
            is InputAction.PerformEditorAction -> {
                ic.performEditorAction(EditorInfo.IME_ACTION_DONE)
            }
            else -> {}
        }
    }

    private fun handleEngineResult(result: EngineResult) {
        val ic = currentInputConnection ?: return

        when (result) {
            is EngineResult.CommitText -> {
                ic.commitText(result.text, 1)
                stateManager.clearComposing()
            }
            is EngineResult.UpdateComposing -> {
                stateManager.update { it.copy(composingText = result.text) }
            }
            is EngineResult.Combined -> {
                if (result.commit != null) {
                    ic.commitText(result.commit, 1)
                }
                stateManager.setComposing(
                    result.composing ?: "",
                    result.candidates ?: emptyList()
                )
            }
            is EngineResult.UpdateCandidates -> {
                stateManager.update { it.copy(candidates = result.candidates) }
            }
            is EngineResult.Delete -> {
                ic.deleteSurroundingText(result.count, 0)
            }
            is EngineResult.Nothing -> {}
        }
    }

    private fun updateInputView() {
        stateManager.update { it.copy() }
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        val inputType = attribute?.inputType ?: 0
        val arrangement = when (inputType and 0xF) {
            2 -> "number"
            3 -> "phone"
            else -> "alpha"
        }
        stateManager.update { it.copy(arrangement = arrangement) }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        stateManager.clearComposing()
    }

    override fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        serviceScope.cancel()
        super.onDestroy()
    }
}

class PinyinComposingResolver : ComposingResolver {
    override fun resolve(buffer: String, params: Map<String, String>): ComposingResult {
        return ComposingResult(displayText = buffer)
    }
}
