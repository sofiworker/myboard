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
import xyz.xiao6.myboard.ui.keyboard.KeyboardSurfaceView
import xyz.xiao6.myboard.ui.popup.FloatingCandidatePopup
import xyz.xiao6.myboard.ui.popup.FloatingComposingPopup
import xyz.xiao6.myboard.ui.popup.FloatingTextPreviewPopup
import xyz.xiao6.myboard.ui.popup.PopupView
import xyz.xiao6.myboard.ui.toolbar.ToolbarView
import xyz.xiao6.myboard.ui.candidate.CandidatePageView
import xyz.xiao6.myboard.ui.layout.LayoutPickerView
import xyz.xiao6.myboard.util.MLog
import java.util.Locale
import android.content.Intent
import xyz.xiao6.myboard.ui.SettingsActivity
import android.widget.Toast
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.FrameLayout
import xyz.xiao6.myboard.store.MyBoardPrefs
import xyz.xiao6.myboard.model.DictionarySpec
import kotlin.math.roundToInt
import java.util.Locale.ROOT

class MyBoardImeService : InputMethodService() {
    private val logTag = "ImeService"

    private var isCandidatePageExpanded: Boolean = false
    private var lastCandidates: List<String> = emptyList()
    private var lastComposing: String = ""

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
    private var candidatePopup: FloatingCandidatePopup? = null
    private var composingPopup: FloatingComposingPopup? = null
    private var wordPreviewPopup: FloatingTextPreviewPopup? = null

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
        candidatePopup?.dismiss()
        composingPopup?.dismiss()
        wordPreviewPopup?.dismiss()
        scope.cancel()
    }

    override fun onCreateInputView(): View {
        MLog.d(logTag, "onCreateInputView")

        val view = LayoutInflater.from(this).inflate(R.layout.ime_view, null, false)
        rootView = view

        val imeRoot = view.findViewById<View>(R.id.imeRoot)
        val topBarSlot = view.findViewById<View>(R.id.topBarSlot)
        val toolbarView = view.findViewById<ToolbarView>(R.id.toolbarView)
        val keyboardView = view.findViewById<KeyboardSurfaceView>(R.id.keyboardView)
        val candidatePageView = view.findViewById<CandidatePageView>(R.id.candidatePageView)
        val layoutPickerView = view.findViewById<LayoutPickerView>(R.id.layoutPickerView)
        val popupHost = view.findViewById<FrameLayout>(R.id.popupHost)
        val popupView = PopupView(popupHost).apply { applyTheme(themeSpec) }
        keyboardView.setPopupView(popupView)
        keyboardView.setTheme(themeSpec)
        toolbarView.applyTheme(themeSpec)
        candidatePageView.applyTheme(themeSpec)
        layoutPickerView.applyTheme(themeSpec)

        val marginPx = (resources.displayMetrics.density * 8f).toInt()
        val candidatePopup = FloatingCandidatePopup(this).apply { applyTheme(themeSpec) }
        val composingPopup = FloatingComposingPopup(this).apply { applyTheme(themeSpec) }
        val wordPreviewPopup = FloatingTextPreviewPopup(this).apply { applyTheme(themeSpec) }
        this.candidatePopup = candidatePopup
        this.composingPopup = composingPopup
        this.wordPreviewPopup = wordPreviewPopup

        val layoutManager = LayoutManager(this).loadAllFromAssets()
        val dictionaryManager = DictionaryManager(this).loadAll()

        // Ensure toolbar is visible and has actions.
        toolbarView.submitItems(buildToolbarItems(toolbarSpec))
        toolbarView.onItemClick = { item ->
            when (item.itemId) {
                "layout" -> {
                    showLayoutPicker(
                        layoutManager = layoutManager,
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

        toolbarView.visibility = VISIBLE
        candidatePopup.onCandidateClick = { text ->
            playKeyFeedback(keyboardView)
            controller?.onCandidateSelected(text)
            isCandidatePageExpanded = false
        }
        candidatePopup.onExpandToggle = {
            if (lastCandidates.isNotEmpty()) {
                isCandidatePageExpanded = !isCandidatePageExpanded
                // Force a re-render even if candidates list didn't change.
                renderCandidatesUi(
                    candidates = lastCandidates,
                    composing = lastComposing,
                    topBarSlot = topBarSlot,
                    toolbarView = toolbarView,
                    keyboardView = keyboardView,
                    candidatePopup = candidatePopup,
                    candidatePageView = candidatePageView,
                )
            }
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
                topBarSlot = topBarSlot,
                toolbarView = toolbarView,
                keyboardView = keyboardView,
                candidatePopup = candidatePopup,
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
                renderCandidatesUi(
                    candidates = list,
                    composing = lastComposing,
                    topBarSlot = topBarSlot,
                    toolbarView = toolbarView,
                    keyboardView = keyboardView,
                    candidatePopup = candidatePopup,
                    candidatePageView = candidatePageView,
                )

                composingPopup.updateAbove(
                    anchor = topBarSlot,
                    composing = c.composingText.value,
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
                        topBarSlot = topBarSlot,
                        toolbarView = toolbarView,
                        keyboardView = keyboardView,
                        candidatePopup = candidatePopup,
                        candidatePageView = candidatePageView,
                    )
                }
                composingPopup.updateAbove(
                    anchor = topBarSlot,
                    composing = composing,
                    xMarginPx = 0,
                    yMarginPx = 0,
                )
            }
            .launchIn(scope)

        // Keep RuntimeDictionaryManager in sync with layout switches.
        c.currentLayout
            .onEach { layout ->
                val layoutId = layout?.layoutId ?: return@onEach
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

        onLocaleSwitched(next)
    }

    private fun showLayoutPicker(
        layoutManager: LayoutManager,
        keyboardView: KeyboardSurfaceView,
        candidatePageView: CandidatePageView,
        layoutPickerView: LayoutPickerView,
    ) {
        // Close any overlays which conflict with the picker.
        isCandidatePageExpanded = false
        candidatePageView.visibility = GONE
        candidatePopup?.dismiss()
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
        val normalized = localeTag.trim().replace('_', '-')
        val next = Locale.forLanguageTag(normalized)
        MLog.d(logTag, "SwitchLocale -> $normalized")

        prefs?.userLocaleTag = normalized
        activeLocaleTag = normalized
        runtimeDicts?.setLocale(next)

        val profile = subtypeManager?.resolve(next) ?: return
        val preferredLayoutId = prefs?.getPreferredLayoutId(profile.localeTag)
        val enabledLayouts = enabledLayoutsFor(profile)
        val targetLayoutId = when {
            !preferredLayoutId.isNullOrBlank() && preferredLayoutId in enabledLayouts -> preferredLayoutId
            enabledLayouts.isNotEmpty() -> enabledLayouts.firstOrNull()
            !profile.defaultLayoutId.isNullOrBlank() -> profile.defaultLayoutId
            else -> profile.layoutIds.firstOrNull()
        } ?: return
        if (controller?.currentLayout?.value?.layoutId != targetLayoutId) {
            controller?.loadLayout(targetLayoutId)
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        candidatePopup?.dismiss()
        composingPopup?.dismiss()
        wordPreviewPopup?.dismiss()
    }

    private fun renderCandidatesUi(
        candidates: List<String>,
        composing: String,
        topBarSlot: View,
        toolbarView: ToolbarView,
        keyboardView: KeyboardSurfaceView,
        candidatePopup: FloatingCandidatePopup,
        candidatePageView: CandidatePageView,
    ) {
        val showCandidates = candidates.isNotEmpty()
        val shouldShowExpandButton = showCandidates && candidates.size >= 6

        // Manual-only: only expand when user taps the arrow.
        val expandPage = showCandidates && isCandidatePageExpanded

        candidatePopup.setExpanded(expandPage)

        toolbarView.visibility = if (showCandidates) GONE else VISIBLE

        if (expandPage) {
            // Expanded page overlays keyboard region; keep keyboard height by making it INVISIBLE instead of GONE.
            keyboardView.visibility = View.INVISIBLE
            candidatePageView.visibility = VISIBLE
            candidatePopup.dismiss()
            candidatePageView.submitCandidates(candidates)
            candidatePageView.submitPinyinSegments(splitPinyinSegments(composing))
        } else {
            candidatePageView.visibility = GONE
            keyboardView.visibility = VISIBLE
            wordPreviewPopup?.dismiss()
            if (showCandidates) {
                candidatePopup.updateInSlot(anchor = topBarSlot, candidates = candidates, showExpandButton = shouldShowExpandButton)
            } else {
                candidatePopup.dismiss()
            }
        }
    }

    private fun splitPinyinSegments(composing: String): List<String> {
        val t = composing.trim()
        if (t.isBlank()) return emptyList()
        val parts = t.split('\'', ' ').map { it.trim() }.filter { it.isNotBlank() }
        return if (parts.isNotEmpty()) parts else listOf(t)
    }
}
