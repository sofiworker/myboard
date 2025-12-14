package xyz.xiao6.myboard.ime

import android.inputmethodservice.InputMethodService
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import xyz.xiao6.myboard.R
import xyz.xiao6.myboard.controller.InputMethodController
import xyz.xiao6.myboard.decoder.DecoderFactory
import xyz.xiao6.myboard.manager.DictionaryManager
import xyz.xiao6.myboard.manager.LayoutManager
import xyz.xiao6.myboard.manager.RuntimeDictionaryManager
import xyz.xiao6.myboard.manager.SubtypeManager
import xyz.xiao6.myboard.ui.candidate.CandidateView
import xyz.xiao6.myboard.ui.keyboard.KeyboardSurfaceView
import xyz.xiao6.myboard.ui.popup.PopupView
import xyz.xiao6.myboard.ui.toolbar.ToolbarView
import xyz.xiao6.myboard.util.MLog
import java.util.Locale
import android.content.Intent
import xyz.xiao6.myboard.ui.MainActivity
import android.widget.Toast
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.TextView

class MyBoardImeService : InputMethodService() {
    private val logTag = "ImeService"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var rootView: View? = null
    private var controller: InputMethodController? = null
    private var runtimeDicts: RuntimeDictionaryManager? = null
    private var decoderFactory: DecoderFactory? = null
    private var subtypeManager: SubtypeManager? = null

    override fun onCreate() {
        super.onCreate()
        MLog.d(logTag, "onCreate")
        decoderFactory = DecoderFactory(this)
        subtypeManager = SubtypeManager(this).loadAll()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onCreateInputView(): View {
        MLog.d(logTag, "onCreateInputView")

        val view = LayoutInflater.from(this).inflate(R.layout.ime_view, null, false)
        rootView = view

        val candidateView = view.findViewById<CandidateView>(R.id.candidateView)
        val toolbarView = view.findViewById<ToolbarView>(R.id.toolbarView)
        val composingStrip = view.findViewById<View>(R.id.composingStrip)
        val composingBadge = view.findViewById<TextView>(R.id.composingBadge)
        val keyboardView = view.findViewById<KeyboardSurfaceView>(R.id.keyboardView)
        keyboardView.setPopupView(PopupView(this))

        // Ensure toolbar is visible and has actions.
        toolbarView.submitItems(
            listOf(
                ToolbarView.Item("voice", R.drawable.ic_toolbar_voice, "Voice"),
                ToolbarView.Item("emoji", R.drawable.ic_toolbar_emoji, "Emoji"),
                ToolbarView.Item("settings", R.drawable.ic_toolbar_settings, "Settings"),
            ),
        )
        toolbarView.onItemClick = { item ->
            when (item.itemId) {
                "settings" -> {
                    val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
                else -> Toast.makeText(this, "toolbar: ${item.itemId}", Toast.LENGTH_SHORT).show()
            }
        }

        candidateView.submitCandidates(emptyList())
        candidateView.visibility = GONE
        toolbarView.visibility = VISIBLE
        composingStrip.visibility = GONE
        composingBadge.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }

        val layoutManager = LayoutManager(this).loadAllFromAssets()
        val dictionaryManager = DictionaryManager(this).loadAll()
        val dicts = RuntimeDictionaryManager(dictionaryManager = dictionaryManager)
        runtimeDicts = dicts

        val c = InputMethodController(
            layoutManager = layoutManager,
        ).apply {
            onCommitText = { text -> commitTextToEditor(text) }
            onSwitchLocale = { localeTag -> onLocaleSwitched(localeTag) }
        }
        controller = c
        c.attach(keyboardView)
        keyboardView.onTrigger = c::onKeyTriggered
        keyboardView.onAction = c::onAction

        c.candidates
            .onEach { list ->
                candidateView.submitCandidates(list)
                renderTopBar(
                    composing = c.composingText.value,
                    candidatesNonEmpty = list.isNotEmpty(),
                    candidateView = candidateView,
                    toolbarView = toolbarView,
                    composingStrip = composingStrip,
                    composingBadge = composingBadge,
                )
            }
            .launchIn(scope)

        c.composingText
            .onEach { composing ->
                renderTopBar(
                    composing = composing,
                    candidatesNonEmpty = c.candidates.value.isNotEmpty(),
                    candidateView = candidateView,
                    toolbarView = toolbarView,
                    composingStrip = composingStrip,
                    composingBadge = composingBadge,
                )
            }
            .launchIn(scope)

        // Keep RuntimeDictionaryManager in sync with layout switches.
        c.currentLayout
            .onEach { layout -> layout?.layoutId?.let { dicts.setLayoutId(it) } }
            .launchIn(scope)

        // Swap decoder whenever the active dictionary changes.
        dicts.active
            .onEach { spec -> c.setDecoder(requireNotNull(decoderFactory).create(spec)) }
            .launchIn(scope)

        candidateView.onCandidateClick = { text ->
            c.onCandidateSelected(text)
        }

        // Initial locale/layout selection will be done in onStartInputView.
        return view
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)

        val locale = resources.configuration.locales[0] ?: Locale.getDefault()
        MLog.d(logTag, "onStartInputView restarting=$restarting locale=${locale.toLanguageTag()}")

        val profile = subtypeManager?.resolve(locale)
        val layoutId = profile?.defaultLayoutId
            ?: profile?.layoutIds?.firstOrNull()
            ?: runCatching { LayoutManager(this).loadAllFromAssets().getDefaultLayout(locale).layoutId }.getOrNull()
            ?: "qwerty"

        runtimeDicts?.setLocale(locale)
        runtimeDicts?.setLayoutId(layoutId)

        controller?.loadLayout(layoutId)
        controller?.setDecoder(requireNotNull(decoderFactory).create(runtimeDicts?.active?.value))
    }

    private fun commitTextToEditor(text: String) {
        val ic = currentInputConnection ?: return
        if (text == "\b") {
            ic.deleteSurroundingText(1, 0)
            return
        }
        ic.commitText(text, 1)
    }

    private fun onLocaleSwitched(localeTag: String) {
        val normalized = localeTag.trim().replace('_', '-')
        val next = Locale.forLanguageTag(normalized)
        MLog.d(logTag, "SwitchLocale -> $normalized")

        runtimeDicts?.setLocale(next)

        val profile = subtypeManager?.resolve(next) ?: return
        val targetLayoutId = profile.defaultLayoutId ?: profile.layoutIds.firstOrNull() ?: return
        if (controller?.currentLayout?.value?.layoutId != targetLayoutId) {
            controller?.loadLayout(targetLayoutId)
        }
    }

    private fun renderTopBar(
        composing: String,
        candidatesNonEmpty: Boolean,
        candidateView: CandidateView,
        toolbarView: ToolbarView,
        composingStrip: View,
        composingBadge: TextView,
    ) {
        val composingNonEmpty = composing.isNotBlank()
        val showCandidates = composingNonEmpty || candidatesNonEmpty
        candidateView.visibility = if (showCandidates) VISIBLE else GONE
        toolbarView.visibility = if (showCandidates) GONE else VISIBLE

        if (showCandidates && composingNonEmpty) {
            composingBadge.text = composing
            composingStrip.visibility = VISIBLE
        } else {
            composingStrip.visibility = GONE
        }
    }
}
