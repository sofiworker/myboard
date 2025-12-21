package xyz.xiao6.myboard.ui

import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.provider.Settings
import android.provider.Settings.Secure
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import xyz.xiao6.myboard.R
import xyz.xiao6.myboard.dictionary.UserDictionaryStore
import xyz.xiao6.myboard.dictionary.format.DictionaryEntry
import xyz.xiao6.myboard.ime.MyBoardImeService
import xyz.xiao6.myboard.manager.DictionaryManager
import xyz.xiao6.myboard.manager.LayoutManager
import xyz.xiao6.myboard.manager.SubtypeManager
import xyz.xiao6.myboard.manager.ToolbarManager
import xyz.xiao6.myboard.model.LocaleLayoutProfile
import xyz.xiao6.myboard.model.DictionarySpec
import xyz.xiao6.myboard.store.SettingsStore
import xyz.xiao6.myboard.util.KeyboardSizeConstraints
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 应用主入口：设置页（若未完成引导会自动跳转到 SetupActivity）。 */
class SettingsActivity : AppCompatActivity() {
    private lateinit var prefs: SettingsStore
    private lateinit var subtypeManager: SubtypeManager
    private lateinit var layoutManager: LayoutManager
    private lateinit var toolbarManager: ToolbarManager
    private lateinit var dictionaryManager: DictionaryManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = SettingsStore(this)
        subtypeManager = SubtypeManager(this).loadAll()
        layoutManager = LayoutManager(this).loadAllFromAssets()
        toolbarManager = ToolbarManager(this).loadAllFromAssets()
        dictionaryManager = DictionaryManager(this).loadAll()

        if (!prefs.onboardingCompleted) {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }

        setContent {
            MaterialTheme {
                SettingsScreen(
                    prefs = prefs,
                    subtypeManager = subtypeManager,
                    layoutManager = layoutManager,
                    toolbarManager = toolbarManager,
                    dictionaryManager = dictionaryManager,
                    imeEnabled = { isMyBoardEnabled() },
                    imeSelected = { isMyBoardSelectedAsDefault() },
                    onOpenImeSettings = { openImeSettings() },
                    onShowImePicker = { showImePicker() },
                    onResetSetup = {
                        prefs.clearAll()
                        startActivity(Intent(this, SetupActivity::class.java))
                        finish()
                    },
                )
            }
        }
    }

    private fun openImeSettings() {
        runCatching {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }.onFailure {
            Toast.makeText(this, getString(R.string.onboarding_error_open_settings_failed), Toast.LENGTH_SHORT).show()
        }
    }

    private fun showImePicker() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
        if (imm == null) {
            Toast.makeText(this, getString(R.string.onboarding_error_imm_unavailable), Toast.LENGTH_SHORT).show()
            return
        }
        imm.showInputMethodPicker()
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

    private fun isMyBoardEnabled(): Boolean {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager ?: return false
        val myId = resolveMyBoardImeId(imm) ?: return false
        return imm.enabledInputMethodList.any { it.id == myId }
    }

    private fun isMyBoardSelectedAsDefault(): Boolean {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager ?: return false
        val myId = resolveMyBoardImeId(imm) ?: return false
        val current = Secure.getString(contentResolver, Secure.DEFAULT_INPUT_METHOD)?.trim().orEmpty()
        return current == myId
    }
}

private sealed interface SettingsRoute {
    data object Main : SettingsRoute
    data object LanguageLayout : SettingsRoute
    data object Feedback : SettingsRoute
    data object InputBehavior : SettingsRoute
    data object Toolbar : SettingsRoute
    data object Dictionaries : SettingsRoute
    data object Appearance : SettingsRoute
    data object KeyboardSize : SettingsRoute
}

private enum class KeyboardSizeHandle {
    NONE,
    LEFT,
    RIGHT,
    TOP,
    BOTTOM,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    prefs: SettingsStore,
    subtypeManager: SubtypeManager,
    layoutManager: LayoutManager,
    toolbarManager: ToolbarManager,
    dictionaryManager: DictionaryManager,
    imeEnabled: () -> Boolean,
    imeSelected: () -> Boolean,
    onOpenImeSettings: () -> Unit,
    onShowImePicker: () -> Unit,
    onResetSetup: () -> Unit,
) {
    val context = LocalContext.current
    var route by remember { mutableStateOf<SettingsRoute>(SettingsRoute.Main) }

    var imeEnabledState by remember { mutableStateOf(false) }
    var imeSelectedState by remember { mutableStateOf(false) }
    fun refreshImeState() {
        imeEnabledState = imeEnabled()
        imeSelectedState = imeSelected()
    }

    DisposableEffect(context) {
        refreshImeState()
        val resolver = context.contentResolver
        val handler = Handler(Looper.getMainLooper())
        val observer =
            object : ContentObserver(handler) {
                override fun onChange(selfChange: Boolean) {
                    refreshImeState()
                }
            }
        resolver.registerContentObserver(Secure.getUriFor(Secure.DEFAULT_INPUT_METHOD), false, observer)
        resolver.registerContentObserver(Secure.getUriFor(Secure.ENABLED_INPUT_METHODS), false, observer)
        onDispose { resolver.unregisterContentObserver(observer) }
    }

    val titleRes = when (route) {
        SettingsRoute.Main -> R.string.settings_title
        SettingsRoute.LanguageLayout -> R.string.settings_language_layout
        SettingsRoute.Feedback -> R.string.settings_feedback
        SettingsRoute.InputBehavior -> R.string.settings_input_behavior
        SettingsRoute.Toolbar -> R.string.settings_toolbar
        SettingsRoute.Dictionaries -> R.string.settings_dictionaries
        SettingsRoute.Appearance -> R.string.settings_appearance
        SettingsRoute.KeyboardSize -> R.string.settings_keyboard_size
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(titleRes)) },
                navigationIcon = {
                    if (route != SettingsRoute.Main) {
                        IconButton(onClick = { route = SettingsRoute.Main }) {
                            Text("←")
                        }
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets.systemBars,
    ) { padding ->
        when (route) {
            SettingsRoute.Main -> SettingsMainList(
                modifier = Modifier.fillMaxSize().padding(padding),
                prefs = prefs,
                imeEnabled = imeEnabledState,
                imeSelected = imeSelectedState,
                onOpenImeSettings = onOpenImeSettings,
                onShowImePicker = onShowImePicker,
                onOpenLanguageLayout = { route = SettingsRoute.LanguageLayout },
                onOpenFeedback = { route = SettingsRoute.Feedback },
                onOpenInputBehavior = { route = SettingsRoute.InputBehavior },
                onOpenToolbar = { route = SettingsRoute.Toolbar },
                onOpenAppearance = { route = SettingsRoute.Appearance },
                onOpenDictionaries = { route = SettingsRoute.Dictionaries },
                onOpenKeyboardSize = { route = SettingsRoute.KeyboardSize },
                onResetSetup = onResetSetup,
            )

            SettingsRoute.LanguageLayout -> LanguageLayoutSettings(
                modifier = Modifier.fillMaxSize().padding(padding),
                prefs = prefs,
                subtypeManager = subtypeManager,
                layoutManager = layoutManager,
            )

            SettingsRoute.Feedback -> FeedbackSettings(
                modifier = Modifier.fillMaxSize().padding(padding),
                prefs = prefs,
            )

            SettingsRoute.InputBehavior -> InputBehaviorSettings(
                modifier = Modifier.fillMaxSize().padding(padding),
                prefs = prefs,
            )

            SettingsRoute.Toolbar -> ToolbarSettings(
                modifier = Modifier.fillMaxSize().padding(padding),
                prefs = prefs,
                toolbarManager = toolbarManager,
            )

            SettingsRoute.Dictionaries -> DictionariesSettings(
                modifier = Modifier.fillMaxSize().padding(padding),
                prefs = prefs,
                subtypeManager = subtypeManager,
                dictionaryManager = dictionaryManager,
            )

            SettingsRoute.Appearance -> PlaceholderSettingsPage(
                modifier = Modifier.fillMaxSize().padding(padding),
                textRes = R.string.settings_placeholder_appearance,
            )

            SettingsRoute.KeyboardSize -> KeyboardSizeSettings(
                modifier = Modifier.fillMaxSize().padding(padding),
                prefs = prefs,
                layoutManager = layoutManager,
            )
        }
    }
}

@Composable
private fun SettingsMainList(
    modifier: Modifier,
    prefs: SettingsStore,
    imeEnabled: Boolean,
    imeSelected: Boolean,
    onOpenImeSettings: () -> Unit,
    onShowImePicker: () -> Unit,
    onOpenLanguageLayout: () -> Unit,
    onOpenFeedback: () -> Unit,
    onOpenInputBehavior: () -> Unit,
    onOpenToolbar: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenDictionaries: () -> Unit,
    onOpenKeyboardSize: () -> Unit,
    onResetSetup: () -> Unit,
) {
    val localeTag = prefs.userLocaleTag
    val preferredLayoutId = localeTag?.let { prefs.getPreferredLayoutId(it) }

    LazyColumn(modifier = modifier) {
        item {
            SectionHeader(textRes = R.string.settings_section_input_method)
        }
        item {
            SettingItem(
                titleRes = R.string.settings_open_ime_settings,
                summary = stringResource(
                    R.string.settings_status_ime_enabled_selected,
                    if (imeEnabled) stringResource(R.string.common_yes) else stringResource(R.string.common_no),
                    if (imeSelected) stringResource(R.string.common_yes) else stringResource(R.string.common_no),
                ),
                onClick = onOpenImeSettings,
            )
        }
        item {
            SettingItem(
                titleRes = R.string.settings_show_ime_picker,
                summaryRes = R.string.settings_show_ime_picker_desc,
                onClick = onShowImePicker,
            )
        }
        item { HorizontalDivider() }

        item { SectionHeader(textRes = R.string.settings_section_input) }
        item {
            SettingItem(
                titleRes = R.string.settings_language_layout,
                summary = stringResource(
                    R.string.settings_status_language_layout,
                    localeTag ?: "-",
                    preferredLayoutId ?: "-",
                ),
                onClick = onOpenLanguageLayout,
            )
        }
        item {
            SettingItem(
                titleRes = R.string.settings_feedback,
                summary = stringResource(
                    R.string.settings_feedback_summary,
                    prefs.clickSoundVolumePercent,
                    if (prefs.vibrationFollowSystem) stringResource(R.string.settings_vibration_follow_system_short)
                    else "${prefs.vibrationStrengthPercent}%",
                ),
                onClick = onOpenFeedback,
            )
        }
        item {
            val summary =
                if (prefs.clearInputAfterTokenClear) {
                    stringResource(
                        R.string.settings_input_behavior_summary_enabled,
                        prefs.clearInputAfterTokenClearDelayMs,
                    )
                } else {
                    stringResource(R.string.settings_input_behavior_summary_disabled)
                }
            SettingItem(
                titleRes = R.string.settings_input_behavior,
                summary = summary,
                onClick = onOpenInputBehavior,
            )
        }
        item {
            val maxCount = prefs.toolbarMaxVisibleCount
            val countLabel =
                if (maxCount <= 0) stringResource(R.string.settings_toolbar_limit_unlimited)
                else stringResource(R.string.settings_toolbar_limit_fixed, maxCount)
            SettingItem(
                titleRes = R.string.settings_toolbar,
                summary = stringResource(R.string.settings_toolbar_summary, countLabel),
                onClick = onOpenToolbar,
            )
        }
        item {
            SettingItem(
                titleRes = R.string.settings_dictionaries,
                summaryRes = R.string.settings_dictionaries_desc,
                onClick = onOpenDictionaries,
            )
        }
        item {
            SettingItem(
                titleRes = R.string.settings_appearance,
                summaryRes = R.string.settings_appearance_desc,
                onClick = onOpenAppearance,
            )
        }

        item { HorizontalDivider() }
        item { SectionHeader(textRes = R.string.settings_section_advanced) }
        item {
            SettingItem(
                titleRes = R.string.settings_keyboard_size,
                summaryRes = R.string.settings_keyboard_size_desc,
                onClick = onOpenKeyboardSize,
            )
        }
        item {
            SettingItem(
                titleRes = R.string.settings_reset,
                summaryRes = R.string.settings_reset_desc,
                onClick = onResetSetup,
            )
        }
    }
}

@Composable
private fun SectionHeader(textRes: Int) {
    Text(
        text = stringResource(textRes),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun SettingItem(
    titleRes: Int,
    summaryRes: Int? = null,
    summary: String? = null,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(stringResource(titleRes)) },
        supportingContent = {
            val s = summary ?: summaryRes?.let { stringResource(it) }
            if (!s.isNullOrBlank()) Text(s)
        },
        trailingContent = { Text("›") },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
}

@Composable
private fun PlaceholderSettingsPage(
    modifier: Modifier,
    textRes: Int,
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(text = stringResource(textRes))
    }
}

@Composable
private fun DictionariesSettings(
    modifier: Modifier,
    prefs: SettingsStore,
    subtypeManager: SubtypeManager,
    dictionaryManager: DictionaryManager,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userStore = remember { UserDictionaryStore(context) }
    var refreshToken by remember { mutableStateOf(0) }

    DisposableEffect(prefs) {
        val listener = prefs.addOnChangeListener { refreshToken += 1 }
        onDispose { prefs.removeOnChangeListener(listener) }
    }

    val localeTags = remember(refreshToken) { resolveDictionaryLocaleTags(prefs, subtypeManager, dictionaryManager) }
    var selectedLocaleTag by remember(localeTags) { mutableStateOf(localeTags.firstOrNull().orEmpty()) }
    if (selectedLocaleTag.isNotBlank() && selectedLocaleTag !in localeTags) {
        selectedLocaleTag = localeTags.firstOrNull().orEmpty()
    }

    val formatOptions = remember {
        listOf(
            DictionaryFormatOption(
                formatId = "rime_dict_yaml",
                labelRes = R.string.settings_dictionaries_format_rime_yaml,
                mimeTypes = arrayOf("text/*", "application/octet-stream"),
            ),
        )
    }
    val schemeOptions = remember {
        listOf(
            CodeSchemeOption(
                codeScheme = "PINYIN_FULL",
                labelRes = R.string.settings_dictionaries_scheme_pinyin_full,
                kind = "PINYIN",
                core = "PINYIN_CORE",
                variant = "quanpin",
                defaultLayoutIds = emptyList(),
            ),
            CodeSchemeOption(
                codeScheme = "PINYIN_T9",
                labelRes = R.string.settings_dictionaries_scheme_pinyin_t9,
                kind = "PINYIN",
                core = "PINYIN_CORE",
                variant = "t9",
                defaultLayoutIds = listOf("t9"),
            ),
        )
    }

    var showCreateDialog by remember { mutableStateOf(false) }
    var createName by remember { mutableStateOf("") }
    var createScheme by remember { mutableStateOf(schemeOptions.first()) }

    var showImportDialog by remember { mutableStateOf(false) }
    var importFormat by remember { mutableStateOf(formatOptions.first()) }
    var importScheme by remember { mutableStateOf(schemeOptions.first()) }
    var importName by remember { mutableStateOf("") }
    var importDictionaryId by remember { mutableStateOf("") }
    var importFileUri by remember { mutableStateOf<Uri?>(null) }

    var pendingDeleteSpec by remember { mutableStateOf<DictionarySpec?>(null) }
    var pendingEntrySpec by remember { mutableStateOf<DictionarySpec?>(null) }
    var entryWord by remember { mutableStateOf("") }
    var entryCode by remember { mutableStateOf("") }
    var entryWeight by remember { mutableStateOf("") }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            importFileUri = uri
            val display = resolveDisplayName(context, uri)
            if (importName.isBlank() && !display.isNullOrBlank()) importName = display
            if (importDictionaryId.isBlank()) {
                importDictionaryId = generateDictionaryId(display ?: "import", selectedLocaleTag, dictionaryManager)
            }
        }
    }

    fun refreshDictionaries() {
        scope.launch {
            withContext(Dispatchers.IO) { dictionaryManager.reload() }
            refreshToken += 1
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(text = stringResource(R.string.settings_dictionaries_create_title)) },
            text = {
                Column {
                    TextField(
                        value = createName,
                        onValueChange = { createName = it },
                        label = { Text(stringResource(R.string.settings_dictionaries_name)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.settings_dictionaries_code_scheme),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    schemeOptions.forEach { option ->
                        val selected = option.codeScheme == createScheme.codeScheme
                        ListItem(
                            headlineContent = { Text(text = stringResource(option.labelRes)) },
                            trailingContent = { if (selected) Text("✓") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { createScheme = option },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val normalizedTag = normalizeLocaleTag(selectedLocaleTag)
                        val dictName = createName.trim().ifBlank { null }
                        val dictionaryId = generateDictionaryId(dictName ?: "custom", normalizedTag, dictionaryManager)
                        val spec = DictionarySpec(
                            dictionaryId = dictionaryId,
                            name = dictName,
                            localeTags = listOf(normalizedTag),
                            layoutIds = createScheme.defaultLayoutIds,
                            assetPath = null,
                            filePath = null,
                            dictionaryVersion = "1.0.0",
                            codeScheme = createScheme.codeScheme,
                            kind = createScheme.kind,
                            core = createScheme.core,
                            variant = createScheme.variant,
                            enabled = true,
                            priority = 10,
                        )
                        scope.launch(Dispatchers.IO) {
                            userStore.createCustomDictionary(spec)
                        }.invokeOnCompletion { refreshDictionaries() }
                        showCreateDialog = false
                    },
                ) {
                    Text(text = stringResource(R.string.settings_dictionaries_create_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text(text = stringResource(R.string.settings_dictionaries_cancel))
                }
            },
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text(text = stringResource(R.string.settings_dictionaries_import_title)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.settings_dictionaries_format),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    formatOptions.forEach { option ->
                        val selected = option.formatId == importFormat.formatId
                        ListItem(
                            headlineContent = { Text(text = stringResource(option.labelRes)) },
                            trailingContent = { if (selected) Text("✓") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { importFormat = option },
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.settings_dictionaries_code_scheme),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    schemeOptions.forEach { option ->
                        val selected = option.codeScheme == importScheme.codeScheme
                        ListItem(
                            headlineContent = { Text(text = stringResource(option.labelRes)) },
                            trailingContent = { if (selected) Text("✓") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { importScheme = option },
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = importName,
                        onValueChange = { importName = it },
                        label = { Text(stringResource(R.string.settings_dictionaries_name)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = importDictionaryId,
                        onValueChange = { importDictionaryId = it },
                        label = { Text(stringResource(R.string.settings_dictionaries_id)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { importLauncher.launch(importFormat.mimeTypes) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = stringResource(R.string.settings_dictionaries_choose_file))
                    }
                    val fileLabel = importFileUri?.let { resolveDisplayName(context, it) }
                    if (!fileLabel.isNullOrBlank()) {
                        Text(
                            text = stringResource(R.string.settings_dictionaries_selected_file, fileLabel),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val uri = importFileUri ?: return@TextButton
                        val normalizedTag = normalizeLocaleTag(selectedLocaleTag)
                        val name = importName.trim().ifBlank { null }
                        val dictId = importDictionaryId.trim().ifBlank {
                            generateDictionaryId(name ?: "import", normalizedTag, dictionaryManager)
                        }
                        val spec = DictionarySpec(
                            dictionaryId = dictId,
                            name = name,
                            localeTags = listOf(normalizedTag),
                            layoutIds = importScheme.defaultLayoutIds,
                            assetPath = null,
                            filePath = null,
                            dictionaryVersion = "1.0.0",
                            codeScheme = importScheme.codeScheme,
                            kind = importScheme.kind,
                            core = importScheme.core,
                            variant = importScheme.variant,
                            enabled = true,
                            priority = 10,
                        )
                        scope.launch(Dispatchers.IO) {
                            val temp = copyUriToTempFile(context, uri)
                            try {
                                userStore.importDictionary(
                                    inputFile = temp,
                                    formatId = importFormat.formatId,
                                    spec = spec,
                                )
                            } finally {
                                temp.delete()
                            }
                        }.invokeOnCompletion {
                            refreshDictionaries()
                        }
                        showImportDialog = false
                    },
                    enabled = importFileUri != null,
                ) {
                    Text(text = stringResource(R.string.settings_dictionaries_import_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text(text = stringResource(R.string.settings_dictionaries_cancel))
                }
            },
        )
    }

    pendingEntrySpec?.let { spec ->
        AlertDialog(
            onDismissRequest = { pendingEntrySpec = null },
            title = { Text(text = stringResource(R.string.settings_dictionaries_add_entry_title)) },
            text = {
                Column {
                    TextField(
                        value = entryWord,
                        onValueChange = { entryWord = it },
                        label = { Text(stringResource(R.string.settings_dictionaries_entry_word)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = entryCode,
                        onValueChange = { entryCode = it },
                        label = { Text(stringResource(R.string.settings_dictionaries_entry_code)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = entryWeight,
                        onValueChange = { entryWeight = it },
                        label = { Text(stringResource(R.string.settings_dictionaries_entry_weight)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                val enabled = entryWord.trim().isNotBlank() && entryCode.trim().isNotBlank()
                TextButton(
                    onClick = {
                        val weight = entryWeight.trim().toIntOrNull() ?: 0
                        scope.launch(Dispatchers.IO) {
                            userStore.appendEntry(
                                spec.dictionaryId,
                                DictionaryEntry(
                                    word = entryWord.trim(),
                                    code = entryCode.trim(),
                                    weight = weight,
                                ),
                            )
                        }.invokeOnCompletion { refreshDictionaries() }
                        entryWord = ""
                        entryCode = ""
                        entryWeight = ""
                        pendingEntrySpec = null
                    },
                    enabled = enabled,
                ) {
                    Text(text = stringResource(R.string.settings_dictionaries_add_entry_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingEntrySpec = null }) {
                    Text(text = stringResource(R.string.settings_dictionaries_cancel))
                }
            },
        )
    }

    pendingDeleteSpec?.let { spec ->
        AlertDialog(
            onDismissRequest = { pendingDeleteSpec = null },
            title = { Text(text = stringResource(R.string.settings_dictionaries_delete_title)) },
            text = { Text(text = stringResource(R.string.settings_dictionaries_delete_message, spec.name ?: spec.dictionaryId)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            userStore.deleteDictionary(spec.dictionaryId)
                        }.invokeOnCompletion {
                            removeDictionaryFromPrefs(prefs, spec)
                            refreshDictionaries()
                        }
                        pendingDeleteSpec = null
                    },
                ) {
                    Text(text = stringResource(R.string.settings_dictionaries_delete_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteSpec = null }) {
                    Text(text = stringResource(R.string.settings_dictionaries_cancel))
                }
            },
        )
    }

    LazyColumn(modifier = modifier) {
        if (localeTags.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.settings_dictionaries_empty),
                    modifier = Modifier.padding(16.dp),
                )
            }
            return@LazyColumn
        }

        item { SectionHeader(textRes = R.string.settings_dictionaries_section_overview) }

        localeTags.forEach { localeTag ->
            val normalizedTag = normalizeLocaleTag(localeTag)
            val locale = Locale.forLanguageTag(normalizedTag)
            val dictionaries = dictionaryManager.findByLocale(locale)
            val dictionaryIds = dictionaries.map { it.dictionaryId }
            val storedEnabled = prefs.getEnabledDictionaryIds(normalizedTag)
            val enabledSet = storedEnabled?.toSet() ?: dictionaryIds.toSet()
            val enabledCount = dictionaryIds.count { it in enabledSet }
            val localeLabel = formatLocaleLabel(normalizedTag)
            val enabledNames =
                dictionaries
                    .filter { it.dictionaryId in enabledSet }
                    .map { it.name?.ifBlank { it.dictionaryId } ?: it.dictionaryId }
            item {
                ListItem(
                    headlineContent = { Text(text = localeLabel) },
                    supportingContent = {
                        Column {
                            Text(
                                text = stringResource(
                                    R.string.settings_dictionaries_enabled_summary,
                                    enabledCount,
                                    dictionaryIds.size,
                                ),
                            )
                            Text(
                                text = stringResource(
                                    R.string.settings_dictionaries_enabled_list,
                                    if (enabledNames.isEmpty()) {
                                        stringResource(R.string.settings_dictionaries_enabled_none)
                                    } else {
                                        enabledNames.joinToString(", ")
                                    },
                                ),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    },
                    trailingContent = {
                        if (storedEnabled != null) {
                            OutlinedButton(onClick = { prefs.clearEnabledDictionaryIds(normalizedTag) }) {
                                Text(text = stringResource(R.string.settings_dictionaries_reset_default))
                            }
                        }
                    },
                )
            }

            item { HorizontalDivider() }
        }

        item { SectionHeader(textRes = R.string.settings_dictionaries_section_manage) }
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedButton(
                    onClick = {
                        createName = ""
                        createScheme = schemeOptions.first()
                        showCreateDialog = true
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = stringResource(R.string.settings_dictionaries_create_action))
                }
                Spacer(modifier = Modifier.size(12.dp))
                OutlinedButton(
                    onClick = {
                        importFormat = formatOptions.first()
                        importScheme = schemeOptions.first()
                        importName = ""
                        importDictionaryId = ""
                        importFileUri = null
                        showImportDialog = true
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = stringResource(R.string.settings_dictionaries_import_action))
                }
            }
        }
        if (localeTags.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.settings_dictionaries_choose_language),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(localeTags, key = { "manage_lang:$it" }) { tag ->
                val isSelected = tag == selectedLocaleTag
                ListItem(
                    headlineContent = { Text(text = formatLocaleLabel(tag)) },
                    trailingContent = { if (isSelected) Text("✓") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedLocaleTag = tag },
                )
            }
            item { HorizontalDivider() }
        }

        if (selectedLocaleTag.isNotBlank()) {
            val normalizedTag = normalizeLocaleTag(selectedLocaleTag)
            val locale = Locale.forLanguageTag(normalizedTag)
            val dictionaries = dictionaryManager.findByLocale(locale)
            val dictionaryIds = dictionaries.map { it.dictionaryId }
            val storedEnabled = prefs.getEnabledDictionaryIds(normalizedTag)
            val enabledSet = storedEnabled?.toSet() ?: dictionaryIds.toSet()
            val enabledLayoutIds = prefs.getEnabledLayoutIds(normalizedTag)

            if (dictionaries.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.settings_dictionaries_none_for_language),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(dictionaries, key = { "${normalizedTag}:${it.dictionaryId}" }) { spec ->
                    val name = spec.name?.ifBlank { null } ?: spec.dictionaryId
                    val isEnabled = spec.dictionaryId in enabledSet
                    val layoutLabel =
                        spec.layoutIds.takeIf { it.isNotEmpty() }?.joinToString(", ")
                    val allowEnable = isDictionaryEnableAllowed(spec, enabledLayoutIds)
                    val isUserDict = userStore.isUserDictionary(spec)
                    val canEditEntries = isUserDict && userStore.hasSourceFile(spec.dictionaryId)
                    val onToggle = { next: Boolean ->
                        if (allowEnable) {
                            val working = (storedEnabled ?: dictionaryIds).toMutableSet()
                            if (next) working.add(spec.dictionaryId) else working.remove(spec.dictionaryId)
                            prefs.setEnabledDictionaryIds(normalizedTag, working.toList())
                        }
                    }

                    ListItem(
                        headlineContent = { Text(text = name) },
                        supportingContent = {
                            Column {
                                Text(text = stringResource(R.string.settings_dictionaries_item_id, spec.dictionaryId))
                                if (!layoutLabel.isNullOrBlank()) {
                                    Text(text = stringResource(R.string.settings_dictionaries_item_layouts, layoutLabel))
                                }
                                if (!allowEnable) {
                                    Text(text = stringResource(R.string.settings_dictionaries_not_available))
                                }
                                if (spec.isDefault) {
                                    Text(text = stringResource(R.string.settings_default))
                                }
                                if (isUserDict) {
                                    Row(modifier = Modifier.padding(top = 6.dp)) {
                                        if (canEditEntries) {
                                            TextButton(onClick = {
                                                entryWord = ""
                                                entryCode = ""
                                                entryWeight = ""
                                                pendingEntrySpec = spec
                                            }) {
                                                Text(text = stringResource(R.string.settings_dictionaries_add_entry_action))
                                            }
                                        }
                                        TextButton(onClick = { pendingDeleteSpec = spec }) {
                                            Text(text = stringResource(R.string.settings_dictionaries_delete_action))
                                        }
                                    }
                                }
                            }
                        },
                        trailingContent = {
                            Switch(
                                checked = isEnabled,
                                onCheckedChange = { onToggle(it) },
                                enabled = allowEnable,
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = allowEnable) { onToggle(!isEnabled) },
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyboardSizeSettings(
    modifier: Modifier,
    prefs: SettingsStore,
    layoutManager: LayoutManager,
) {
    val defaultLayout = remember { layoutManager.getDefaultLayout(Locale.getDefault()) }
    val defaultWidthRatio = defaultLayout.totalWidthRatio.coerceIn(0.5f, 1.0f)
    val defaultHeightRatio = defaultLayout.totalHeightRatio.coerceIn(0.15f, 0.8f)
    val defaultWidthOffset = defaultLayout.totalWidthDpOffset
    val defaultHeightOffset = defaultLayout.totalHeightDpOffset

    // Keep ratio as-is (IME resize currently adjusts dp offset only).
    var widthRatio by remember { mutableStateOf(prefs.globalKeyboardWidthRatio ?: defaultWidthRatio) }
    var heightRatio by remember { mutableStateOf(prefs.globalKeyboardHeightRatio ?: defaultHeightRatio) }
    var widthOffset by remember { mutableStateOf(prefs.globalKeyboardWidthDpOffset ?: defaultWidthOffset) }
    var heightOffset by remember { mutableStateOf(prefs.globalKeyboardHeightDpOffset ?: defaultHeightOffset) }

    fun persist() {
        prefs.globalKeyboardWidthRatio = widthRatio
        prefs.globalKeyboardHeightRatio = heightRatio
        prefs.globalKeyboardWidthDpOffset = widthOffset
        prefs.globalKeyboardHeightDpOffset = heightOffset
    }

    // Keep Settings UI in sync with IME toolbar resizing (shared prefs are the single source of truth).
    DisposableEffect(prefs) {
        fun syncFromPrefs() {
            widthRatio = prefs.globalKeyboardWidthRatio ?: defaultWidthRatio
            heightRatio = prefs.globalKeyboardHeightRatio ?: defaultHeightRatio
            widthOffset = prefs.globalKeyboardWidthDpOffset ?: defaultWidthOffset
            heightOffset = prefs.globalKeyboardHeightDpOffset ?: defaultHeightOffset
        }

        // Ensure ratio exists once user starts customizing.
        if (prefs.globalKeyboardWidthRatio == null) prefs.globalKeyboardWidthRatio = widthRatio
        if (prefs.globalKeyboardHeightRatio == null) prefs.globalKeyboardHeightRatio = heightRatio

        val listener = prefs.addOnChangeListener { syncFromPrefs() }
        onDispose { prefs.removeOnChangeListener(listener) }
    }

    Column(modifier = modifier.padding(16.dp)) {
        Text(text = stringResource(R.string.settings_keyboard_size_desc))
        Spacer(modifier = Modifier.height(12.dp))

        KeyboardSizePreview(
            widthRatio = widthRatio,
            heightRatio = heightRatio,
            widthOffsetDp = widthOffset,
            heightOffsetDp = heightOffset,
            onChangeOffsets = { newW, newH ->
                widthOffset = newW
                heightOffset = newH
                persist()
            },
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "ratio: w=${String.format(Locale.ROOT, "%.2f", widthRatio)} h=${String.format(Locale.ROOT, "%.2f", heightRatio)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "offset: w=${String.format(Locale.ROOT, "%.0f", widthOffset)}dp h=${String.format(Locale.ROOT, "%.0f", heightOffset)}dp",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    widthOffset = defaultWidthOffset
                    heightOffset = defaultHeightOffset
                    prefs.globalKeyboardWidthRatio = defaultWidthRatio
                    prefs.globalKeyboardHeightRatio = defaultHeightRatio
                    prefs.globalKeyboardWidthDpOffset = defaultWidthOffset
                    prefs.globalKeyboardHeightDpOffset = defaultHeightOffset
                },
            ) {
                Text(stringResource(R.string.settings_keyboard_size_reset_default))
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

private fun resolveDictionaryLocaleTags(
    prefs: SettingsStore,
    subtypeManager: SubtypeManager,
    dictionaryManager: DictionaryManager,
): List<String> {
    val enabledTags = prefs.getEnabledLocaleTags()
    val subtypeTags = subtypeManager.listAll().map { it.localeTag }
    val dictionaryTags = dictionaryManager.listAll().flatMap { it.localeTags }
    val combined =
        (enabledTags + subtypeTags + dictionaryTags)
            .map { normalizeLocaleTag(it) }
            .filter { it.isNotBlank() }
            .distinct()
    if (combined.isEmpty()) {
        val fallback = normalizeLocaleTag(Locale.getDefault().toLanguageTag())
        return if (fallback.isBlank()) emptyList() else listOf(fallback)
    }
    return combined.sorted()
}

private fun isDictionaryEnableAllowed(
    spec: DictionarySpec,
    enabledLayoutIds: List<String>,
): Boolean {
    if (!spec.enabled) return false
    if (enabledLayoutIds.isEmpty()) return true
    if (spec.layoutIds.isEmpty()) return true
    return spec.layoutIds.any { it in enabledLayoutIds }
}

private data class DictionaryFormatOption(
    val formatId: String,
    val labelRes: Int,
    val mimeTypes: Array<String>,
)

private data class CodeSchemeOption(
    val codeScheme: String,
    val labelRes: Int,
    val kind: String?,
    val core: String?,
    val variant: String?,
    val defaultLayoutIds: List<String>,
)

private fun normalizeLocaleTag(tag: String): String {
    val t = tag.trim().replace('_', '-')
    val parts = t.split('-').filter { it.isNotBlank() }
    if (parts.isEmpty()) return ""
    val language = parts[0].lowercase(Locale.ROOT)
    val region = parts.getOrNull(1)?.uppercase(Locale.ROOT)
    return if (region.isNullOrBlank()) language else "$language-$region"
}

private fun resolveDisplayName(context: android.content.Context, uri: Uri): String? {
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) {
                return cursor.getString(idx)
            }
        }
    }
    return uri.lastPathSegment
}

private fun copyUriToTempFile(context: android.content.Context, uri: Uri): java.io.File {
    val temp = java.io.File.createTempFile("myboard_dict_", ".tmp", context.cacheDir)
    context.contentResolver.openInputStream(uri)?.use { input ->
        temp.outputStream().use { output -> input.copyTo(output) }
    } ?: error("Failed to open uri: $uri")
    return temp
}

private fun generateDictionaryId(
    baseName: String,
    localeTag: String,
    dictionaryManager: DictionaryManager,
): String {
    val normalizedTag = normalizeLocaleTag(localeTag)
    val safeBase = baseName.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "_").trim('_')
    val prefix = listOf(safeBase, normalizedTag.replace('-', '_')).filter { it.isNotBlank() }.joinToString("_")
    val seed = if (prefix.isBlank()) "user_dict" else "user_$prefix"
    val existing = dictionaryManager.listAll().map { it.dictionaryId }.toSet()
    if (seed !in existing) return seed
    var idx = 2
    while (true) {
        val candidate = "${seed}_$idx"
        if (candidate !in existing) return candidate
        idx++
    }
}

private fun removeDictionaryFromPrefs(prefs: SettingsStore, spec: DictionarySpec) {
    val dictId = spec.dictionaryId
    val tags = spec.localeTags.map { normalizeLocaleTag(it) }.filter { it.isNotBlank() }.distinct()
    for (tag in tags) {
        val enabled = prefs.getEnabledDictionaryIds(tag) ?: continue
        if (dictId !in enabled) continue
        prefs.setEnabledDictionaryIds(tag, enabled.filterNot { it == dictId })
    }
}

@Composable
private fun KeyboardSizePreview(
    widthRatio: Float,
    heightRatio: Float,
    widthOffsetDp: Float,
    heightOffsetDp: Float,
    onChangeOffsets: (newWidthOffsetDp: Float, newHeightOffsetDp: Float) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // Screen metrics in px, matching IME calculations (real pixels).
    val metrics = context.resources.displayMetrics
    val screenWidthPx = metrics.widthPixels.toFloat().coerceAtLeast(1f)
    val screenHeightPx = metrics.heightPixels.toFloat().coerceAtLeast(1f)
    val dpPx = with(density) { 1.dp.toPx() }.coerceAtLeast(0.5f)
    val topBarHeightPx = with(density) { 48.dp.toPx() }
    val minWidthPx = KeyboardSizeConstraints.minKeyboardWidthPx(metrics.density).toFloat()
    val minHeightPx = KeyboardSizeConstraints.minKeyboardHeightPx(metrics.density).toFloat()
    val maxKeyboardHeightPx =
        KeyboardSizeConstraints.maxKeyboardHeightPx(metrics.heightPixels, metrics.density).toFloat()
            .coerceAtLeast(minHeightPx)

    fun clampWidthOffsetDp(deltaWidthPx: Float, currentOffsetDp: Float): Float {
        val curPx = screenWidthPx * widthRatio + currentOffsetDp * dpPx
        val targetPx = (curPx + deltaWidthPx).coerceIn(minWidthPx, screenWidthPx)
        return (targetPx - screenWidthPx * widthRatio) / dpPx
    }

    fun clampHeightOffsetDp(deltaHeightPx: Float, currentOffsetDp: Float): Float {
        val curPx = screenHeightPx * heightRatio + currentOffsetDp * dpPx
        val targetPx = (curPx + deltaHeightPx).coerceIn(minHeightPx, maxKeyboardHeightPx)
        return (targetPx - screenHeightPx * heightRatio) / dpPx
    }

    val onChangeOffsetsLatest by rememberUpdatedState(onChangeOffsets)

    BoxWithConstraints(modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp).height(280.dp)) {
        val previewW = with(density) { maxWidth.toPx() }
        val previewH = with(density) { maxHeight.toPx() }
        val scale = kotlin.math.min(previewW / screenWidthPx, previewH / screenHeightPx).coerceIn(0.1f, 1f)

        val keyboardWidthPx = (screenWidthPx * widthRatio + widthOffsetDp * dpPx).coerceIn(minWidthPx, screenWidthPx)
        val keyboardHeightPx = (screenHeightPx * heightRatio + heightOffsetDp * dpPx).coerceIn(minHeightPx, maxKeyboardHeightPx)
        val panelWidthPx = keyboardWidthPx
        val panelHeightPx = keyboardHeightPx + topBarHeightPx

        val screenW = screenWidthPx * scale
        val screenH = screenHeightPx * scale
        val screenLeft = (previewW - screenW) / 2f
        val screenTop = (previewH - screenH) / 2f
        val screenRight = screenLeft + screenW
        val screenBottom = screenTop + screenH

        val panelW = panelWidthPx * scale
        val panelH = panelHeightPx * scale
        val panelLeft = screenLeft + (screenW - panelW) / 2f
        val panelTop = screenBottom - panelH
        val panelRight = panelLeft + panelW
        val panelBottom = panelTop + panelH

        val borderColor = Color(0xFF007AFF)
        val handleRadius = with(density) { 10.dp.toPx() }
        val handleHit = with(density) { 28.dp.toPx() }

        val left = panelLeft
        val top = panelTop
        val right = panelRight
        val bottom = panelBottom
  	    val cx = (left + right) / 2f
        val cy = (top + bottom) / 2f

        var activeHandle by remember { mutableStateOf(KeyboardSizeHandle.NONE) }

        fun pickHandleAt(p: Offset): KeyboardSizeHandle {
            fun near(a: Offset, b: Offset): Boolean {
                val dx = a.x - b.x
                val dy = a.y - b.y
                return (dx * dx + dy * dy) <= (handleHit * handleHit)
            }
            val leftP = Offset(left, cy)
            val rightP = Offset(right, cy)
            val topP = Offset(cx, top)
            val bottomP = Offset(cx, bottom)
            return when {
                near(p, leftP) -> KeyboardSizeHandle.LEFT
                near(p, rightP) -> KeyboardSizeHandle.RIGHT
                near(p, topP) -> KeyboardSizeHandle.TOP
                near(p, bottomP) -> KeyboardSizeHandle.BOTTOM
                else -> KeyboardSizeHandle.NONE
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(widthRatio, heightRatio, scale, screenWidthPx, screenHeightPx) {
                    var wDp = widthOffsetDp
                    var hDp = heightOffsetDp
                    detectDragGestures(
                        onDragStart = { start ->
                            activeHandle = pickHandleAt(start)
                            wDp = widthOffsetDp
                            hDp = heightOffsetDp
                        },
                        onDragEnd = { activeHandle = KeyboardSizeHandle.NONE },
                        onDragCancel = { activeHandle = KeyboardSizeHandle.NONE },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (activeHandle == KeyboardSizeHandle.NONE) return@detectDragGestures
                            val dx = dragAmount.x / scale
                            val dy = dragAmount.y / scale
                            when (activeHandle) {
                                KeyboardSizeHandle.LEFT -> {
                                    wDp = clampWidthOffsetDp(-dx, wDp)
                                    hDp = clampHeightOffsetDp(dy, hDp)
                                }
                                KeyboardSizeHandle.RIGHT -> {
                                    wDp = clampWidthOffsetDp(dx, wDp)
                                    hDp = clampHeightOffsetDp(dy, hDp)
                                }
                                KeyboardSizeHandle.TOP -> {
                                    wDp = clampWidthOffsetDp(dx, wDp)
                                    hDp = clampHeightOffsetDp(-dy, hDp)
                                }
                                KeyboardSizeHandle.BOTTOM -> {
                                    wDp = clampWidthOffsetDp(dx, wDp)
                                    hDp = clampHeightOffsetDp(dy, hDp)
                                }
                                KeyboardSizeHandle.NONE -> Unit
                            }
                            onChangeOffsetsLatest(wDp, hDp)
                        },
                    )
                },
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                // Background.
                drawRoundRect(
                    color = Color(0xFFF2F2F7),
                    size = size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(with(density) { 12.dp.toPx() }),
                )

                // Phone screen frame (full device screen).
                val frameCorner = with(density) { 18.dp.toPx() }
                drawRoundRect(
                    color = Color(0xFF1C1C1E),
                    topLeft = Offset(screenLeft, screenTop),
                    size = Size(screenW, screenH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(frameCorner),
                )
                val inset = with(density) { 3.dp.toPx() }
                drawRoundRect(
                    color = Color(0xFFF9F9FB),
                    topLeft = Offset(screenLeft + inset, screenTop + inset),
                    size = Size((screenW - inset * 2).coerceAtLeast(1f), (screenH - inset * 2).coerceAtLeast(1f)),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius((frameCorner - inset).coerceAtLeast(0f)),
                )

                // Panel border.
                drawRoundRect(
                    color = borderColor,
                    topLeft = Offset(left, top),
                    size = Size(panelW, panelH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(with(density) { 14.dp.toPx() }),
                    style = Stroke(width = with(density) { 2.dp.toPx() }),
                )
                // Toolbar separator line.
                drawLine(
                    color = borderColor.copy(alpha = 0.35f),
                    start = Offset(left, top + topBarHeightPx * scale),
                    end = Offset(right, top + topBarHeightPx * scale),
                    strokeWidth = with(density) { 1.dp.toPx() },
                )
                // Handles as circles.
                drawCircle(borderColor, radius = handleRadius, center = Offset(left, cy))
                drawCircle(borderColor, radius = handleRadius, center = Offset(right, cy))
                drawCircle(borderColor, radius = handleRadius, center = Offset(cx, top))
                drawCircle(borderColor, radius = handleRadius, center = Offset(cx, bottom))
            }

            // Center hint text.
            Text(
                text = "拖动手柄调整",
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LanguageLayoutSettings(
    modifier: Modifier,
    prefs: SettingsStore,
    subtypeManager: SubtypeManager,
    layoutManager: LayoutManager,
) {
    val profiles =
        remember {
            subtypeManager.listAll()
                .filter { it.enabled && it.localeTag.isNotBlank() }
                .sortedWith(compareByDescending<LocaleLayoutProfile> { it.priority }.thenBy { it.localeTag })
        }

    fun defaultLocaleTag(): String? {
        val preferred = prefs.userLocaleTag?.takeIf { it.isNotBlank() }
        if (!preferred.isNullOrBlank() && profiles.any { it.localeTag == preferred }) return preferred
        val resolved = subtypeManager.resolve(Locale.getDefault())?.localeTag
        if (!resolved.isNullOrBlank() && profiles.any { it.localeTag == resolved }) return resolved
        return profiles.firstOrNull()?.localeTag
    }

    var enabledLocaleTags by remember {
        val fromPrefs = prefs.getEnabledLocaleTags()
        val initial =
            if (fromPrefs.isNotEmpty()) fromPrefs
            else listOfNotNull(defaultLocaleTag())
        mutableStateOf(initial.distinct())
    }

    val expanded: SnapshotStateMap<String, Boolean> = remember { mutableStateMapOf() }
    val enabledLayoutsByLocale: SnapshotStateMap<String, List<String>> = remember { mutableStateMapOf() }
    val preferredLayoutByLocale: SnapshotStateMap<String, String?> = remember { mutableStateMapOf() }

    fun ensureLocaleInitialized(tag: String) {
        val profile = subtypeManager.get(tag) ?: return
        val ordered = profile.layoutIds
        val enabledFromPrefs = prefs.getEnabledLayoutIds(tag).takeIf { it.isNotEmpty() } ?: ordered
        val nextEnabled = enabledFromPrefs.filter { it in ordered }.distinct()
        prefs.setEnabledLayoutIds(tag, nextEnabled)

        val preferred = prefs.getPreferredLayoutId(tag)
        val nextPreferred = preferred?.takeIf { it in nextEnabled } ?: nextEnabled.firstOrNull()
        prefs.setPreferredLayoutId(tag, nextPreferred)

        enabledLayoutsByLocale[tag] = nextEnabled
        preferredLayoutByLocale[tag] = nextPreferred
    }

    DisposableEffect(enabledLocaleTags) {
        val normalized =
            enabledLocaleTags.mapNotNull { it.trim().takeIf { t -> t.isNotBlank() } }.distinct()
        if (normalized != enabledLocaleTags) {
            enabledLocaleTags = normalized
            return@DisposableEffect onDispose {}
        }

        // Persist enabled locale list.
        prefs.setEnabledLocaleTags(enabledLocaleTags)

        // Ensure current locale is one of the enabled locales.
        val current = prefs.userLocaleTag
        if (current.isNullOrBlank() || current !in enabledLocaleTags) {
            prefs.userLocaleTag = enabledLocaleTags.firstOrNull()
        }

        // Ensure each enabled locale has a valid enabled layout list + preferred layout.
        enabledLocaleTags.forEach(::ensureLocaleInitialized)
        onDispose {}
    }

    val currentLocaleTag = prefs.userLocaleTag

    LazyColumn(modifier = modifier, contentPadding = WindowInsets.systemBars.asPaddingValues()) {
        item { SectionHeader(textRes = R.string.settings_language_layout_language) }
        item {
            Text(
                text = stringResource(R.string.onboarding_layout_multi_select_hint),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        items(profiles, key = { it.localeTag }) { p ->
            val tag = p.localeTag
            val languageEnabled = tag in enabledLocaleTags
            val isCurrent = tag == currentLocaleTag
            val isExpanded = expanded[tag] ?: false

            ListItem(
                headlineContent = { Text(formatLocaleLabel(tag)) },
                supportingContent = {
                    val currentLabel = if (isCurrent) stringResource(R.string.settings_default) else ""
                    if (currentLabel.isNotBlank()) Text(currentLabel)
                },
                leadingContent = {
                    Checkbox(
                        checked = languageEnabled,
                        onCheckedChange = { next ->
                            val currentEnabled = enabledLocaleTags.toMutableList()
                            if (!next && currentEnabled.size <= 1) return@Checkbox
                            if (next) {
                                if (tag !in currentEnabled) currentEnabled.add(tag)
                            } else {
                                currentEnabled.remove(tag)
                            }
                            enabledLocaleTags = currentEnabled.distinct()
                            if (!next) {
                                expanded[tag] = false
                                if (prefs.userLocaleTag == tag) {
                                    prefs.userLocaleTag = enabledLocaleTags.firstOrNull()
                                }
                            } else {
                                ensureLocaleInitialized(tag)
                                if (prefs.userLocaleTag.isNullOrBlank()) prefs.userLocaleTag = tag
                            }
                        },
                    )
                },
                trailingContent = {
                    Text(if (isExpanded) "▲" else "▼")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (languageEnabled) {
                            prefs.userLocaleTag = tag
                            expanded[tag] = !(expanded[tag] ?: false)
                        }
                    },
            )
            HorizontalDivider()

            if (languageEnabled && (expanded[tag] ?: false)) {
                val enabledLayouts = enabledLayoutsByLocale[tag].orEmpty()
                val preferredLayoutId = preferredLayoutByLocale[tag]
                Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
                    Text(
                        text = stringResource(R.string.settings_language_layout_layouts),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                }

                p.layoutIds.forEach { layoutId ->
                    val checked = layoutId in enabledLayouts
                    val layoutName =
                        runCatching { layoutManager.getLayout(layoutId).name }.getOrNull()
                            ?.takeIf { it.isNotBlank() }
                            ?: layoutId

                    ListItem(
                        headlineContent = { Text(stringResource(R.string.onboarding_layout_item, layoutName, layoutId)) },
                        supportingContent = {
                            val label = if (preferredLayoutId == layoutId) stringResource(R.string.settings_default) else ""
                            if (label.isNotBlank()) Text(label)
                        },
                        leadingContent = {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { next ->
                                    val set = enabledLayouts.toMutableSet()
                                    if (next) set.add(layoutId) else set.remove(layoutId)
                                    val nextEnabled = p.layoutIds.filter { it in set }.distinct()
                                    enabledLayoutsByLocale[tag] = nextEnabled
                                    prefs.setEnabledLayoutIds(tag, nextEnabled)

                                    val preferred = prefs.getPreferredLayoutId(tag)
                                    val nextPreferred = preferred?.takeIf { it in nextEnabled } ?: nextEnabled.firstOrNull()
                                    preferredLayoutByLocale[tag] = nextPreferred
                                    prefs.setPreferredLayoutId(tag, nextPreferred)
                                },
                            )
                        },
                        modifier = Modifier.padding(start = 24.dp),
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 24.dp))
                }
            }
        }
    }
}

@Composable
private fun FeedbackSettings(
    modifier: Modifier,
    prefs: SettingsStore,
) {
    var clickVolume by remember { mutableStateOf(prefs.clickSoundVolumePercent.toFloat()) }
    var vibrationFollowSystem by remember { mutableStateOf(prefs.vibrationFollowSystem) }
    var vibrationStrength by remember { mutableStateOf(prefs.vibrationStrengthPercent.toFloat()) }

    LazyColumn(modifier = modifier, contentPadding = WindowInsets.systemBars.asPaddingValues()) {
        item { SectionHeader(textRes = R.string.settings_section_sound) }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_click_sound_volume)) },
                supportingContent = { Text(stringResource(R.string.settings_click_sound_volume_desc, clickVolume.toInt())) },
            )
        }
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Slider(
                    value = clickVolume,
                    onValueChange = { clickVolume = it },
                    valueRange = 0f..100f,
                    onValueChangeFinished = { prefs.clickSoundVolumePercent = clickVolume.toInt() },
                )
            }
        }
        item { HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp)) }

        item { SectionHeader(textRes = R.string.settings_section_vibration) }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_vibration_follow_system)) },
                supportingContent = { Text(stringResource(R.string.settings_vibration_follow_system_desc)) },
                trailingContent = {
                    Switch(
                        checked = vibrationFollowSystem,
                        onCheckedChange = {
                            vibrationFollowSystem = it
                            prefs.vibrationFollowSystem = it
                        },
                    )
                },
            )
        }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_vibration_strength)) },
                supportingContent = {
                    val desc =
                        if (vibrationFollowSystem) stringResource(R.string.settings_vibration_strength_following_system)
                        else stringResource(R.string.settings_vibration_strength_desc, vibrationStrength.toInt())
                    Text(desc)
                },
            )
        }
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Slider(
                    value = vibrationStrength,
                    onValueChange = { vibrationStrength = it },
                    enabled = !vibrationFollowSystem,
                    valueRange = 0f..100f,
                    onValueChangeFinished = { prefs.vibrationStrengthPercent = vibrationStrength.toInt() },
                )
            }
        }
    }
}

@Composable
private fun InputBehaviorSettings(
    modifier: Modifier,
    prefs: SettingsStore,
) {
    var clearAfterToken by remember { mutableStateOf(prefs.clearInputAfterTokenClear) }
    var clearDelayMs by remember { mutableStateOf(prefs.clearInputAfterTokenClearDelayMs.toFloat()) }

    LazyColumn(modifier = modifier, contentPadding = WindowInsets.systemBars.asPaddingValues()) {
        item { SectionHeader(textRes = R.string.settings_section_input) }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_clear_input_after_token_clear)) },
                supportingContent = { Text(stringResource(R.string.settings_clear_input_after_token_clear_desc)) },
                trailingContent = {
                    Switch(
                        checked = clearAfterToken,
                        onCheckedChange = {
                            clearAfterToken = it
                            prefs.clearInputAfterTokenClear = it
                        },
                    )
                },
            )
        }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_clear_input_delay)) },
                supportingContent = {
                    Text(stringResource(R.string.settings_clear_input_delay_desc, clearDelayMs.toInt()))
                },
            )
        }
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Slider(
                    value = clearDelayMs,
                    onValueChange = { clearDelayMs = it },
                    enabled = clearAfterToken,
                    valueRange = 120f..1500f,
                    onValueChangeFinished = {
                        prefs.clearInputAfterTokenClearDelayMs = clearDelayMs.toInt()
                    },
                )
            }
        }
    }
}

@Composable
private fun ToolbarSettings(
    modifier: Modifier,
    prefs: SettingsStore,
    toolbarManager: ToolbarManager,
) {
    val toolbarSpec = remember(toolbarManager) { toolbarManager.getDefaultToolbar() }
    val defaultItems = remember(toolbarSpec) { buildToolbarSettingItems(toolbarSpec) }
    val orderedDefault = remember(defaultItems, prefs.toolbarItemOrder) {
        applyToolbarOrderForSettings(defaultItems, prefs.toolbarItemOrder)
    }
    var items by remember { mutableStateOf(orderedDefault) }
    var maxCount by remember { mutableStateOf(prefs.toolbarMaxVisibleCount.toFloat()) }
    var draggingItemId by remember { mutableStateOf<String?>(null) }
    var dragOffsetPx by remember { mutableStateOf(0f) }
    var itemHeightPx by remember { mutableStateOf(0) }
    val latestItems by rememberUpdatedState(items)

    LazyColumn(modifier = modifier, contentPadding = WindowInsets.systemBars.asPaddingValues()) {
        item { SectionHeader(textRes = R.string.settings_toolbar) }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_toolbar_limit)) },
                supportingContent = {
                    val label =
                        if (maxCount.toInt() <= 0) {
                            stringResource(R.string.settings_toolbar_limit_unlimited)
                        } else {
                            stringResource(R.string.settings_toolbar_limit_fixed, maxCount.toInt())
                        }
                    Text(stringResource(R.string.settings_toolbar_limit_desc, label))
                },
            )
        }
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Slider(
                    value = maxCount,
                    onValueChange = {
                        maxCount = it
                        prefs.toolbarMaxVisibleCount = it.toInt()
                    },
                    valueRange = 0f..12f,
                )
            }
        }
        item { HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp)) }
        item { SectionHeader(textRes = R.string.settings_toolbar_sort) }
        items(items, key = { it.itemId }) { item ->
            val isDragging = draggingItemId == item.itemId
            ListItem(
                headlineContent = { Text(item.name) },
                supportingContent = { Text(item.itemId) },
                modifier = Modifier
                    .onSizeChanged { size ->
                        if (size.height > 0) itemHeightPx = size.height
                    }
                    .offset { IntOffset(0, if (isDragging) dragOffsetPx.toInt() else 0) }
                    .pointerInput(item.itemId, itemHeightPx) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggingItemId = item.itemId
                                dragOffsetPx = 0f
                            },
                            onDragCancel = {
                                draggingItemId = null
                                dragOffsetPx = 0f
                            },
                            onDragEnd = {
                                draggingItemId = null
                                dragOffsetPx = 0f
                                prefs.toolbarItemOrder = items.map { it.itemId }
                            },
                            onDrag = { change, dragAmount ->
                                if (draggingItemId != item.itemId || itemHeightPx <= 0) return@detectDragGesturesAfterLongPress
                                dragOffsetPx += dragAmount.y
                                val move = (dragOffsetPx / itemHeightPx).toInt()
                                if (move != 0) {
                                    val from = latestItems.indexOfFirst { it.itemId == item.itemId }
                                    val to = (from + move).coerceIn(0, latestItems.lastIndex)
                                    if (from >= 0 && to != from) {
                                        val next = latestItems.toMutableList()
                                        val moving = next.removeAt(from)
                                        next.add(to, moving)
                                        items = next
                                        dragOffsetPx -= move * itemHeightPx
                                        prefs.toolbarItemOrder = next.map { it.itemId }
                                    }
                                }
                                change.consume()
                            },
                        )
                    },
            )
            HorizontalDivider()
        }
    }
}

private data class ToolbarSettingItem(
    val itemId: String,
    val name: String,
)

private fun buildToolbarSettingItems(toolbarSpec: xyz.xiao6.myboard.model.ToolbarSpec?): List<ToolbarSettingItem> {
    val items = toolbarSpec?.items.orEmpty().filter { it.enabled }
    if (items.isNotEmpty()) {
        return items
            .sortedWith(compareByDescending<xyz.xiao6.myboard.model.ToolbarItemSpec> { it.priority }.thenBy { it.itemId })
            .map { ToolbarSettingItem(it.itemId, it.name) }
    }
    return listOf(
        ToolbarSettingItem("layout", "Layout"),
        ToolbarSettingItem("voice", "Voice"),
        ToolbarSettingItem("emoji", "Emoji"),
        ToolbarSettingItem("clipboard", "Clipboard"),
        ToolbarSettingItem("kb_resize", "Resize"),
        ToolbarSettingItem("settings", "Settings"),
    )
}

private fun applyToolbarOrderForSettings(
    items: List<ToolbarSettingItem>,
    order: List<String>,
): List<ToolbarSettingItem> {
    if (order.isEmpty()) return items
    val byId = items.associateBy { it.itemId }
    val ordered = order.mapNotNull { byId[it] }
    val orderSet = order.toSet()
    val remaining = items.filterNot { it.itemId in orderSet }
    return ordered + remaining
}

private fun formatLocaleLabel(localeTag: String): String {
    val tag = localeTag.trim().replace('_', '-')
    val display = Locale.forLanguageTag(tag).getDisplayName(Locale.getDefault()).ifBlank { tag }
    return "$display ($tag)"
}
