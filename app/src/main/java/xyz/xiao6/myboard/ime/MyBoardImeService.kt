package xyz.xiao6.myboard.ime

import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.LayoutInflater
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.xiao6.myboard.R
import xyz.xiao6.myboard.controller.InputMethodController
import xyz.xiao6.myboard.decoder.DecoderFactory
import xyz.xiao6.myboard.manager.DictionaryManager
import xyz.xiao6.myboard.manager.LayoutManager
import xyz.xiao6.myboard.manager.RuntimeDictionaryManager
import xyz.xiao6.myboard.manager.SubtypeManager
import xyz.xiao6.myboard.manager.ThemeManager
import xyz.xiao6.myboard.manager.ToolbarManager
import xyz.xiao6.myboard.model.ThemeSpec
import xyz.xiao6.myboard.model.ToolbarSpec
import xyz.xiao6.myboard.ui.ime.ImePanelView
import xyz.xiao6.myboard.ui.keyboard.KeyboardSurfaceView
import xyz.xiao6.myboard.ui.popup.FloatingComposingPopup
import xyz.xiao6.myboard.ui.popup.FloatingTextPreviewPopup
import xyz.xiao6.myboard.ui.popup.PopupView
import xyz.xiao6.myboard.ui.toolbar.ToolbarView
import xyz.xiao6.myboard.ui.candidate.CandidatePageView
import xyz.xiao6.myboard.ui.layout.LayoutPickerView
import xyz.xiao6.myboard.ui.symbols.SymbolsLayoutView
import xyz.xiao6.myboard.ui.emoji.EmojiLayoutView
import xyz.xiao6.myboard.ui.ime.KeyboardResizeOverlayView
import xyz.xiao6.myboard.util.MLog
import xyz.xiao6.myboard.util.KeyboardSizeConstraints
import java.util.Locale
import android.content.Intent
import android.content.ClipboardManager
import xyz.xiao6.myboard.ui.SettingsActivity
import android.widget.Toast
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.FrameLayout
import android.view.KeyEvent
import xyz.xiao6.myboard.store.SettingsStore
import xyz.xiao6.myboard.model.DictionarySpec
import android.graphics.Color
import xyz.xiao6.myboard.ui.theme.ThemeRuntime
import xyz.xiao6.myboard.model.GestureType
import xyz.xiao6.myboard.model.KeyIds
import kotlin.math.roundToInt
import java.util.Locale.ROOT
import xyz.xiao6.myboard.util.PinyinSyllableSegmenter
import xyz.xiao6.myboard.ui.clipboard.ClipboardLayoutView
import xyz.xiao6.myboard.composer.ComposerRegistry
import xyz.xiao6.myboard.suggest.SuggestionCandidate
import xyz.xiao6.myboard.suggest.SuggestionContext
import xyz.xiao6.myboard.suggest.SuggestionManager
import xyz.xiao6.myboard.suggest.SuggestionSource
import xyz.xiao6.myboard.ui.popup.CandidateActionPopup
import xyz.xiao6.myboard.ui.ImePickerActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class MyBoardImeService : InputMethodService() {
    private val logTag = "ImeService"

    private var isCandidatePageExpanded: Boolean = false
    private var lastCandidates: List<String> = emptyList()
    private var lastComposing: String = ""
    private var lastComposingOptions: List<String> = emptyList()
    private var candidatePageSelectedPinyinIndex: Int = 0
    private var candidatePagePreviewCandidates: List<String>? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var rootView: View? = null
    private var controller: InputMethodController? = null
    private var runtimeDicts: RuntimeDictionaryManager? = null
    private var decoderFactory: DecoderFactory? = null
    private var subtypeManager: SubtypeManager? = null
    private var prefs: SettingsStore? = null
    private var themeSpec: ThemeSpec? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingClearInputRunnable: Runnable? = null
    private var pendingClearInputSeq: Long = 0L
    private var toolbarView: ToolbarView? = null
    private var prefsListener: SettingsStore.ChangeListener? = null
    private var toolbarSpec: ToolbarSpec? = null
    private var activeLocaleTag: String? = null
    private var decoderBuildJob: Job? = null
    private var composingPopup: FloatingComposingPopup? = null
    private var wordPreviewPopup: FloatingTextPreviewPopup? = null
    private var symbolsView: SymbolsLayoutView? = null
    private var emojiView: EmojiLayoutView? = null
    private var clipboardView: ClipboardLayoutView? = null
    private var resizeOverlay: KeyboardResizeOverlayView? = null
    private var resizeBaselineWidthDpOffset: Float? = null
    private var resizeBaselineHeightDpOffset: Float? = null
    private var topBarSlotView: View? = null
    private var popupMarginPx: Int = 0
    private var currentEditorInfo: EditorInfo? = null
    private var clipboardManager: ClipboardManager? = null
    private var clipboardListener: ClipboardManager.OnPrimaryClipChangedListener? = null
    private val clipboardEntries: ArrayDeque<ClipboardLayoutView.ClipboardEntry> = ArrayDeque()
    private var clipboardEntryIdSeed: Long = 0
    private var isEditingComposing: Boolean = false
    private var composingEditBuffer: String = ""
    private var composingEditCursor: Int = 0
    private var suggestionManager: SuggestionManager? = null
    private var suggestionCandidates: List<SuggestionCandidate> = emptyList()
    private var suggestionByText: Map<String, SuggestionCandidate> = emptyMap()
    private var lastDecoderCandidates: List<String> = emptyList()
    private var lastCommittedWord: String? = null
    private var candidateActionPopup: CandidateActionPopup? = null
    private val commitLatenciesMs = ArrayList<Long>(2048)
    private var imePickerNotificationShown: Boolean = false

    override fun onCreate() {
        super.onCreate()
        MLog.d(logTag, "onCreate")
        decoderFactory = DecoderFactory(this)
        subtypeManager = SubtypeManager(this).loadAll()
        prefs = SettingsStore(this)
        suggestionManager = SuggestionManager(this, prefs!!)
        themeSpec = ThemeManager(this).loadAllFromAssets().getDefaultTheme()
        toolbarSpec = ToolbarManager(this).loadAllFromAssets().getDefaultToolbar()
        clipboardManager = getSystemService(ClipboardManager::class.java)
        registerClipboardListener()
        ensureImePickerChannel()
    }

    override fun onDestroy() {
        prefsListener?.let { listener -> prefs?.removeOnChangeListener(listener) }
        prefsListener = null
        super.onDestroy()
        composingPopup?.dismiss()
        wordPreviewPopup?.dismiss()
        candidateActionPopup?.dismiss()
        unregisterClipboardListener()
        scope.cancel()
    }

    override fun onCreateInputView(): View {
        MLog.d(logTag, "onCreateInputView")

        val view = LayoutInflater.from(this).inflate(R.layout.ime_view, null, false)
        rootView = view

        val imeRoot = view.findViewById<View>(R.id.imeRoot)
        val imePanel = view.findViewById<ImePanelView>(R.id.imePanel)
        val topBarSlot = view.findViewById<View>(R.id.topBarSlot)
        topBarSlotView = topBarSlot
        val toolbarDivider = view.findViewById<View>(R.id.toolbarDivider)
        val toolbarView = view.findViewById<ToolbarView>(R.id.toolbarView)
        this.toolbarView = toolbarView
        val keyboardView = view.findViewById<KeyboardSurfaceView>(R.id.keyboardView)
        val candidatePageView = view.findViewById<CandidatePageView>(R.id.candidatePageView)
        val layoutPickerView = view.findViewById<LayoutPickerView>(R.id.layoutPickerView)
        val symbolsView = view.findViewById<SymbolsLayoutView>(R.id.symbolsView)
        val emojiView = view.findViewById<EmojiLayoutView>(R.id.emojiView)
        val clipboardView = view.findViewById<ClipboardLayoutView>(R.id.clipboardView)
        val resizeOverlay = view.findViewById<KeyboardResizeOverlayView>(R.id.resizeOverlay)
        val popupHost = view.findViewById<FrameLayout>(R.id.popupHost)
        val popupView = PopupView(popupHost).apply { applyTheme(themeSpec) }
        keyboardView.setPopupView(popupView)
        keyboardView.setTheme(themeSpec)
        toolbarView.applyTheme(themeSpec)
        applyImePanelTheme(imePanel, themeSpec)
        applyToolbarDividerTheme(toolbarDivider, themeSpec)
        candidatePageView.applyTheme(themeSpec)
        layoutPickerView.applyTheme(themeSpec)
        symbolsView.applyTheme(themeSpec)
        this.symbolsView = symbolsView
        emojiView.applyTheme(themeSpec)
        this.emojiView = emojiView
        clipboardView.applyTheme(themeSpec)
        this.clipboardView = clipboardView
        resizeOverlay.target = imePanel
        resizeOverlay.visibility = GONE
        this.resizeOverlay = resizeOverlay

        val marginPx = (resources.displayMetrics.density * 8f).toInt()
        popupMarginPx = marginPx
        val composingPopup =
            FloatingComposingPopup(this).apply {
                applyTheme(themeSpec)
                onClick = { toggleComposingEditMode() }
                onCursorMove = { index ->
                    if (isEditingComposing) {
                        composingEditCursor = index
                        updateComposingPopup()
                    }
                }
            }
        val wordPreviewPopup = FloatingTextPreviewPopup(this).apply { applyTheme(themeSpec) }
        this.composingPopup = composingPopup
        this.wordPreviewPopup = wordPreviewPopup
        val candidateActionPopup =
            CandidateActionPopup(this).apply {
                onBlock = { text -> handleSuggestionBlock(text) }
                onDemote = { text -> handleSuggestionDemote(text) }
            }
        this.candidateActionPopup = candidateActionPopup

        val layoutManager = LayoutManager(this).loadAllFromAssets()
        val dictionaryManager = DictionaryManager(this).loadAll()

        fun hideOverlays() {
            isCandidatePageExpanded = false
            candidatePageView.visibility = GONE
            layoutPickerView.visibility = GONE
            emojiView.visibility = GONE
            clipboardView.visibility = GONE
            clipboardView.clearSelection()
            resizeOverlay.visibility = GONE
            toolbarView.clearCandidates()
            wordPreviewPopup.dismiss()
            candidateActionPopup?.dismiss()
        }

        fun showSymbols() {
            hideOverlays()
            composingPopup.dismiss()
            toolbarView.visibility = View.INVISIBLE
            keyboardView.visibility = View.INVISIBLE
            symbolsView.visibility = VISIBLE
        }

        fun hideSymbols() {
            if (symbolsView.visibility != VISIBLE) return
            symbolsView.visibility = GONE
            toolbarView.visibility = VISIBLE
            keyboardView.visibility = VISIBLE
            renderCandidatesUi(
                candidates = lastCandidates,
                composing = lastComposing,
                composingOptions = lastComposingOptions,
                toolbarView = toolbarView,
                keyboardView = keyboardView,
                candidatePageView = candidatePageView,
            )
        }

        fun showEmoji() {
            hideOverlays()
            composingPopup.dismiss()
            toolbarView.visibility = View.INVISIBLE
            keyboardView.visibility = View.INVISIBLE
            symbolsView.visibility = GONE
            emojiView.visibility = VISIBLE
        }

        fun hideEmoji() {
            if (emojiView.visibility != VISIBLE) return
            emojiView.visibility = GONE
            toolbarView.visibility = VISIBLE
            keyboardView.visibility = VISIBLE
            renderCandidatesUi(
                candidates = lastCandidates,
                composing = lastComposing,
                composingOptions = lastComposingOptions,
                toolbarView = toolbarView,
                keyboardView = keyboardView,
                candidatePageView = candidatePageView,
            )
        }

        fun showClipboard(selectionMode: Boolean) {
            hideOverlays()
            composingPopup.dismiss()
            toolbarView.visibility = View.INVISIBLE
            keyboardView.visibility = View.INVISIBLE
            symbolsView.visibility = GONE
            emojiView.visibility = GONE
            clipboardView.visibility = VISIBLE
            clipboardView.submitItems(clipboardEntries.toList())
            clipboardView.setSelectionMode(selectionMode)
        }

        fun hideClipboard() {
            if (clipboardView.visibility != VISIBLE) return
            clipboardView.visibility = GONE
            clipboardView.clearSelection()
            toolbarView.visibility = VISIBLE
            keyboardView.visibility = VISIBLE
            renderCandidatesUi(
                candidates = lastCandidates,
                composing = lastComposing,
                composingOptions = lastComposingOptions,
                toolbarView = toolbarView,
                keyboardView = keyboardView,
                candidatePageView = candidatePageView,
            )
        }

        fun showResize() {
            // Only applies to the main keyboard panel; close other overlays.
            symbolsView.visibility = GONE
            emojiView.visibility = GONE
            clipboardView.visibility = GONE
            isCandidatePageExpanded = false
            candidatePageView.visibility = GONE
            layoutPickerView.visibility = GONE
            toolbarView.clearCandidates()
            wordPreviewPopup.dismiss()

            // Snapshot baseline so "Reset" returns to the state before this resize session.
            val p = prefs
            val layout = controller?.currentLayout?.value
            if (p != null && layout != null) {
                // Ensure global ratio baseline is set once and stays consistent across toolbar/settings.
                if (p.globalKeyboardWidthRatio == null) p.globalKeyboardWidthRatio = layout.totalWidthRatio
                if (p.globalKeyboardHeightRatio == null) p.globalKeyboardHeightRatio = layout.totalHeightRatio
                resizeBaselineWidthDpOffset = p.globalKeyboardWidthDpOffset ?: layout.totalWidthDpOffset
                resizeBaselineHeightDpOffset = p.globalKeyboardHeightDpOffset ?: layout.totalHeightDpOffset
            }

            resizeOverlay.visibility = VISIBLE
            showImeHint("拖动手柄可同时调整宽高（横向=宽度，纵向=高度）")
        }

        fun hideResize() {
            if (resizeOverlay.visibility != VISIBLE) return
            resizeOverlay.visibility = GONE
        }

        resizeOverlay.onDismiss = { hideResize() }
        resizeOverlay.onReset = fun() {
            val p = prefs ?: return
            val layout = controller?.currentLayout?.value ?: return
            val w = resizeBaselineWidthDpOffset ?: (p.globalKeyboardWidthDpOffset ?: layout.totalWidthDpOffset)
            val h = resizeBaselineHeightDpOffset ?: (p.globalKeyboardHeightDpOffset ?: layout.totalHeightDpOffset)
            p.globalKeyboardWidthDpOffset = w
            p.globalKeyboardHeightDpOffset = h
            val sized = applyGlobalKeyboardSize(layout)
            imePanel.applyKeyboardLayoutSize(sized)
        }
        resizeOverlay.onConfirm = fun() {
            hideResize()
        }
        resizeOverlay.onResizeDeltaPx = fun(deltaWidthPx: Int, deltaHeightPx: Int) {
            val p = prefs ?: return
            val layout = controller?.currentLayout?.value ?: return
            val density = resources.displayMetrics.density.coerceAtLeast(0.5f)
            val screenWidthPx = resources.displayMetrics.widthPixels.coerceAtLeast(1)
            val screenHeightPx = resources.displayMetrics.heightPixels.coerceAtLeast(1)
            val minWidthPx = KeyboardSizeConstraints.minKeyboardWidthPx(density)
            val minHeightPx = KeyboardSizeConstraints.minKeyboardHeightPx(density)
            val maxHeightPx = KeyboardSizeConstraints.maxKeyboardHeightPx(screenHeightPx, density).coerceAtLeast(minHeightPx)

            if (p.globalKeyboardWidthRatio == null) p.globalKeyboardWidthRatio = layout.totalWidthRatio
            if (p.globalKeyboardHeightRatio == null) p.globalKeyboardHeightRatio = layout.totalHeightRatio

            if (deltaWidthPx != 0) {
                val ratio = p.globalKeyboardWidthRatio ?: layout.totalWidthRatio
                val curOffDp = p.globalKeyboardWidthDpOffset ?: layout.totalWidthDpOffset
                val curPx = (screenWidthPx * ratio + curOffDp * density).toInt()
                val targetPx = (curPx + deltaWidthPx).coerceIn(minWidthPx, screenWidthPx)
                val nextOffDp = (targetPx - screenWidthPx * ratio) / density
                p.globalKeyboardWidthDpOffset = nextOffDp
            }
            if (deltaHeightPx != 0) {
                val ratio = p.globalKeyboardHeightRatio ?: layout.totalHeightRatio
                val curOffDp = p.globalKeyboardHeightDpOffset ?: layout.totalHeightDpOffset
                val curPx = (screenHeightPx * ratio + curOffDp * density).toInt()
                val targetPx = (curPx + deltaHeightPx).coerceIn(minHeightPx, maxHeightPx)
                val nextOffDp = (targetPx - screenHeightPx * ratio) / density
                p.globalKeyboardHeightDpOffset = nextOffDp
            }

            val sized = applyGlobalKeyboardSize(layout)
            imePanel.applyKeyboardLayoutSize(sized)
        }

        symbolsView.onBack = { hideSymbols() }
        symbolsView.onCommitSymbol = { symbol ->
            playKeyFeedback(keyboardView)
            commitTextToEditor(symbol)
            if (!symbolsView.isLocked()) hideSymbols()
        }

        emojiView.onBack = { hideEmoji() }
        emojiView.onCommit = { text ->
            playKeyFeedback(keyboardView)
            commitTextToEditor(text)
        }

        clipboardView.onBack = { hideClipboard() }
        clipboardView.onCommit = { entry ->
            playKeyFeedback(keyboardView)
            commitTextToEditor(entry.text)
        }
        clipboardView.onClearAll = {
            clipboardEntries.clear()
            clipboardView.submitItems(emptyList())
            clipboardView.setSelectionMode(false)
        }
        clipboardView.onDeleteSelected = onDeleteSelected@{ selectedIds ->
            if (selectedIds.isEmpty()) return@onDeleteSelected
            val remaining = clipboardEntries.filterNot { it.id in selectedIds }
            clipboardEntries.clear()
            clipboardEntries.addAll(remaining)
            clipboardView.submitItems(clipboardEntries.toList())
            clipboardView.clearSelection()
        }

        // Ensure toolbar is visible and has actions.
        registerPrefsListener()
        updateToolbarFromPrefs()
        updateBenchmarkFlags()
        toolbarView.onItemClick = { item ->
            when (item.itemId) {
                "layout" -> {
                    showLayoutPicker(
                        layoutManager = layoutManager,
                        toolbarView = toolbarView,
                        keyboardView = keyboardView,
                        candidatePageView = candidatePageView,
                        layoutPickerView = layoutPickerView,
                    )
                }
                "emoji" -> {
                    showEmoji()
                }
                "clipboard" -> {
                    showClipboard(selectionMode = false)
                }
                "kb_resize" -> {
                    showResize()
                }
                "settings" -> {
                    val intent = Intent(this, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
                else -> Toast.makeText(this, "toolbar: ${item.itemId}", Toast.LENGTH_SHORT).show()
            }
        }
        toolbarView.onItemLongClick = { item ->
            when (item.itemId) {
                "clipboard" -> {
                    showClipboard(selectionMode = true)
                }
            }
        }
        toolbarView.onOverflowClick = {
            // Priority:
            // 1) If symbols panel is open -> close it.
            // 2) If there are candidates -> toggle expanded candidate page.
            // 3) Otherwise -> collapse/hide the IME.
            when {
                symbolsView.visibility == VISIBLE -> hideSymbols()
                emojiView.visibility == VISIBLE -> hideEmoji()
                clipboardView.visibility == VISIBLE -> hideClipboard()
                resizeOverlay.visibility == VISIBLE -> hideResize()
                lastCandidates.isNotEmpty() -> {
                    isCandidatePageExpanded = !isCandidatePageExpanded
                    renderCandidatesUi(
                        candidates = lastCandidates,
                        composing = lastComposing,
                        composingOptions = lastComposingOptions,
                        toolbarView = toolbarView,
                        keyboardView = keyboardView,
                        candidatePageView = candidatePageView,
                    )
                }
                else -> requestHideSelf(0)
            }
        }
        toolbarView.onOverflowLongClick = {
            val intent = Intent(this, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }

        toolbarView.visibility = VISIBLE
        toolbarView.onCandidateClick = { text ->
            playKeyFeedback(keyboardView)
            handleSuggestionCommit(text)
            isCandidatePageExpanded = false
            isEditingComposing = false
            composingEditCursor = 0
        }
        toolbarView.onCandidateLongPress = { anchor, text ->
            showCandidateActionPopup(anchor, text)
        }
        candidatePageView.onCandidateClick = { text ->
            playKeyFeedback(keyboardView)
            handleSuggestionCommit(text)
            isCandidatePageExpanded = false
            candidatePageView.visibility = GONE
            keyboardView.visibility = VISIBLE
            wordPreviewPopup.dismiss()
            isEditingComposing = false
            composingEditCursor = 0
        }
        candidatePageView.onPinyinSelected = { index ->
            candidatePageSelectedPinyinIndex = index
            val specs = runtimeDicts?.activeList?.value.orEmpty()
            val factory = decoderFactory
            val left = lastComposingOptions.takeIf { it.isNotEmpty() } ?: splitPinyinSegments(lastComposing)
            val seg = left.getOrNull(index).orEmpty()
            val key = xyz.xiao6.myboard.decoder.normalizePinyinKey(seg)
            candidatePagePreviewCandidates =
                if (factory == null || key.isBlank()) null
                else factory.candidatesByPrefix(specs, key, limit = 200)

            if (candidatePageView.visibility == VISIBLE) {
                candidatePageView.submitPinyinSegments(left, selectedIndex = candidatePageSelectedPinyinIndex)
                candidatePageView.submitCandidates(candidatePagePreviewCandidates ?: lastCandidates)
            }
        }
        candidatePageView.onCandidateLongPress = { anchor, text ->
            wordPreviewPopup.showAbove(anchor, text, marginPx = marginPx)
        }
        candidatePageView.onCandidatePreviewDismiss = {
            wordPreviewPopup.dismiss()
        }
        candidatePageView.onBack = {
            isCandidatePageExpanded = false
            candidatePageView.visibility = GONE
            keyboardView.visibility = VISIBLE
            wordPreviewPopup.dismiss()
            renderCandidatesUi(
                candidates = lastCandidates,
                composing = lastComposing,
                composingOptions = lastComposingOptions,
                toolbarView = toolbarView,
                keyboardView = keyboardView,
                candidatePageView = candidatePageView,
            )
        }
        candidatePageView.onBackspace = {
            commitTextToEditor("\b")
            clearSuggestionsAfterDelete(keepToolbarHidden = true)
        }
        candidatePageView.onRetype = {
            controller?.resetComposing()
        }

        val dicts = RuntimeDictionaryManager(dictionaryManager = dictionaryManager)
        runtimeDicts = dicts

        val c = InputMethodController(
            layoutManager = layoutManager,
            scope = scope,
        ).apply {
            onCommitText = { text -> commitTextToEditor(text) }
            onSwitchLocale = { localeTag -> onLocaleSwitched(localeTag) }
            onToggleLocale = { toggleLocale() }
            onShowSymbols = { showSymbols() }
            onCommitLatencyMs = { ms ->
                commitLatenciesMs.add(ms)
                if (commitLatenciesMs.size % 100 == 0) {
                    logCommitLatencyStats()
                }
            }
        }
        controller = c
        c.attach(keyboardView)
        keyboardView.onTrigger = onTrigger@{ keyId, trigger ->
            if (handleComposingEditKey(keyId, trigger, keyboardView)) return@onTrigger
            if (!(keyId == KeyIds.BACKSPACE && trigger == GestureType.LONG_PRESS)) {
                cancelPendingClearInput()
            }
            playKeyFeedback(keyboardView)
            if (keyId == KeyIds.BACKSPACE && trigger == GestureType.LONG_PRESS) {
                handleBackspaceLongPress()
                return@onTrigger
            }
            c.onKeyTriggered(keyId, trigger)
            if (keyId == KeyIds.BACKSPACE && trigger == GestureType.TAP) {
                clearSuggestionsAfterDelete(keepToolbarHidden = false)
            }
        }
        keyboardView.onAction = { action ->
            cancelPendingClearInput()
            playKeyFeedback(keyboardView)
            c.onAction(action)
        }

        c.candidates
            .onEach { list ->
                lastDecoderCandidates = list
                lastComposing = c.composingText.value
                lastComposingOptions = c.composingOptions.value
                updateSuggestions(
                    composing = lastComposing,
                    decoderCandidates = lastDecoderCandidates,
                    toolbarView = toolbarView,
                    keyboardView = keyboardView,
                    candidatePageView = candidatePageView,
                )

                val (raw, display) = buildComposingPopupText(c.composingText.value)
                composingPopup.updateAbove(
                    anchor = topBarSlot,
                    composing = raw,
                    displayText = display,
                    xMarginPx = 0,
                    yMarginPx = 0,
                    editing = isEditingComposing,
                    cursorIndex = composingEditCursor,
                )
            }
            .launchIn(scope)

        c.composingText
            .onEach { composing ->
                if (composing.isBlank()) {
                    isCandidatePageExpanded = false
                    isEditingComposing = false
                    composingEditCursor = 0
                }
                if (composing != lastComposing) {
                    candidatePageSelectedPinyinIndex = 0
                    candidatePagePreviewCandidates = null
                }
                lastComposing = composing
                if (isEditingComposing) {
                    composingEditBuffer = composing
                    val count = composingEditBuffer.codePointCount(0, composingEditBuffer.length)
                    composingEditCursor = composingEditCursor.coerceIn(0, count)
                }
                if (composing.isBlank()) {
                    updateSuggestions(
                        composing = composing,
                        decoderCandidates = lastDecoderCandidates,
                        toolbarView = toolbarView,
                        keyboardView = keyboardView,
                        candidatePageView = candidatePageView,
                    )
                } else {
                    updateSuggestions(
                        composing = composing,
                        decoderCandidates = lastDecoderCandidates,
                        toolbarView = toolbarView,
                        keyboardView = keyboardView,
                        candidatePageView = candidatePageView,
                    )
                }
                val (raw, display) = buildComposingPopupText(composing)
                composingPopup.updateAbove(
                    anchor = topBarSlot,
                    composing = raw,
                    displayText = display,
                    xMarginPx = 0,
                    yMarginPx = 0,
                    editing = isEditingComposing,
                    cursorIndex = composingEditCursor,
                )
            }
            .launchIn(scope)

        c.composingOptions
            .onEach { options ->
                lastComposingOptions = options
                candidatePageSelectedPinyinIndex = 0
                candidatePagePreviewCandidates = null
                if (isCandidatePageExpanded && lastCandidates.isNotEmpty()) {
                    renderCandidatesUi(
                        candidates = lastCandidates,
                        composing = lastComposing,
                        composingOptions = lastComposingOptions,
                        toolbarView = toolbarView,
                        keyboardView = keyboardView,
                        candidatePageView = candidatePageView,
                    )
                }
            }
            .launchIn(scope)

        // Keep RuntimeDictionaryManager in sync with layout switches.
        c.currentLayout
            .onEach { layout ->
                val resolved = layout ?: return@onEach
                val layoutId = resolved.layoutId
                val sized = applyGlobalKeyboardSize(resolved)
                imePanel.applyKeyboardLayoutSize(sized)
                dicts.setLayoutId(layoutId)
                activeLocaleTag?.let { tag -> prefs?.setPreferredLayoutId(tag, layoutId) }
            }
            .launchIn(scope)

        // Swap decoder whenever the active dictionary changes.
        dicts.activeList
            .onEach { specs -> swapDecoderAsync(c, specs) }
            .launchIn(scope)

        // Initial locale/layout selection will be done in onStartInputView.
        return view
    }

    private fun applyGlobalKeyboardSize(layout: xyz.xiao6.myboard.model.KeyboardLayout): xyz.xiao6.myboard.model.KeyboardLayout {
        val p = prefs ?: return layout

        // Ensure the global size is fully initialized (ratio + offset) without overwriting any existing value.
        val wRatio = p.globalKeyboardWidthRatio ?: layout.totalWidthRatio.also { p.globalKeyboardWidthRatio = it }
        val hRatio = p.globalKeyboardHeightRatio ?: layout.totalHeightRatio.also { p.globalKeyboardHeightRatio = it }
        val wOff = p.globalKeyboardWidthDpOffset ?: layout.totalWidthDpOffset.also { p.globalKeyboardWidthDpOffset = it }
        val hOff = p.globalKeyboardHeightDpOffset ?: layout.totalHeightDpOffset.also { p.globalKeyboardHeightDpOffset = it }

        return layout.copy(
            totalWidthRatio = wRatio,
            totalWidthDpOffset = wOff,
            totalHeightRatio = hRatio,
            totalHeightDpOffset = hOff,
        )
    }

    private fun toggleLocale() {
        val sm = subtypeManager ?: return
        val profiles = enabledLocaleProfiles(sm)
        if (profiles.isEmpty()) return
        if (profiles.size <= 1) {
            showImeHint("仅启用了一个语言；请在设置中启用英文等其它语言后再切换")
            return
        }

        val currentRaw =
            activeLocaleTag
                ?: prefs?.userLocaleTag
                ?: (resources.configuration.locales[0] ?: Locale.getDefault()).toLanguageTag()
        val current = normalizeLocaleTag(currentRaw)

        val tags = profiles.map { it.localeTag }
        val zh = tags.firstOrNull { it.startsWith("zh", ignoreCase = true) }
        val en = tags.firstOrNull { it.startsWith("en", ignoreCase = true) }

        val next =
            when {
                current.startsWith("zh", ignoreCase = true) && !en.isNullOrBlank() -> en
                current.startsWith("en", ignoreCase = true) && !zh.isNullOrBlank() -> zh
                else -> {
                    val idx = tags.indexOfFirst { normalizeLocaleTag(it) == current }
                    if (idx >= 0) tags[(idx + 1) % tags.size] else tags.first()
                }
            }

        switchLocaleWithLayoutPolicy(next)
    }

    private fun showImeHint(text: String) {
        val popup = wordPreviewPopup
        val anchor =
            topBarSlotView
                ?: rootView?.findViewById(R.id.topBarSlot)
        if (popup == null || anchor == null) {
            Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT).show()
            return
        }
        val margin = popupMarginPx.takeIf { it > 0 } ?: (resources.displayMetrics.density * 8f).toInt()
        popup.showAbove(anchor, text, marginPx = margin)
        anchor.removeCallbacks(dismissImeHintRunnable)
        anchor.postDelayed(dismissImeHintRunnable, 1600L)
    }

    private fun applyToolbarDividerTheme(divider: View?, theme: ThemeSpec?) {
        if (divider == null) return
        val extend = theme?.layout?.extendToToolbar == true
        if (extend) {
            divider.visibility = GONE
            return
        }
        val runtime = theme?.let { ThemeRuntime(it) }
        val fallback = Color.parseColor("#14000000")
        val color =
            runtime?.resolveColor(theme?.candidates?.divider?.color ?: theme?.toolbar?.surface?.stroke?.color, fallback)
                ?: fallback
        divider.setBackgroundColor(color)
        divider.visibility = VISIBLE
    }

    private fun applyImePanelTheme(panel: View?, theme: ThemeSpec?) {
        if (panel == null) return
        val extend = theme?.layout?.extendToToolbar == true
        if (!extend) {
            panel.setBackgroundColor(Color.TRANSPARENT)
            return
        }
        val runtime = theme?.let { ThemeRuntime(it) }
        val fallback = Color.parseColor("#F2F2F7")
        val color =
            runtime?.resolveColor(theme?.layout?.background?.color ?: theme?.colors?.get("background"), fallback)
                ?: fallback
        panel.setBackgroundColor(color)
    }

    private val dismissImeHintRunnable = Runnable {
        wordPreviewPopup?.dismiss()
    }

    private fun showLayoutPicker(
        layoutManager: LayoutManager,
        toolbarView: ToolbarView,
        keyboardView: KeyboardSurfaceView,
        candidatePageView: CandidatePageView,
        layoutPickerView: LayoutPickerView,
    ) {
        // Close any overlays which conflict with the picker.
        symbolsView?.visibility = GONE
        emojiView?.visibility = GONE
        clipboardView?.visibility = GONE
        isCandidatePageExpanded = false
        candidatePageView.visibility = GONE
        toolbarView.clearCandidates()
        wordPreviewPopup?.dismiss()

        val sections = buildLayoutPickerSections(layoutManager)
        if (sections.isEmpty()) return

        keyboardView.visibility = View.INVISIBLE
        layoutPickerView.visibility = VISIBLE
        layoutPickerView.submitSections(sections)
        layoutPickerView.onDismiss = {
            layoutPickerView.visibility = GONE
            keyboardView.visibility = VISIBLE
        }
        layoutPickerView.onLayoutSelected = { localeTag, selectedId ->
            layoutPickerView.visibility = GONE
            keyboardView.visibility = VISIBLE
            onLocaleAndLayoutSelected(localeTag, selectedId)
        }
    }

    private fun normalizeLocaleTag(tag: String): String {
        val t = tag.trim().replace('_', '-')
        val parts = t.split('-').filter { it.isNotBlank() }
        if (parts.isEmpty()) return ""
        val language = parts[0].lowercase(ROOT)
        val region = parts.getOrNull(1)?.uppercase(ROOT)
        return if (region.isNullOrBlank()) language else "$language-$region"
    }

    private fun buildToolbarItems(spec: ToolbarSpec?): List<ToolbarView.Item> {
        val items = spec?.items.orEmpty()
            .filter { it.enabled }
            .sortedWith(compareByDescending<xyz.xiao6.myboard.model.ToolbarItemSpec> { it.priority }.thenBy { it.itemId })
        if (items.isEmpty()) {
            return listOf(
                ToolbarView.Item("layout", R.drawable.ic_toolbar_layout, "Layout"),
                ToolbarView.Item("voice", R.drawable.ic_toolbar_voice, "Voice"),
                ToolbarView.Item("emoji", R.drawable.ic_toolbar_emoji, "Emoji"),
                ToolbarView.Item("clipboard", R.drawable.ic_toolbar_clipboard, "Clipboard"),
                ToolbarView.Item("kb_resize", android.R.drawable.ic_menu_crop, "Resize"),
                ToolbarView.Item("settings", R.drawable.ic_toolbar_settings, "Settings"),
            )
        }

        fun iconResId(icon: String): Int {
            return when (icon.lowercase()) {
                "layout" -> R.drawable.ic_toolbar_layout
                "voice" -> R.drawable.ic_toolbar_voice
                "emoji" -> R.drawable.ic_toolbar_emoji
                "clipboard" -> R.drawable.ic_toolbar_clipboard
                "kb_resize" -> android.R.drawable.ic_menu_crop
                "settings" -> R.drawable.ic_toolbar_settings
                else -> R.drawable.ic_toolbar_settings
            }
        }

        val ordered = applyToolbarOrder(items, prefs?.toolbarItemOrder.orEmpty())
        return ordered.map { ToolbarView.Item(it.itemId, iconResId(it.icon), it.name) }
    }

    private fun applyToolbarOrder(
        items: List<xyz.xiao6.myboard.model.ToolbarItemSpec>,
        order: List<String>,
    ): List<xyz.xiao6.myboard.model.ToolbarItemSpec> {
        if (order.isEmpty()) return items
        val byId = items.associateBy { it.itemId }
        val ordered = order.mapNotNull { byId[it] }
        val orderSet = order.toSet()
        val remaining =
            items.filterNot { it.itemId in orderSet }
                .sortedWith(compareByDescending<xyz.xiao6.myboard.model.ToolbarItemSpec> { it.priority }.thenBy { it.itemId })
        return ordered + remaining
    }

    private fun enabledLayoutsFor(profile: xyz.xiao6.myboard.model.LocaleLayoutProfile): List<String> {
        val tag = profile.localeTag
        val enabled = prefs?.getEnabledLayoutIds(tag).orEmpty()
        val fromPrefs = enabled.filter { it in profile.layoutIds }.distinct()
        return if (fromPrefs.isNotEmpty()) fromPrefs else profile.layoutIds.distinct()
    }

    private fun enabledLocaleProfiles(sm: SubtypeManager): List<xyz.xiao6.myboard.model.LocaleLayoutProfile> {
        val all =
            sm.listAll()
                .filter { it.enabled }
                .sortedWith(compareByDescending<xyz.xiao6.myboard.model.LocaleLayoutProfile> { it.priority }.thenBy { it.localeTag })
        val enabledTags = prefs?.getEnabledLocaleTags().orEmpty()
        if (enabledTags.isEmpty()) return all
        val filtered = all.filter { it.localeTag in enabledTags }
        return if (filtered.isNotEmpty()) filtered else all
    }

    private fun formatLocaleLabel(localeTag: String): String {
        val tag = localeTag.trim().replace('_', '-')
        val display = Locale.forLanguageTag(tag).getDisplayName(Locale.getDefault()).ifBlank { tag }
        return "$display ($tag)"
    }

    private fun buildLayoutPickerSections(layoutManager: LayoutManager): List<LayoutPickerView.LocaleSection> {
        val sm = subtypeManager ?: return emptyList()
        val profiles = enabledLocaleProfiles(sm)
        if (profiles.isEmpty()) return emptyList()

        layoutManager.loadAllFromAssets()
        val currentLocale = activeLocaleTag ?: prefs?.userLocaleTag
        val currentLayoutId = controller?.currentLayout?.value?.layoutId

        return profiles.mapNotNull { profile ->
            val enabledLayoutIds = enabledLayoutsFor(profile)
            if (enabledLayoutIds.isEmpty()) return@mapNotNull null

            val options =
                enabledLayoutIds.mapNotNull { id ->
                    val spec = runCatching { layoutManager.getLayout(id) }.getOrNull() ?: return@mapNotNull null
                    val selected = normalizeLocaleTag(profile.localeTag) == normalizeLocaleTag(currentLocale.orEmpty()) && id == currentLayoutId
                    LayoutPickerView.LayoutOption(
                        localeTag = profile.localeTag,
                        layoutId = id,
                        name = spec.name?.ifBlank { id } ?: id,
                        selected = selected,
                        layout = spec,
                    )
                }
            if (options.isEmpty()) return@mapNotNull null
            LayoutPickerView.LocaleSection(
                localeTag = profile.localeTag,
                label = formatLocaleLabel(profile.localeTag),
                options = options,
            )
        }
    }

    private fun onLocaleAndLayoutSelected(localeTag: String, layoutId: String) {
        val normalized = normalizeLocaleTag(localeTag)
        val nextLocale = Locale.forLanguageTag(normalized)

        val profile = subtypeManager?.get(normalized) ?: subtypeManager?.resolve(nextLocale) ?: return
        val enabledLayouts = enabledLayoutsFor(profile)
        val targetLayoutId = if (layoutId in enabledLayouts) layoutId else enabledLayouts.firstOrNull() ?: return

        val enabledTags = prefs?.getEnabledLocaleTags().orEmpty()
        if (enabledTags.isNotEmpty() && normalized !in enabledTags) {
            prefs?.setEnabledLocaleTags((enabledTags + normalized).distinct())
        }

        prefs?.userLocaleTag = normalized
        activeLocaleTag = normalized
        runtimeDicts?.setLocale(nextLocale)
        updateRuntimeDictionariesFromPrefs()
        prefs?.setPreferredLayoutId(normalized, targetLayoutId)

        if (controller?.currentLayout?.value?.layoutId != targetLayoutId) {
            controller?.loadLayout(targetLayoutId)
        }
        MLog.d(logTag, "IME loadLayout onLocaleAndLayoutSelected layoutId=$targetLayoutId locale=$normalized")
        applyLocaleSymbolOverrides(normalized)
    }

    private fun switchToNextEnabledLayout(profile: xyz.xiao6.myboard.model.LocaleLayoutProfile) {
        val list = enabledLayoutsFor(profile)
        if (list.isEmpty()) return
        val current = controller?.currentLayout?.value?.layoutId
        val idx = current?.let { list.indexOf(it) } ?: -1
        val next = if (idx < 0) list.first() else list[(idx + 1) % list.size]
        prefs?.setPreferredLayoutId(profile.localeTag, next)
        controller?.loadLayout(next)
        MLog.d(logTag, "IME loadLayout switchToNextEnabledLayout layoutId=$next locale=${profile.localeTag}")
        applyLocaleSymbolOverrides(profile.localeTag)
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        currentEditorInfo = info
        resetImeWindowState()

        val systemLocale = resources.configuration.locales[0] ?: Locale.getDefault()
        val enabledTags = prefs?.getEnabledLocaleTags().orEmpty()
        val preferredTagRaw = prefs?.userLocaleTag
        val preferredTag = preferredTagRaw?.takeIf { enabledTags.isEmpty() || it in enabledTags }
        val locale = preferredTag?.let { Locale.forLanguageTag(it) } ?: systemLocale
        MLog.d(
            logTag,
            "onStartInputView restarting=$restarting locale=${locale.toLanguageTag()} preferred=$preferredTag",
        )

        val profile = subtypeManager?.resolve(systemLocale, preferredLocaleTag = preferredTag)
        activeLocaleTag = profile?.localeTag ?: preferredTag ?: locale.toLanguageTag()
        applyComposerForLocale(activeLocaleTag ?: locale.toLanguageTag())
        val preferredLayoutId = activeLocaleTag?.let { prefs?.getPreferredLayoutId(it) }
        val enabledLayoutIds = activeLocaleTag?.let { tag -> prefs?.getEnabledLayoutIds(tag) }.orEmpty()
        val enabledLayoutIdsFiltered = if (profile == null) emptyList() else enabledLayoutIds.filter { it in profile.layoutIds }

        val layoutId =
            when {
                !preferredLayoutId.isNullOrBlank() &&
                    profile?.layoutIds?.contains(preferredLayoutId) == true &&
                    (enabledLayoutIdsFiltered.isEmpty() || preferredLayoutId in enabledLayoutIdsFiltered) ->
                    preferredLayoutId

                enabledLayoutIdsFiltered.isNotEmpty() -> enabledLayoutIdsFiltered.firstOrNull()
                !profile?.defaultLayoutId.isNullOrBlank() -> profile?.defaultLayoutId
                !profile?.layoutIds.isNullOrEmpty() -> profile?.layoutIds?.firstOrNull()
                else -> runCatching { LayoutManager(this).loadAllFromAssets().getDefaultLayout(locale).layoutId }.getOrNull()
            } ?: "qwerty"

        runtimeDicts?.setLocale(locale)
        runtimeDicts?.setLayoutId(layoutId)
        updateRuntimeDictionariesFromPrefs()

        controller?.loadLayout(layoutId)
        MLog.d(logTag, "IME loadLayout onStartInputView layoutId=$layoutId locale=${activeLocaleTag ?: locale.toLanguageTag()}")
        swapDecoderAsync(controller, runtimeDicts?.activeList?.value.orEmpty())
        applyLocaleSymbolOverrides(activeLocaleTag ?: locale.toLanguageTag())
    }

    private fun swapDecoderAsync(controller: InputMethodController?, specs: List<DictionarySpec>) {
        val c = controller ?: return
        val factory = decoderFactory ?: return
        decoderBuildJob?.cancel()
        decoderBuildJob =
            scope.launch(Dispatchers.Default) {
                val decoder = factory.create(specs)
                withContext(Dispatchers.Main.immediate) {
                    c.setDecoder(decoder)
                }
            }
    }

    private fun commitTextToEditor(text: String) {
        cancelPendingClearInput()
        val ic = currentInputConnection ?: return
        if (text == "\b") {
            ic.deleteSurroundingText(1, 0)
            return
        }
        if (text == "\n") {
            val info = currentEditorInfo
            val inputType = info?.inputType ?: 0
            val isMultiLine = (inputType and android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0
            if (isMultiLine) {
                ic.commitText("\n", 1)
                return
            }

            val actionFromOptions = info?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
            val actionId = info?.actionId ?: 0
            val performed =
                when {
                    actionId != 0 -> ic.performEditorAction(actionId)
                    actionFromOptions != EditorInfo.IME_ACTION_NONE -> ic.performEditorAction(actionFromOptions)
                    else -> false
                }
            if (!performed) {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            }
            return
        }
        ic.commitText(text, 1)
        recordCommittedText(text)
    }

    private fun recordCommittedText(text: String) {
        val tag = activeLocaleTag ?: Locale.getDefault().toLanguageTag()
        val manager = suggestionManager ?: return
        manager.onCommittedText(tag, text, lastCommittedWord)
        val words = extractTokens(text)
        if (words.isNotEmpty()) {
            lastCommittedWord = words.last()
        }
        if (lastComposing.isBlank()) {
            val toolbar = toolbarView
            val keyboard = rootView?.findViewById<KeyboardSurfaceView>(R.id.keyboardView)
            val candidatePage = rootView?.findViewById<CandidatePageView>(R.id.candidatePageView)
            if (toolbar != null && keyboard != null && candidatePage != null) {
                updateSuggestions(
                    composing = lastComposing,
                    decoderCandidates = lastDecoderCandidates,
                    toolbarView = toolbar,
                    keyboardView = keyboard,
                    candidatePageView = candidatePage,
                )
            }
        }
    }

    private fun extractTokens(text: String): List<String> {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return emptyList()
        val regex = Regex("[\\p{IsHan}]+|[\\p{L}\\p{N}]+")
        return regex.findAll(trimmed).map { it.value }.toList()
    }

    private fun handleBackspaceLongPress() {
        val composing = lastComposing
        if (composing.isBlank()) return
        controller?.resetComposing()
        scheduleClearInputAfterTokenClear()
    }

    private fun scheduleClearInputAfterTokenClear() {
        val p = prefs ?: return
        if (!p.clearInputAfterTokenClear) return
        val delayMs = p.clearInputAfterTokenClearDelayMs.toLong().coerceAtLeast(0L)
        val seq = ++pendingClearInputSeq
        pendingClearInputRunnable?.let { mainHandler.removeCallbacks(it) }
        val runnable = Runnable {
            if (seq != pendingClearInputSeq) return@Runnable
            clearEditorTextSafely()
        }
        pendingClearInputRunnable = runnable
        mainHandler.postDelayed(runnable, delayMs)
    }

    private fun cancelPendingClearInput() {
        pendingClearInputSeq++
        pendingClearInputRunnable?.let { mainHandler.removeCallbacks(it) }
        pendingClearInputRunnable = null
    }

    private fun clearEditorTextSafely() {
        val ic = currentInputConnection ?: return
        ic.beginBatchEdit()
        try {
            ic.finishComposingText()
            val extracted = ic.getExtractedText(ExtractedTextRequest(), 0)
            val text = extracted?.text
            if (text != null) {
                val total = text.length
                if (total > 0) {
                    val selectionOk = ic.setSelection(0, total)
                    if (selectionOk) {
                        ic.commitText("", 1)
                    } else {
                        val before = extracted.selectionStart.coerceAtLeast(0)
                        val after = (total - extracted.selectionEnd).coerceAtLeast(0)
                        ic.deleteSurroundingText(before, after)
                    }
                }
            } else {
                ic.deleteSurroundingText(1000, 1000)
            }
        } finally {
            ic.endBatchEdit()
        }
    }

    private fun playKeyFeedback(view: View) {
        val p = prefs ?: return

        val clickVolume = (p.clickSoundVolumePercent.coerceIn(0, 100) / 100f)
        if (clickVolume > 0f) {
            val am = getSystemService(AUDIO_SERVICE) as? AudioManager
            am?.playSoundEffect(AudioManager.FX_KEY_CLICK, clickVolume)
        }

        if (p.vibrationFollowSystem) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            return
        }

        val strength = (p.vibrationStrengthPercent.coerceIn(0, 100) / 100f)
        if (strength <= 0f) return
        vibrateOnce(strength)
    }

    private fun vibrateOnce(strength: Float) {
        val vibrator =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(VIBRATOR_SERVICE) as? Vibrator
            } ?: return

        if (!vibrator.hasVibrator()) return

        val durationMs = 12L
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val amplitude = (strength.coerceIn(0f, 1f) * 255f).roundToInt().coerceIn(1, 255)
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    }

    private fun onLocaleSwitched(localeTag: String) {
        switchLocaleWithLayoutPolicy(localeTag)
    }

    private fun switchLocaleWithLayoutPolicy(localeTag: String) {
        val normalized = localeTag.trim().replace('_', '-')
        val nextLocale = Locale.forLanguageTag(normalized)
        MLog.d(logTag, "SwitchLocale(policy) -> $normalized")

        val sm = subtypeManager ?: return
        val profile = sm.resolve(nextLocale) ?: return
        val currentLayoutId = controller?.currentLayout?.value?.layoutId.orEmpty().trim()

        // (AGENTS.md #9) If target language has the current layout: do not redraw the keyboard; only replace symbols.
        if (currentLayoutId.isNotBlank() && profile.layoutIds.contains(currentLayoutId)) {
            prefs?.userLocaleTag = normalized
            activeLocaleTag = normalized
            applyComposerForLocale(normalized)
            runtimeDicts?.setLocale(nextLocale)
            runtimeDicts?.setLayoutId(currentLayoutId)
            updateRuntimeDictionariesFromPrefs()
            prefs?.setPreferredLayoutId(profile.localeTag, currentLayoutId)
            applyLocaleSymbolOverrides(normalized)
            return
        }

        // Otherwise: choose a layout for the target language (user-enabled first, then language default).
        val preferredLayoutId = prefs?.getPreferredLayoutId(profile.localeTag)
        val enabledLayouts = enabledLayoutsFor(profile)
        val targetLayoutId =
            when {
                !preferredLayoutId.isNullOrBlank() && preferredLayoutId in enabledLayouts -> preferredLayoutId
                enabledLayouts.isNotEmpty() -> enabledLayouts.firstOrNull()
                !profile.defaultLayoutId.isNullOrBlank() -> profile.defaultLayoutId
                else -> profile.layoutIds.firstOrNull()
            } ?: return

        prefs?.userLocaleTag = normalized
        activeLocaleTag = normalized
        applyComposerForLocale(normalized)
        runtimeDicts?.setLocale(nextLocale)
        runtimeDicts?.setLayoutId(targetLayoutId)
        updateRuntimeDictionariesFromPrefs()
        prefs?.setPreferredLayoutId(profile.localeTag, targetLayoutId)

        if (controller?.currentLayout?.value?.layoutId != targetLayoutId) {
            controller?.loadLayout(targetLayoutId)
        }
        MLog.d(logTag, "IME loadLayout switchLocaleWithLayoutPolicy layoutId=$targetLayoutId locale=$normalized")
        applyLocaleSymbolOverrides(normalized)
    }

    private fun applyLocaleSymbolOverrides(localeTag: String) {
        val c = controller ?: return
        val layout = c.currentLayout.value
        val keys = layout?.rows?.flatMap { it.keys }.orEmpty()

        val currentNormalized = normalizeLocaleTag(localeTag)
        val nextToggle = computeNextLocaleTagForToggle(currentNormalized)
        val currentLabel = shortLocaleKeyLabel(currentNormalized)
        val nextLabel = nextToggle?.let { shortLocaleKeyLabel(it) }

        val labelOverrides = LinkedHashMap<String, String>()
        val hintOverrides = LinkedHashMap<String, Map<String, String>>()

        // Locale toggle key: best-effort by keyId.
        for (key in keys) {
            if (key.keyId != "key_lang_toggle") continue
            labelOverrides[key.keyId] = currentLabel
            hintOverrides[key.keyId] =
                if (!nextLabel.isNullOrBlank()) {
                    mapOf("BOTTOM_RIGHT" to "/$nextLabel")
                } else {
                    mapOf("BOTTOM_RIGHT" to "")
                }
        }

        applyLocaleCaseLabels(
            keys = keys,
            localeTag = currentNormalized,
            labelOverrides = labelOverrides,
        )
        applyLocaleCaseHints(
            keys = keys,
            localeTag = currentNormalized,
            hintOverrides = hintOverrides,
        )

        val invalidateIds = (labelOverrides.keys + hintOverrides.keys).toSet()
        c.updateState(
            reducer = { s -> s.copy(localeTag = currentNormalized, labelOverrides = labelOverrides, hintOverrides = hintOverrides) },
            invalidateKeyIds = invalidateIds,
        )
    }

    private fun applyComposerForLocale(localeTag: String) {
        val language = Locale.forLanguageTag(localeTag).language.lowercase()
        val composer =
            when (language) {
                "ja" -> ComposerRegistry.romajiHiragana
                else -> ComposerRegistry.appender
            }
        controller?.setComposer(composer)
    }

    private fun updateSuggestions(
        composing: String,
        decoderCandidates: List<String>,
        toolbarView: ToolbarView,
        keyboardView: KeyboardSurfaceView,
        candidatePageView: CandidatePageView,
    ) {
        if (prefs?.benchmarkDisableCandidates == true) {
            clearSuggestionState()
            renderCandidatesUi(
                candidates = emptyList(),
                composing = composing,
                composingOptions = lastComposingOptions,
                toolbarView = toolbarView,
                keyboardView = keyboardView,
                candidatePageView = candidatePageView,
            )
            return
        }
        val manager = suggestionManager
        val tag = activeLocaleTag ?: Locale.getDefault().toLanguageTag()
        val prefs = prefs
        val context =
            SuggestionContext(
                composingText = composing,
                decoderCandidates = decoderCandidates,
                lastCommittedWord = lastCommittedWord,
                localeTag = tag,
                suggestionEnabled = prefs?.suggestionEnabled ?: true,
                learningEnabled = prefs?.suggestionLearningEnabled ?: true,
            )
        suggestionCandidates =
            if (manager == null) {
                decoderCandidates.mapIndexed { idx, text ->
                    SuggestionCandidate(text = text, source = SuggestionSource.DECODER, score = 1.0 - idx * 0.001)
                }
            } else {
                manager.build(context, limit = 200)
            }
        suggestionByText = suggestionCandidates.associateBy { it.text }
        lastCandidates = suggestionCandidates.map { it.text }
        renderCandidatesUi(
            candidates = lastCandidates,
            composing = composing,
            composingOptions = lastComposingOptions,
            toolbarView = toolbarView,
            keyboardView = keyboardView,
            candidatePageView = candidatePageView,
        )
    }

    private fun clearSuggestionState() {
        lastCandidates = emptyList()
        suggestionCandidates = emptyList()
        suggestionByText = emptyMap()
        candidatePagePreviewCandidates = null
        isCandidatePageExpanded = false
    }

    private fun clearSuggestionsAfterDelete(keepToolbarHidden: Boolean) {
        if (lastComposing.isNotBlank() || lastCandidates.isEmpty()) return
        lastCommittedWord = null
        lastCandidates = emptyList()
        suggestionCandidates = emptyList()
        suggestionByText = emptyMap()
        candidatePagePreviewCandidates = null
        isCandidatePageExpanded = false

        val toolbar = toolbarView ?: return
        val keyboard = rootView?.findViewById<KeyboardSurfaceView>(R.id.keyboardView) ?: return
        val candidatePage = rootView?.findViewById<CandidatePageView>(R.id.candidatePageView) ?: return
        candidatePage.visibility = GONE
        toolbar.clearCandidates()
        toolbar.setItemsVisible(!keepToolbarHidden)
        keyboard.visibility = VISIBLE
        wordPreviewPopup?.dismiss()
    }

    private fun handleSuggestionCommit(text: String) {
        val candidate = suggestionByText[text]
        if (candidate == null || candidate.source == SuggestionSource.DECODER) {
            controller?.onCandidateSelected(text)
            return
        }
        val commit = candidate.commitText ?: candidate.text
        commitTextToEditor(commit)
        controller?.resetComposing()
    }

    private fun handleSuggestionBlock(text: String) {
        val tag = activeLocaleTag ?: Locale.getDefault().toLanguageTag()
        suggestionManager?.blockWord(tag, text)
        showImeHint(getString(R.string.settings_suggestions_blocked_hint))
        val toolbar = toolbarView
        val keyboard = rootView?.findViewById<KeyboardSurfaceView>(R.id.keyboardView)
        val candidatePage = rootView?.findViewById<CandidatePageView>(R.id.candidatePageView)
        if (toolbar != null && keyboard != null && candidatePage != null) {
            updateSuggestions(
                composing = lastComposing,
                decoderCandidates = lastDecoderCandidates,
                toolbarView = toolbar,
                keyboardView = keyboard,
                candidatePageView = candidatePage,
            )
        }
    }

    private fun handleSuggestionDemote(text: String) {
        val tag = activeLocaleTag ?: Locale.getDefault().toLanguageTag()
        suggestionManager?.demoteWord(tag, text)
        val toolbar = toolbarView
        val keyboard = rootView?.findViewById<KeyboardSurfaceView>(R.id.keyboardView)
        val candidatePage = rootView?.findViewById<CandidatePageView>(R.id.candidatePageView)
        if (toolbar != null && keyboard != null && candidatePage != null) {
            updateSuggestions(
                composing = lastComposing,
                decoderCandidates = lastDecoderCandidates,
                toolbarView = toolbar,
                keyboardView = keyboard,
                candidatePageView = candidatePage,
            )
        }
    }

    private fun showCandidateActionPopup(anchor: View, text: String) {
        val popup = candidateActionPopup ?: return
        val margin = popupMarginPx.takeIf { it > 0 } ?: (resources.displayMetrics.density * 8f).toInt()
        popup.showAbove(anchor, text, marginPx = margin)
    }

    private fun applyLocaleCaseHints(
        keys: List<xyz.xiao6.myboard.model.Key>,
        localeTag: String,
        hintOverrides: MutableMap<String, Map<String, String>>,
    ) {
        val normalized = normalizeLocaleTag(localeTag)
        for (key in keys) {
            if (key.hints.isEmpty()) continue
            val action = key.actions[GestureType.FLICK_UP] ?: continue
            if (action.actionType != xyz.xiao6.myboard.model.ActionType.COMMIT_TEXT) continue
            val hint =
                resolveLocaleCommitText(action, normalized)
                    ?: resolveDefaultCommitText(action)
            if (hint.isNullOrBlank()) continue
            hintOverrides[key.keyId] = mapOf("BOTTOM_CENTER" to hint)
        }
    }

    private fun applyLocaleCaseLabels(
        keys: List<xyz.xiao6.myboard.model.Key>,
        localeTag: String,
        labelOverrides: MutableMap<String, String>,
    ) {
        val normalized = normalizeLocaleTag(localeTag)
        for (key in keys) {
            val action = key.actions[GestureType.TAP] ?: continue
            if (action.actionType != xyz.xiao6.myboard.model.ActionType.COMMIT_TEXT) continue
            val label =
                resolveLocaleCommitText(action, normalized)
                    ?: resolveDefaultCommitText(action)
            if (label.isNullOrBlank()) continue
            labelOverrides[key.keyId] = label
        }
    }

    private fun resolveLocaleCommitText(
        action: xyz.xiao6.myboard.model.KeyAction,
        localeTag: String,
    ): String? {
        val matched = action.cases.firstOrNull { case ->
            val required = case.whenCondition?.locale?.trim().orEmpty()
            if (required.isBlank()) return@firstOrNull false
            val reqNorm = normalizeLocaleTag(required)
            if (reqNorm.isBlank()) return@firstOrNull false
            if (reqNorm.contains('-')) {
                reqNorm.equals(localeTag, ignoreCase = true)
            } else {
                localeTag.startsWith(reqNorm, ignoreCase = true)
            }
        } ?: return null
        val commit = matched.doActions.firstOrNull() as? xyz.xiao6.myboard.model.Action.CommitText ?: return null
        return commit.text
    }

    private fun resolveDefaultCommitText(action: xyz.xiao6.myboard.model.KeyAction): String? {
        val commit = action.defaultActions.firstOrNull() as? xyz.xiao6.myboard.model.Action.CommitText ?: return null
        return commit.text
    }

    private fun computeNextLocaleTagForToggle(currentNormalized: String): String? {
        val sm = subtypeManager ?: return null
        val profiles = enabledLocaleProfiles(sm)
        if (profiles.size <= 1) return null

        val tags = profiles.map { normalizeLocaleTag(it.localeTag) }.distinct()
        val zh = tags.firstOrNull { it.startsWith("zh", ignoreCase = true) }
        val en = tags.firstOrNull { it.startsWith("en", ignoreCase = true) }

        return when {
            currentNormalized.startsWith("zh", ignoreCase = true) && !en.isNullOrBlank() -> en
            currentNormalized.startsWith("en", ignoreCase = true) && !zh.isNullOrBlank() -> zh
            else -> {
                val idx = tags.indexOfFirst { it == currentNormalized }
                if (idx >= 0) tags[(idx + 1) % tags.size] else tags.firstOrNull()
            }
        }
    }

    private fun shortLocaleKeyLabel(normalizedLocaleTag: String): String {
        val parts = normalizedLocaleTag.split('-')
        val language = parts.firstOrNull().orEmpty().lowercase(ROOT)
        return when (language) {
            "zh" -> "中"
            "en" -> "EN"
            else -> language.take(2).uppercase(ROOT).ifBlank { "?" }
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        dismissImePickerNotification()
        resetImeWindowState()
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        dismissImePickerNotification()
        resetImeWindowState()
    }

    override fun onWindowShown() {
        super.onWindowShown()
        showImePickerNotificationIfNeeded()
    }

    private fun resetImeWindowState() {
        cancelPendingClearInput()
        isCandidatePageExpanded = false
        lastCandidates = emptyList()
        lastComposing = ""
        lastComposingOptions = emptyList()
        candidatePageSelectedPinyinIndex = 0
        candidatePagePreviewCandidates = null

        composingPopup?.dismiss()
        wordPreviewPopup?.dismiss()

        controller?.resetComposing()
        controller?.resetLayoutStateToDefault()

        symbolsView?.visibility = GONE
        emojiView?.visibility = GONE
        clipboardView?.visibility = GONE
        clipboardView?.clearSelection()
        resizeOverlay?.visibility = GONE

        rootView?.findViewById<xyz.xiao6.myboard.ui.toolbar.ToolbarView>(R.id.toolbarView)?.let { toolbar ->
            toolbar.visibility = VISIBLE
            toolbar.clearCandidates()
        }
        rootView?.findViewById<xyz.xiao6.myboard.ui.candidate.CandidatePageView>(R.id.candidatePageView)?.visibility = GONE
        rootView?.findViewById<xyz.xiao6.myboard.ui.layout.LayoutPickerView>(R.id.layoutPickerView)?.visibility = GONE
        rootView?.findViewById<xyz.xiao6.myboard.ui.keyboard.KeyboardSurfaceView>(R.id.keyboardView)?.visibility = VISIBLE

        // Re-apply global keyboard size (e.g. returning from Settings without switching layouts).
        val layout = controller?.currentLayout?.value
        if (layout != null) {
            rootView?.findViewById<xyz.xiao6.myboard.ui.ime.ImePanelView>(R.id.imePanel)
                ?.applyKeyboardLayoutSize(applyGlobalKeyboardSize(layout))
        }
    }

    private fun showImePickerNotificationIfNeeded() {
        if (imePickerNotificationShown) return
        val manager = NotificationManagerCompat.from(this)
        if (!manager.areNotificationsEnabled()) return
        val intent =
            Intent(this, ImePickerActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        val pending =
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val notification =
            NotificationCompat.Builder(this, IME_PICKER_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(getString(R.string.ime_picker_title))
                .setContentText(getString(R.string.ime_picker_desc))
                .setContentIntent(pending)
                .setOngoing(true)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .build()
        manager.notify(IME_PICKER_NOTIFICATION_ID, notification)
        imePickerNotificationShown = true
    }

    private fun dismissImePickerNotification() {
        if (!imePickerNotificationShown) return
        NotificationManagerCompat.from(this).cancel(IME_PICKER_NOTIFICATION_ID)
        imePickerNotificationShown = false
    }

    private fun ensureImePickerChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val existing = manager.getNotificationChannel(IME_PICKER_CHANNEL_ID)
        if (existing != null) return
        val channel =
            NotificationChannel(
                IME_PICKER_CHANNEL_ID,
                getString(R.string.ime_picker_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.ime_picker_channel_desc)
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
            }
        manager.createNotificationChannel(channel)
    }

    private fun updateToolbarFromPrefs() {
        val p = prefs ?: return
        val toolbar = toolbarView ?: return
        toolbar.setMaxVisibleCount(p.toolbarMaxVisibleCount)
        toolbar.submitItems(buildToolbarItems(toolbarSpec))
    }

    private fun registerPrefsListener() {
        val p = prefs ?: return
        prefsListener?.let { p.removeOnChangeListener(it) }
        val listener = p.addOnChangeListener {
            updateToolbarFromPrefs()
            updateRuntimeDictionariesFromPrefs()
            updateBenchmarkFlags()
        }
        prefsListener = listener
    }

    private fun updateRuntimeDictionariesFromPrefs() {
        val dicts = runtimeDicts ?: return
        val p = prefs ?: return
        val tag = activeLocaleTag ?: p.userLocaleTag
        if (tag.isNullOrBlank()) {
            dicts.setEnabledDictionaryIds(null)
            return
        }
        dicts.setEnabledDictionaryIds(p.getEnabledDictionaryIds(tag))
    }

    private fun updateBenchmarkFlags() {
        val keyboard = rootView?.findViewById<KeyboardSurfaceView>(R.id.keyboardView) ?: return
        val p = prefs ?: return
        keyboard.setPreviewEnabled(!p.benchmarkDisableKeyPreview)
        keyboard.setDecorationsEnabled(!p.benchmarkDisableKeyDecorations)
        keyboard.setLabelsEnabled(!p.benchmarkDisableKeyLabels)
        keyboard.setDebugTouchLoggingEnabled(p.debugTouchLoggingEnabled)
    }

    private fun logCommitLatencyStats() {
        if (commitLatenciesMs.isEmpty()) return
        val sorted = commitLatenciesMs.sorted()
        val p50 = percentile(sorted, 50.0)
        val p95 = percentile(sorted, 95.0)
        val p99 = percentile(sorted, 99.0)
        val max = sorted.lastOrNull() ?: 0L
        MLog.d(
            logTag,
            "inputLatency count=${sorted.size} p50=${p50}ms p95=${p95}ms p99=${p99}ms max=${max}ms",
        )
    }

    private fun percentile(sorted: List<Long>, p: Double): Long {
        if (sorted.isEmpty()) return 0L
        val pos = (p / 100.0) * (sorted.size - 1)
        val lower = kotlin.math.floor(pos).toInt()
        val upper = kotlin.math.ceil(pos).toInt()
        if (lower == upper) return sorted[lower]
        val weight = pos - lower
        val low = sorted[lower]
        val high = sorted[upper]
        return (low + (high - low) * weight).toLong()
    }

    private fun renderCandidatesUi(
        candidates: List<String>,
        composing: String,
        composingOptions: List<String>,
        toolbarView: ToolbarView,
        keyboardView: KeyboardSurfaceView,
        candidatePageView: CandidatePageView,
    ) {
        if (symbolsView?.visibility == VISIBLE || emojiView?.visibility == VISIBLE || clipboardView?.visibility == VISIBLE) {
            toolbarView.clearCandidates()
            candidatePageView.visibility = GONE
            toolbarView.visibility = View.INVISIBLE
            keyboardView.visibility = View.INVISIBLE
            return
        }
        if (resizeOverlay?.visibility == VISIBLE) {
            // Resizing mode: keep keyboard visible and avoid showing candidates/expanded page.
            toolbarView.clearCandidates()
            toolbarView.visibility = VISIBLE
            toolbarView.setItemsVisible(true)
            candidatePageView.visibility = GONE
            keyboardView.visibility = VISIBLE
            return
        }

        val showCandidates = candidates.isNotEmpty()
        // Manual-only: only expand when user taps the toolbar overflow arrow.
        val expandPage = showCandidates && isCandidatePageExpanded

        // Keep toolbar visible so the overflow arrow is always available.
        toolbarView.visibility = VISIBLE
        toolbarView.setItemsVisible(!showCandidates)
        toolbarView.setOverflowContentDescription(
            when {
                symbolsView?.visibility == VISIBLE -> "Close"
                clipboardView?.visibility == VISIBLE -> "Close"
                showCandidates -> if (expandPage) "Collapse candidates" else "Expand candidates"
                else -> "Hide keyboard"
            },
        )
        toolbarView.setOverflowRotation(if (expandPage) 180f else 0f)

        if (expandPage) {
            // Expanded page overlays keyboard region; keep keyboard height by making it INVISIBLE instead of GONE.
            keyboardView.visibility = View.INVISIBLE
            candidatePageView.visibility = VISIBLE
            toolbarView.clearCandidates()
            val left = composingOptions.takeIf { it.isNotEmpty() } ?: splitPinyinSegments(composing)
            candidatePageSelectedPinyinIndex =
                candidatePageSelectedPinyinIndex.coerceIn(0, (left.size - 1).coerceAtLeast(0))
            candidatePageView.submitPinyinSegments(left, selectedIndex = candidatePageSelectedPinyinIndex)
            candidatePageView.submitCandidates(candidatePagePreviewCandidates ?: candidates)
        } else {
            candidatePageView.visibility = GONE
            keyboardView.visibility = VISIBLE
            wordPreviewPopup?.dismiss()
            if (showCandidates) {
                toolbarView.submitCandidates(candidates)
            } else {
                toolbarView.clearCandidates()
            }
        }
    }

    private fun splitPinyinSegments(composing: String): List<String> {
        return PinyinSyllableSegmenter.segmentToList(composing)
    }

    private fun toggleComposingEditMode() {
        val composing = lastComposing
        if (composing.isBlank()) return
        isEditingComposing = !isEditingComposing
        if (isEditingComposing) {
            composingEditBuffer = composing
            composingEditCursor = composingEditBuffer.codePointCount(0, composingEditBuffer.length)
        }
        updateComposingPopup()
    }

    private fun updateComposingPopup() {
        val popup = composingPopup ?: return
        val anchor = topBarSlotView ?: return
        val (raw, display) = buildComposingPopupText(lastComposing)
        popup.updateAbove(
            anchor = anchor,
            composing = raw,
            displayText = display,
            xMarginPx = 0,
            yMarginPx = 0,
            editing = isEditingComposing,
            cursorIndex = composingEditCursor,
        )
    }

    private fun buildComposingPopupText(composing: String): Pair<String, String> {
        val raw = if (isEditingComposing) composingEditBuffer else composing
        if (raw.isBlank()) return "" to ""
        val segmented = PinyinSyllableSegmenter.segmentForDisplay(raw)
        val display = if (segmented.isBlank()) raw.trim() else segmented
        return raw to display
    }

    private fun handleComposingEditKey(
        keyId: String,
        trigger: GestureType,
        keyboardView: KeyboardSurfaceView,
    ): Boolean {
        if (!isEditingComposing || trigger != GestureType.TAP && trigger != GestureType.LONG_PRESS) return false
        when (keyId) {
            KeyIds.BACKSPACE -> {
                if (trigger == GestureType.LONG_PRESS) {
                    composingEditBuffer = ""
                    composingEditCursor = 0
                } else {
                    val result = deleteBeforeCursor(composingEditBuffer, composingEditCursor)
                    composingEditBuffer = result.text
                    composingEditCursor = result.cursor
                }
                applyComposingEditBuffer()
                return true
            }
            KeyIds.ENTER, KeyIds.SPACE -> {
                isEditingComposing = false
                composingEditCursor = 0
                updateComposingPopup()
                return false
            }
        }
        val label = keyboardView.resolveKeyLabelForInput(keyId)
            ?.takeIf { it.length == 1 && it[0].isLetter() }
            ?: return true
        val result = insertAtCursor(composingEditBuffer, composingEditCursor, label)
        composingEditBuffer = result.text
        composingEditCursor = result.cursor
        applyComposingEditBuffer()
        return true
    }

    private fun applyComposingEditBuffer() {
        controller?.replaceComposing(composingEditBuffer)
        updateComposingPopup()
    }

    private data class CursorResult(val text: String, val cursor: Int)

    private fun insertAtCursor(text: String, cursor: Int, insert: String): CursorResult {
        val safeCursor = cursor.coerceIn(0, text.codePointCount(0, text.length))
        val offset = text.offsetByCodePoints(0, safeCursor)
        val next = text.substring(0, offset) + insert + text.substring(offset)
        return CursorResult(next, safeCursor + insert.codePointCount(0, insert.length))
    }

    private fun deleteBeforeCursor(text: String, cursor: Int): CursorResult {
        val count = text.codePointCount(0, text.length)
        val safeCursor = cursor.coerceIn(0, count)
        if (safeCursor == 0) return CursorResult(text, 0)
        val leftOffset = text.offsetByCodePoints(0, safeCursor - 1)
        val rightOffset = text.offsetByCodePoints(0, safeCursor)
        val next = text.removeRange(leftOffset, rightOffset)
        return CursorResult(next, safeCursor - 1)
    }

    private fun registerClipboardListener() {
        val manager = clipboardManager ?: return
        if (clipboardListener != null) return
        val listener = ClipboardManager.OnPrimaryClipChangedListener { readPrimaryClip() }
        clipboardListener = listener
        manager.addPrimaryClipChangedListener(listener)
        readPrimaryClip()
    }

    private fun unregisterClipboardListener() {
        val manager = clipboardManager ?: return
        val listener = clipboardListener ?: return
        manager.removePrimaryClipChangedListener(listener)
        clipboardListener = null
    }

    private fun readPrimaryClip() {
        val manager = clipboardManager ?: return
        val clip = manager.primaryClip ?: return
        if (clip.itemCount <= 0) return
        for (i in 0 until clip.itemCount) {
            val item = clip.getItemAt(i)
            val text = item.coerceToText(this)?.toString().orEmpty()
            addClipboardEntry(text)
        }
    }

    private fun addClipboardEntry(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        clipboardEntries.removeAll { it.text == trimmed }
        val entry = ClipboardLayoutView.ClipboardEntry(
            id = ++clipboardEntryIdSeed,
            text = trimmed,
            timestamp = System.currentTimeMillis(),
        )
        clipboardEntries.addFirst(entry)
        while (clipboardEntries.size > MAX_CLIPBOARD_ENTRIES) {
            clipboardEntries.removeLast()
        }
        if (clipboardView?.visibility == VISIBLE) {
            clipboardView?.submitItems(clipboardEntries.toList())
        }
    }

    private companion object {
        private const val MAX_CLIPBOARD_ENTRIES = 50
        private const val IME_PICKER_CHANNEL_ID = "ime_picker"
        private const val IME_PICKER_NOTIFICATION_ID = 0x494D45
    }
}
