package xyz.xiao6.myboard.ime

import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.LayoutInflater
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.inputmethod.EditorInfo
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
import xyz.xiao6.myboard.util.MLog
import java.util.Locale
import android.content.Intent
import xyz.xiao6.myboard.ui.SettingsActivity
import android.widget.Toast
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.FrameLayout
import xyz.xiao6.myboard.store.MyBoardPrefs
import xyz.xiao6.myboard.model.ActionType
import xyz.xiao6.myboard.model.DictionarySpec
import xyz.xiao6.myboard.model.KeyAction
import kotlin.math.roundToInt
import java.util.Locale.ROOT
import xyz.xiao6.myboard.util.PinyinSyllableSegmenter

class MyBoardImeService : InputMethodService() {
    private val logTag = "ImeService"

    private var isCandidatePageExpanded: Boolean = false
    private var lastCandidates: List<String> = emptyList()
    private var lastComposing: String = ""
    private var lastComposingOptions: List<String> = emptyList()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var rootView: View? = null
    private var controller: InputMethodController? = null
    private var runtimeDicts: RuntimeDictionaryManager? = null
    private var decoderFactory: DecoderFactory? = null
    private var subtypeManager: SubtypeManager? = null
    private var prefs: MyBoardPrefs? = null
    private var themeSpec: ThemeSpec? = null
    private var toolbarSpec: ToolbarSpec? = null
    private var activeLocaleTag: String? = null
    private var decoderBuildJob: Job? = null
    private var composingPopup: FloatingComposingPopup? = null
    private var wordPreviewPopup: FloatingTextPreviewPopup? = null
    private var symbolsView: SymbolsLayoutView? = null
    private var topBarSlotView: View? = null
    private var popupMarginPx: Int = 0

    override fun onCreate() {
        super.onCreate()
        MLog.d(logTag, "onCreate")
        decoderFactory = DecoderFactory(this)
        subtypeManager = SubtypeManager(this).loadAll()
        prefs = MyBoardPrefs(this)
        themeSpec = ThemeManager(this).loadAllFromAssets().getDefaultTheme()
        toolbarSpec = ToolbarManager(this).loadAllFromAssets().getDefaultToolbar()
    }

    override fun onDestroy() {
        super.onDestroy()
        composingPopup?.dismiss()
        wordPreviewPopup?.dismiss()
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
        val toolbarView = view.findViewById<ToolbarView>(R.id.toolbarView)
        val keyboardView = view.findViewById<KeyboardSurfaceView>(R.id.keyboardView)
        val candidatePageView = view.findViewById<CandidatePageView>(R.id.candidatePageView)
        val layoutPickerView = view.findViewById<LayoutPickerView>(R.id.layoutPickerView)
        val symbolsView = view.findViewById<SymbolsLayoutView>(R.id.symbolsView)
        val popupHost = view.findViewById<FrameLayout>(R.id.popupHost)
        val popupView = PopupView(popupHost).apply { applyTheme(themeSpec) }
        keyboardView.setPopupView(popupView)
        keyboardView.setTheme(themeSpec)
        toolbarView.applyTheme(themeSpec)
        candidatePageView.applyTheme(themeSpec)
        layoutPickerView.applyTheme(themeSpec)
        symbolsView.applyTheme(themeSpec)
        this.symbolsView = symbolsView

        val marginPx = (resources.displayMetrics.density * 8f).toInt()
        popupMarginPx = marginPx
        val composingPopup = FloatingComposingPopup(this).apply { applyTheme(themeSpec) }
        val wordPreviewPopup = FloatingTextPreviewPopup(this).apply { applyTheme(themeSpec) }
        this.composingPopup = composingPopup
        this.wordPreviewPopup = wordPreviewPopup

        val layoutManager = LayoutManager(this).loadAllFromAssets()
        val dictionaryManager = DictionaryManager(this).loadAll()

        fun hideOverlays() {
            isCandidatePageExpanded = false
            candidatePageView.visibility = GONE
            layoutPickerView.visibility = GONE
            toolbarView.clearCandidates()
            wordPreviewPopup.dismiss()
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

        symbolsView.onBack = { hideSymbols() }
        symbolsView.onCommitSymbol = { symbol ->
            playKeyFeedback(keyboardView)
            controller?.onAction(KeyAction(actionType = ActionType.COMMIT, value = symbol))
            if (!symbolsView.isLocked()) hideSymbols()
        }

        // Ensure toolbar is visible and has actions.
        toolbarView.submitItems(buildToolbarItems(toolbarSpec))
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
                "settings" -> {
                    val intent = Intent(this, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
                else -> Toast.makeText(this, "toolbar: ${item.itemId}", Toast.LENGTH_SHORT).show()
            }
        }
        toolbarView.onOverflowClick = {
            // Priority:
            // 1) If symbols panel is open -> close it.
            // 2) If there are candidates -> toggle expanded candidate page.
            // 3) Otherwise -> collapse/hide the IME.
            when {
                symbolsView.visibility == VISIBLE -> hideSymbols()
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
            controller?.onCandidateSelected(text)
            isCandidatePageExpanded = false
        }
        candidatePageView.onCandidateClick = { text ->
            playKeyFeedback(keyboardView)
            controller?.onCandidateSelected(text)
            isCandidatePageExpanded = false
            candidatePageView.visibility = GONE
            keyboardView.visibility = VISIBLE
            wordPreviewPopup.dismiss()
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
            controller?.onAction(xyz.xiao6.myboard.model.KeyAction(actionType = xyz.xiao6.myboard.model.ActionType.BACKSPACE))
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
        }
        controller = c
        c.attach(keyboardView)
        keyboardView.onTrigger = { keyId, trigger ->
            playKeyFeedback(keyboardView)
            c.onKeyTriggered(keyId, trigger)
        }
        keyboardView.onAction = { action ->
            playKeyFeedback(keyboardView)
            c.onAction(action)
        }

        c.candidates
            .onEach { list ->
                lastCandidates = list
                lastComposing = c.composingText.value
                lastComposingOptions = c.composingOptions.value
                renderCandidatesUi(
                    candidates = list,
                    composing = lastComposing,
                    composingOptions = lastComposingOptions,
                    toolbarView = toolbarView,
                    keyboardView = keyboardView,
                    candidatePageView = candidatePageView,
                )

                composingPopup.updateAbove(
                    anchor = topBarSlot,
                    composing = PinyinSyllableSegmenter.segmentForDisplay(c.composingText.value),
                    xMarginPx = 0,
                    yMarginPx = 0,
                )
            }
            .launchIn(scope)

        c.composingText
            .onEach { composing ->
                if (composing.isBlank()) {
                    isCandidatePageExpanded = false
                }
                lastComposing = composing
                if (composing.isBlank()) {
                    lastCandidates = emptyList()
                    renderCandidatesUi(
                        candidates = emptyList(),
                        composing = composing,
                        composingOptions = lastComposingOptions,
                        toolbarView = toolbarView,
                        keyboardView = keyboardView,
                        candidatePageView = candidatePageView,
                    )
                }
                composingPopup.updateAbove(
                    anchor = topBarSlot,
                    composing = PinyinSyllableSegmenter.segmentForDisplay(composing),
                    xMarginPx = 0,
                    yMarginPx = 0,
                )
            }
            .launchIn(scope)

        c.composingOptions
            .onEach { options ->
                lastComposingOptions = options
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
                imePanel.applyKeyboardLayoutSize(resolved)
                dicts.setLayoutId(layoutId)
                activeLocaleTag?.let { tag -> prefs?.setPreferredLayoutId(tag, layoutId) }
            }
            .launchIn(scope)

        // Swap decoder whenever the active dictionary changes.
        dicts.active
            .onEach { spec -> swapDecoderAsync(c, spec) }
            .launchIn(scope)

        // Initial locale/layout selection will be done in onStartInputView.
        return view
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
        val popup = wordPreviewPopup ?: return
        val anchor =
            topBarSlotView
                ?: rootView?.findViewById(R.id.topBarSlot)
                ?: return
        val margin = popupMarginPx.takeIf { it > 0 } ?: (resources.displayMetrics.density * 8f).toInt()
        popup.showAbove(anchor, text, marginPx = margin)
        anchor.removeCallbacks(dismissImeHintRunnable)
        anchor.postDelayed(dismissImeHintRunnable, 1600L)
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
                ToolbarView.Item("settings", R.drawable.ic_toolbar_settings, "Settings"),
            )
        }

        fun iconResId(icon: String): Int {
            return when (icon.lowercase()) {
                "layout" -> R.drawable.ic_toolbar_layout
                "voice" -> R.drawable.ic_toolbar_voice
                "emoji" -> R.drawable.ic_toolbar_emoji
                "settings" -> R.drawable.ic_toolbar_settings
                else -> R.drawable.ic_toolbar_settings
            }
        }

        return items.map { ToolbarView.Item(it.itemId, iconResId(it.icon), it.name) }
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
        prefs?.setPreferredLayoutId(normalized, targetLayoutId)

        if (controller?.currentLayout?.value?.layoutId != targetLayoutId) {
            controller?.loadLayout(targetLayoutId)
        }
    }

    private fun switchToNextEnabledLayout(profile: xyz.xiao6.myboard.model.LocaleLayoutProfile) {
        val list = enabledLayoutsFor(profile)
        if (list.isEmpty()) return
        val current = controller?.currentLayout?.value?.layoutId
        val idx = current?.let { list.indexOf(it) } ?: -1
        val next = if (idx < 0) list.first() else list[(idx + 1) % list.size]
        prefs?.setPreferredLayoutId(profile.localeTag, next)
        controller?.loadLayout(next)
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)

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

        controller?.loadLayout(layoutId)
        swapDecoderAsync(controller, runtimeDicts?.active?.value)
        applyLocaleSymbolOverrides(activeLocaleTag ?: locale.toLanguageTag())
    }

    private fun swapDecoderAsync(controller: InputMethodController?, spec: DictionarySpec?) {
        val c = controller ?: return
        val factory = decoderFactory ?: return
        decoderBuildJob?.cancel()
        decoderBuildJob =
            scope.launch(Dispatchers.Default) {
                val decoder = factory.create(spec)
                withContext(Dispatchers.Main.immediate) {
                    c.setDecoder(decoder)
                }
            }
    }

    private fun commitTextToEditor(text: String) {
        val ic = currentInputConnection ?: return
        if (text == "\b") {
            ic.deleteSurroundingText(1, 0)
            return
        }
        ic.commitText(text, 1)
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
            runtimeDicts?.setLocale(nextLocale)
            runtimeDicts?.setLayoutId(currentLayoutId)
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
        runtimeDicts?.setLocale(nextLocale)
        runtimeDicts?.setLayoutId(targetLayoutId)
        prefs?.setPreferredLayoutId(profile.localeTag, targetLayoutId)

        if (controller?.currentLayout?.value?.layoutId != targetLayoutId) {
            controller?.loadLayout(targetLayoutId)
        }
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
        val hintOverrides = LinkedHashMap<String, Map<xyz.xiao6.myboard.model.HintPosition, String>>()

        // Locale toggle key(s): drive by SpecialKey so custom layouts work.
        for (key in keys) {
            if (key.specialKey != xyz.xiao6.myboard.model.SpecialKey.TOGGLE_LOCALE) continue
            labelOverrides[key.keyId] = currentLabel
            if (!nextLabel.isNullOrBlank()) {
                hintOverrides[key.keyId] =
                    mapOf(xyz.xiao6.myboard.model.HintPosition.BOTTOM_RIGHT to "/$nextLabel")
            }
        }

        // Basic punctuation replacement when keeping the same layout across locales:
        // - Try to find common punctuation keys by keyId or by their current label.
        // - If layout is fully custom, this is best-effort and won't override unrelated keys.
        val wantsChinesePunct = currentNormalized.startsWith("zh", ignoreCase = true)
        val commaOut = if (wantsChinesePunct) "，" else ","
        val periodOut = if (wantsChinesePunct) "。" else "."

        for (key in keys) {
            if (key.specialKey != null) continue
            val label = key.label.trim()
            when {
                key.keyId == "key_comma" || label == "," || label == "，" || key.primaryCode == 44 -> {
                    labelOverrides[key.keyId] = commaOut
                }
                key.keyId == "key_period" || label == "." || label == "。" || key.primaryCode == 46 || key.primaryCode == 12290 -> {
                    labelOverrides[key.keyId] = periodOut
                }
            }
        }

        val invalidateIds = (labelOverrides.keys + hintOverrides.keys).toSet()
        c.updateState(
            reducer = { s -> s.copy(labelOverrides = labelOverrides, hintOverrides = hintOverrides) },
            invalidateKeyIds = invalidateIds,
        )
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
        composingPopup?.dismiss()
        wordPreviewPopup?.dismiss()
    }

    private fun renderCandidatesUi(
        candidates: List<String>,
        composing: String,
        composingOptions: List<String>,
        toolbarView: ToolbarView,
        keyboardView: KeyboardSurfaceView,
        candidatePageView: CandidatePageView,
    ) {
        if (symbolsView?.visibility == VISIBLE) {
            toolbarView.clearCandidates()
            candidatePageView.visibility = GONE
            toolbarView.visibility = View.INVISIBLE
            keyboardView.visibility = View.INVISIBLE
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
            candidatePageView.submitCandidates(candidates)
            val left = composingOptions.takeIf { it.isNotEmpty() } ?: splitPinyinSegments(composing)
            candidatePageView.submitPinyinSegments(left)
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
}
