package xyz.xiao6.myboard.ui

import android.os.Bundle
import android.content.Intent
import android.content.ComponentName
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.provider.Settings.Secure
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch
import xyz.xiao6.myboard.R
import xyz.xiao6.myboard.ime.MyBoardImeService
import xyz.xiao6.myboard.manager.LayoutManager
import xyz.xiao6.myboard.manager.SubtypeManager
import xyz.xiao6.myboard.model.LocaleLayoutProfile
import xyz.xiao6.myboard.store.SettingsStore
import xyz.xiao6.myboard.ui.theme.MyBoardTheme
import java.util.Locale

/** 首次安装引导（Setup Wizard Activity）。 */
class SetupActivity : AppCompatActivity() {
    private lateinit var prefs: SettingsStore
    private lateinit var subtypeManager: SubtypeManager
    private lateinit var layoutManager: LayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = SettingsStore(this)
        subtypeManager = SubtypeManager(this).loadAll()
        layoutManager = LayoutManager(this).loadAllFromAssets()

        setContent {
            MyBoardTheme {
                OnboardingWizard(
                    prefs = prefs,
                    subtypeManager = subtypeManager,
                    layoutManager = layoutManager,
                    isImeEnabled = { isMyBoardEnabled() },
                    isImeSelected = { isMyBoardSelectedAsDefault() },
                    openImeSettings = { openImeSettings() },
                    showImePicker = { showImePicker() },
                    finishActivity = {
                        startActivity(Intent(this, SettingsActivity::class.java))
                        finish()
                    },
                )
            }
        }
    }

    private fun isMyBoardEnabled(): Boolean {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager ?: return false
        val myId = resolveMyBoardImeId(imm) ?: return false
        return imm.enabledInputMethodList.any { it.id == myId }
    }

    private fun isMyBoardSelectedAsDefault(): Boolean {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager ?: return false
        val myId = resolveMyBoardImeId(imm) ?: return false
        val current =
            Secure.getString(contentResolver, Secure.DEFAULT_INPUT_METHOD)?.trim().orEmpty()
        return current == myId
    }

    private fun resolveMyBoardImeId(imm: InputMethodManager): String? {
        val expectedPackage = packageName
        val expectedServiceName = MyBoardImeService::class.java.name
        val info =
            imm.inputMethodList.firstOrNull { imi ->
                val si = imi.serviceInfo
                val className = if (si.name.startsWith(".")) si.packageName + si.name else si.name
                si.packageName == expectedPackage && className == expectedServiceName
            }
        return info?.id
    }

    private fun openImeSettings() {
        runCatching {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }.onFailure {
            Toast.makeText(
                this,
                getString(R.string.onboarding_error_open_settings_failed),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showImePicker() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
        if (imm == null) {
            Toast.makeText(
                this,
                getString(R.string.onboarding_error_imm_unavailable),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        imm.showInputMethodPicker()
    }
}

private enum class WizardStep {
    ENABLE_IME,
    PICK_IME,
    LOCALE,
    LAYOUT,
}

@OptIn(ExperimentalPagerApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun OnboardingWizard(
    prefs: SettingsStore,
    subtypeManager: SubtypeManager,
    layoutManager: LayoutManager,
    isImeEnabled: () -> Boolean,
    isImeSelected: () -> Boolean,
    openImeSettings: () -> Unit,
    showImePicker: () -> Unit,
    finishActivity: () -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val steps = remember { WizardStep.entries.toList() }
    val pagerState = rememberPagerState()
    val scope = rememberCoroutineScope()

    var imeEnabled by remember { mutableStateOf(false) }
    var imeSelected by remember { mutableStateOf(false) }
    var selectedLocaleTag by remember { mutableStateOf<String?>(null) }
    var selectedLayoutId by remember { mutableStateOf<String?>(null) }
    var enabledLayoutIds by remember { mutableStateOf<List<String>>(emptyList()) }

    fun refreshImeState() {
        imeEnabled = isImeEnabled()
        imeSelected = isImeSelected()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        refreshImeState()
        val obs =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) refreshImeState()
            }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    DisposableEffect(context) {
        val resolver = context.contentResolver
        val handler = Handler(Looper.getMainLooper())
        val observer =
            object : ContentObserver(handler) {
                override fun onChange(selfChange: Boolean) {
                    refreshImeState()
                }
            }

        resolver.registerContentObserver(
            Secure.getUriFor(Secure.DEFAULT_INPUT_METHOD),
            false,
            observer,
        )
        resolver.registerContentObserver(
            Secure.getUriFor(Secure.ENABLED_INPUT_METHODS),
            false,
            observer,
        )
        onDispose { resolver.unregisterContentObserver(observer) }
    }

    fun ensureInitialSelections() {
        if (selectedLocaleTag.isNullOrBlank()) {
            selectedLocaleTag =
                prefs.userLocaleTag
                    ?: subtypeManager.resolve(Locale.getDefault())?.localeTag
                    ?: subtypeManager.listAll().firstOrNull()?.localeTag
        }
        val localeTag = selectedLocaleTag ?: return
        val profile = subtypeManager.get(localeTag) ?: return
        val enabled =
            prefs.getEnabledLayoutIds(localeTag).takeIf { it.isNotEmpty() }
                ?: profile.layoutIds
        enabledLayoutIds = enabled.filter { it in profile.layoutIds }

        val preferred =
            prefs.getPreferredLayoutId(localeTag)
                ?.takeIf { it in enabledLayoutIds }
                ?: profile.defaultLayoutId?.takeIf { it in enabledLayoutIds }
                ?: enabledLayoutIds.firstOrNull()
        selectedLayoutId = preferred
        prefs.userLocaleTag = localeTag
        prefs.setEnabledLocaleTags(listOf(localeTag))
        prefs.setEnabledLayoutIds(localeTag, enabledLayoutIds)
        if (!preferred.isNullOrBlank()) prefs.setPreferredLayoutId(localeTag, preferred)
    }

    DisposableEffect(Unit) {
        ensureInitialSelections()
        onDispose {}
    }

    LaunchedEffect(selectedLocaleTag) {
        val localeTag = selectedLocaleTag ?: return@LaunchedEffect
        prefs.userLocaleTag = localeTag
        prefs.setEnabledLocaleTags(listOf(localeTag))
        val profile = subtypeManager.get(localeTag) ?: return@LaunchedEffect
        val enabled =
            prefs.getEnabledLayoutIds(localeTag).takeIf { it.isNotEmpty() }
                ?: profile.layoutIds
        enabledLayoutIds = enabled.filter { it in profile.layoutIds }
        prefs.setEnabledLayoutIds(localeTag, enabledLayoutIds)

        val preferred =
            prefs.getPreferredLayoutId(localeTag)
                ?.takeIf { it in enabledLayoutIds }
                ?: profile.defaultLayoutId?.takeIf { it in enabledLayoutIds }
                ?: enabledLayoutIds.firstOrNull()
        selectedLayoutId = preferred
        if (!preferred.isNullOrBlank()) prefs.setPreferredLayoutId(localeTag, preferred)
    }

    fun canGoNext(step: WizardStep): Boolean {
        return when (step) {
            WizardStep.ENABLE_IME -> imeEnabled
            WizardStep.PICK_IME -> imeSelected
            WizardStep.LOCALE -> !selectedLocaleTag.isNullOrBlank()
            WizardStep.LAYOUT -> !selectedLocaleTag.isNullOrBlank() && !selectedLayoutId.isNullOrBlank()
        }
    }

    fun goNextOrFinish() {
        val step = steps[pagerState.currentPage]
        if (!canGoNext(step)) return
        if (step == WizardStep.LAYOUT) {
            prefs.onboardingCompleted = true
            finishActivity()
            return
        }
        val next = (pagerState.currentPage + 1).coerceAtMost(steps.lastIndex)
        scope.launch { pagerState.animateScrollToPage(next) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            R.string.onboarding_step_indicator,
                            pagerState.currentPage + 1,
                            steps.size,
                        ),
                    )
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            HorizontalPager(
                count = steps.size,
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier.weight(1f),
            ) { page ->
                when (steps[page]) {
                    WizardStep.ENABLE_IME -> StepEnableIme(
                        enabled = imeEnabled,
                        onOpenSettings = openImeSettings,
                    )

                    WizardStep.PICK_IME -> StepPickIme(
                        selected = imeSelected,
                        onPickIme = showImePicker,
                    )

                    WizardStep.LOCALE -> StepSelectLocale(
                        enabled = imeEnabled,
                        selected = imeSelected,
                        profiles = subtypeManager.listAll()
                            .filter { it.enabled && it.localeTag.isNotBlank() },
                        selectedLocaleTag = selectedLocaleTag,
                        onSelect = { tag -> selectedLocaleTag = tag },
                    )

                    WizardStep.LAYOUT -> {
                        val localeTag = selectedLocaleTag
                        val profile = localeTag?.let { subtypeManager.get(it) }
                        StepSelectLayout(
                            localeTag = localeTag,
                            profile = profile,
                            layoutManager = layoutManager,
                            enabledLayoutIds = enabledLayoutIds,
                            onSelect = { layoutId ->
                                val tag = localeTag ?: return@StepSelectLayout
                                val ordered = profile?.layoutIds.orEmpty()
                                val current = enabledLayoutIds.toMutableSet()
                                if (layoutId in current) current.remove(layoutId) else current.add(layoutId)
                                val nextEnabled = ordered.filter { it in current }.distinct()
                                enabledLayoutIds = nextEnabled
                                prefs.setEnabledLayoutIds(tag, nextEnabled)

                                val preferred = prefs.getPreferredLayoutId(tag)
                                val nextPreferred =
                                    preferred?.takeIf { it in nextEnabled }
                                        ?: nextEnabled.firstOrNull()
                                selectedLayoutId = nextPreferred
                                prefs.setPreferredLayoutId(tag, nextPreferred)
                            },
                        )
                    }
                }
            }

            val step = steps[pagerState.currentPage]
            val nextEnabled = canGoNext(step)

            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = {
                        val prev = (pagerState.currentPage - 1).coerceAtLeast(0)
                        scope.launch { pagerState.animateScrollToPage(prev) }
                    },
                    enabled = pagerState.currentPage > 0,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.onboarding_back))
                }
                Button(
                    onClick = {
                        if (!nextEnabled) {
                            val msg = when (step) {
                                WizardStep.ENABLE_IME -> R.string.onboarding_error_enable_first
                                WizardStep.PICK_IME -> R.string.onboarding_error_pick_first
                                WizardStep.LOCALE -> R.string.onboarding_error_select_language
                                WizardStep.LAYOUT -> R.string.onboarding_error_select_layout
                            }
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = context.getString(
                                        msg
                                    )
                                )
                            }
                            return@Button
                        }
                        goNextOrFinish()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        if (step == WizardStep.LAYOUT) stringResource(R.string.onboarding_finish)
                        else stringResource(R.string.onboarding_next),
                    )
                }
            }
        }
    }
}

@Composable
private fun StepEnableIme(
    enabled: Boolean,
    onOpenSettings: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = stringResource(R.string.onboarding_enable_title),
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(
                R.string.onboarding_status_enabled,
                if (enabled) stringResource(R.string.common_yes) else stringResource(R.string.common_no),
            ),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = stringResource(R.string.onboarding_enable_desc))
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onOpenSettings) {
            Text(stringResource(R.string.onboarding_open_ime_settings))
        }
    }
}

@Composable
private fun StepPickIme(
    selected: Boolean,
    onPickIme: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = stringResource(R.string.onboarding_pick_title),
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(
                R.string.onboarding_status_selected,
                if (selected) stringResource(R.string.common_yes) else stringResource(R.string.common_no),
            ),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = stringResource(R.string.onboarding_pick_desc))
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onPickIme) {
            Text(stringResource(R.string.onboarding_show_ime_picker))
        }
    }
}

@Composable
private fun StepSelectLocale(
    enabled: Boolean,
    selected: Boolean,
    profiles: List<LocaleLayoutProfile>,
    selectedLocaleTag: String?,
    onSelect: (String) -> Unit,
) {
    val canSelect = enabled && selected
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel =
        selectedLocaleTag
            ?.takeIf { it.isNotBlank() }
            ?.let { formatLocaleLabelComposable(it) }
            ?: ""
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = stringResource(R.string.onboarding_language_title),
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(
                R.string.onboarding_status_ready_for_language,
                if (enabled) stringResource(R.string.common_yes) else stringResource(R.string.common_no),
                if (selected) stringResource(R.string.common_yes) else stringResource(R.string.common_no),
            ),
        )
        if (!canSelect) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = stringResource(R.string.onboarding_language_locked_hint))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = stringResource(R.string.onboarding_language_dropdown_label))
        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { if (canSelect) expanded = true },
                enabled = canSelect,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = selectedLabel.ifBlank { stringResource(R.string.onboarding_language_dropdown_placeholder) },
                        modifier = Modifier.weight(1f),
                    )
                    Text(text = "▼")
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(),
            ) {
                val sorted = profiles.sortedBy { it.localeTag }
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(scrollState),
                ) {
                    sorted.forEach { p ->
                        val tag = p.localeTag
                        DropdownMenuItem(
                            text = { Text(formatLocaleLabelComposable(tag)) },
                            onClick = {
                                expanded = false
                                onSelect(tag)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepSelectLayout(
    localeTag: String?,
    profile: LocaleLayoutProfile?,
    layoutManager: LayoutManager,
    enabledLayoutIds: List<String>,
    onSelect: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = stringResource(R.string.onboarding_layout_title),
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (localeTag.isNullOrBlank() || profile == null) {
            Text(text = stringResource(R.string.onboarding_layout_missing_language))
            return
        }

        Text(
            text = stringResource(
                R.string.onboarding_layout_for_language,
                formatLocaleLabelComposable(localeTag)
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = stringResource(R.string.onboarding_layout_multi_select_hint))
        Spacer(modifier = Modifier.height(16.dp))

        val ids = profile.layoutIds.distinct()
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(ids) { id ->
                val layoutName =
                    runCatching { layoutManager.getLayout(id).name }.getOrNull()
                        ?.takeIf { it.isNotBlank() } ?: id
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = id in enabledLayoutIds,
                        onCheckedChange = { onSelect(id) },
                    )
                    Text(text = stringResource(R.string.onboarding_layout_item, layoutName, id))
                }
            }
        }
    }
}

@Composable
private fun formatLocaleLabelComposable(localeTag: String): String {
    val tag = normalizeLocaleTagComposable(localeTag)
    val display = Locale.forLanguageTag(tag).getDisplayName(Locale.getDefault()).ifBlank { tag }
    return "$display ($tag)"
}

@Composable
private fun normalizeLocaleTagComposable(tag: String?): String {
    val raw = tag?.trim().orEmpty().replace('_', '-')
    val parts = raw.split('-').filter { it.isNotBlank() }
    if (parts.isEmpty()) return ""
    val language = parts[0].lowercase(Locale.ROOT)
    val region = parts.getOrNull(1)?.uppercase(Locale.ROOT)
    return if (region.isNullOrBlank()) language else "$language-$region"
}
