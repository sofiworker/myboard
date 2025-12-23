package xyz.xiao6.myboard.ui

import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.view.WindowManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import xyz.xiao6.myboard.R
import xyz.xiao6.myboard.store.SettingsStore

class BenchmarkInputActivity : AppCompatActivity() {
    private var requestedIme = false
    private var prevUserLocaleTag: String? = null
    private var prevEnabledLocales: List<String> = emptyList()
    private var prevEnabledLayoutsEn: List<String> = emptyList()
    private var prevPreferredLayoutEn: String? = null
    private var prevBenchmarkDisableCandidates: Boolean = false
    private var prevBenchmarkDisableKeyPreview: Boolean = false
    private var prevBenchmarkDisableKeyDecorations: Boolean = false
    private var prevBenchmarkDisableKeyLabels: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE,
        )
        applyBenchmarkLocale()
        setContentView(R.layout.activity_benchmark_input)

        val input = findViewById<EditText>(R.id.benchmarkInputField)
        input.requestFocus()
        input.requestFocusFromTouch()
    }

    override fun onDestroy() {
        super.onDestroy()
        restoreLocale()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus || requestedIme) return
        val input = findViewById<EditText>(R.id.benchmarkInputField)
        input.requestFocus()
        input.requestFocusFromTouch()
        showIme(input)
        requestedIme = true
    }

    private fun showIme(target: EditText) {
        val imm = getSystemService<InputMethodManager>() ?: return
        target.post {
            if (!target.hasWindowFocus()) return@post
            if (!imm.isActive(target)) {
                target.postDelayed(
                    { imm.showSoftInput(target, InputMethodManager.SHOW_IMPLICIT) },
                    120L,
                )
            } else {
                imm.showSoftInput(target, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    private fun applyBenchmarkLocale() {
        val prefs = SettingsStore(this)
        prevUserLocaleTag = prefs.userLocaleTag
        prevEnabledLocales = prefs.getEnabledLocaleTags()
        prevEnabledLayoutsEn = prefs.getEnabledLayoutIds(LOCALE_EN)
        prevPreferredLayoutEn = prefs.getPreferredLayoutId(LOCALE_EN)
        prevBenchmarkDisableCandidates = prefs.benchmarkDisableCandidates
        prevBenchmarkDisableKeyPreview = prefs.benchmarkDisableKeyPreview
        prevBenchmarkDisableKeyDecorations = prefs.benchmarkDisableKeyDecorations
        prevBenchmarkDisableKeyLabels = prefs.benchmarkDisableKeyLabels

        val enabledLocales =
            (prevEnabledLocales + LOCALE_EN)
                .distinct()
                .filter { it.isNotBlank() }
        prefs.setEnabledLocaleTags(enabledLocales)
        prefs.setEnabledLayoutIds(LOCALE_EN, listOf(LAYOUT_QWERTY))
        prefs.setPreferredLayoutId(LOCALE_EN, LAYOUT_QWERTY)
        prefs.userLocaleTag = LOCALE_EN
        prefs.benchmarkDisableCandidates = false
        prefs.benchmarkDisableKeyPreview = false
        prefs.benchmarkDisableKeyDecorations = false
        prefs.benchmarkDisableKeyLabels = false
    }

    private fun restoreLocale() {
        val prefs = SettingsStore(this)
        prefs.userLocaleTag = prevUserLocaleTag
        prefs.setEnabledLocaleTags(prevEnabledLocales)
        prefs.setEnabledLayoutIds(LOCALE_EN, prevEnabledLayoutsEn)
        prefs.setPreferredLayoutId(LOCALE_EN, prevPreferredLayoutEn)
        prefs.benchmarkDisableCandidates = prevBenchmarkDisableCandidates
        prefs.benchmarkDisableKeyPreview = prevBenchmarkDisableKeyPreview
        prefs.benchmarkDisableKeyDecorations = prevBenchmarkDisableKeyDecorations
        prefs.benchmarkDisableKeyLabels = prevBenchmarkDisableKeyLabels
    }

    private companion object {
        private const val LOCALE_EN = "en_US"
        private const val LAYOUT_QWERTY = "qwerty"
    }
}
