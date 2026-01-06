package xyz.xiao6.myboard.ui

import android.content.Intent
import android.content.Context
import xyz.xiao6.myboard.ui.settings.AppearanceSettings
import xyz.xiao6.myboard.ui.settings.DictionariesSettings
import xyz.xiao6.myboard.ui.settings.FeedbackSettings
import xyz.xiao6.myboard.ui.settings.InputBehaviorSettings
import xyz.xiao6.myboard.ui.settings.KeyboardSizeSettings
import xyz.xiao6.myboard.ui.settings.LanguageLayoutSettings
import xyz.xiao6.myboard.ui.settings.SuggestionsSettings
import xyz.xiao6.myboard.ui.settings.ToolbarSettings
import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.provider.Settings.Secure
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import xyz.xiao6.myboard.R
import xyz.xiao6.myboard.dictionary.UserDictionaryStore
import xyz.xiao6.myboard.dictionary.format.DictionaryEntry
import xyz.xiao6.myboard.ime.MyBoardImeService
import xyz.xiao6.myboard.manager.DictionaryManager
import xyz.xiao6.myboard.manager.LayoutManager
import xyz.xiao6.myboard.manager.SubtypeManager
import xyz.xiao6.myboard.manager.ThemeManager
import xyz.xiao6.myboard.manager.ToolbarManager
import xyz.xiao6.myboard.model.DictionarySpec
import xyz.xiao6.myboard.model.BackgroundStyle
import xyz.xiao6.myboard.model.KeyboardLayout
import xyz.xiao6.myboard.model.Key
import xyz.xiao6.myboard.model.KeyAction
import xyz.xiao6.myboard.model.KeyIds
import xyz.xiao6.myboard.model.KeyUI
import xyz.xiao6.myboard.model.Action
import xyz.xiao6.myboard.model.ActionType
import xyz.xiao6.myboard.model.CommandType
import xyz.xiao6.myboard.model.GestureType
import xyz.xiao6.myboard.model.GridPosition
import xyz.xiao6.myboard.model.ModifierKey
import xyz.xiao6.myboard.model.KeyActionCase
import xyz.xiao6.myboard.model.WhenCondition
import xyz.xiao6.myboard.model.LayoutDefaults
import xyz.xiao6.myboard.model.RowAlignment
import xyz.xiao6.myboard.model.ThemeSpec
import xyz.xiao6.myboard.model.validate
import xyz.xiao6.myboard.store.SettingsStore
import xyz.xiao6.myboard.ui.theme.MyBoardTheme
import xyz.xiao6.myboard.util.KeyboardSizeConstraints
import java.io.File
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** 应用主入口：设置页（若未完成引导会自动跳转到 SetupActivity）。 */
class SettingsActivity : AppCompatActivity() {
    private lateinit var prefs: SettingsStore
    private lateinit var subtypeManager: SubtypeManager
    private lateinit var layoutManager: LayoutManager
    private lateinit var toolbarManager: ToolbarManager
    private lateinit var dictionaryManager: DictionaryManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        prefs = SettingsStore(this)
        subtypeManager = SubtypeManager(this).loadAll()
        layoutManager = LayoutManager(this).loadAll()
        toolbarManager = ToolbarManager(this).loadAllFromAssets()
        dictionaryManager = DictionaryManager(this).loadAll()

        if (!prefs.onboardingCompleted) {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }

        setContent {
            MyBoardTheme {
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
    data object Suggestions : SettingsRoute
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
    var languageDetailTag by remember { mutableStateOf<String?>(null) }
    var showQuickInput by remember { mutableStateOf(false) }
    var quickInputText by remember { mutableStateOf("") }
    var quickInputDragOffsetPx by remember { mutableStateOf(0f) }
    var quickInputHeightPx by remember { mutableStateOf(0) }
    var fabOffsetX by remember { mutableStateOf(0f) }
    var fabOffsetY by remember { mutableStateOf(0f) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var imeEnabledState by remember { mutableStateOf(false) }
    var imeSelectedState by remember { mutableStateOf(false) }
    var showLayoutMenu by remember { mutableStateOf(false) }
    var showCreateLayoutDialog by remember { mutableStateOf(false) }
    var showImportLayout by remember { mutableStateOf(false) }
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
        SettingsRoute.Suggestions -> R.string.settings_suggestions
        SettingsRoute.Dictionaries -> R.string.settings_dictionaries
        SettingsRoute.Appearance -> R.string.settings_appearance
        SettingsRoute.KeyboardSize -> R.string.settings_keyboard_size
    }

    LaunchedEffect(route) {
        if (route != SettingsRoute.LanguageLayout) {
            showLayoutMenu = false
            showCreateLayoutDialog = false
            showImportLayout = false
        }
    }

    BackHandler(enabled = route != SettingsRoute.Main || languageDetailTag != null) {
        when {
            route == SettingsRoute.LanguageLayout && languageDetailTag != null -> languageDetailTag = null
            else -> route = SettingsRoute.Main
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(titleRes)) },
                navigationIcon = {
                    if (route != SettingsRoute.Main || languageDetailTag != null) {
                        IconButton(
                            onClick = {
                                when {
                                    route == SettingsRoute.LanguageLayout && languageDetailTag != null ->
                                        languageDetailTag = null
                                    else -> route = SettingsRoute.Main
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                            )
                        }
                    }
                },
                actions = {
                    if (route == SettingsRoute.LanguageLayout) {
                        IconButton(onClick = { showLayoutMenu = true }) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = stringResource(R.string.settings_language_layout_menu),
                            )
                        }
                        DropdownMenu(
                            expanded = showLayoutMenu,
                            onDismissRequest = { showLayoutMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.settings_language_layout_create_custom)) },
                                onClick = {
                                    showLayoutMenu = false
                                    showCreateLayoutDialog = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.settings_language_layout_import_json)) },
                                onClick = {
                                    showLayoutMenu = false
                                    showImportLayout = true
                                },
                            )
                        }
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets.systemBars,
    ) { padding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val maxWidthPx = constraints.maxWidth.toFloat()
            val maxHeightPx = constraints.maxHeight.toFloat()
            val fabSizePx = with(density) { 56.dp.toPx() }
            val fabMarginPx = with(density) { 16.dp.toPx() }
            val fabBaseX = (maxWidthPx - fabSizePx - fabMarginPx).coerceAtLeast(0f)
            val fabBaseY = (maxHeightPx - fabSizePx - fabMarginPx).coerceAtLeast(0f)
            val fabX = (fabBaseX + fabOffsetX).coerceIn(0f, (maxWidthPx - fabSizePx).coerceAtLeast(0f))
            val fabY = (fabBaseY + fabOffsetY).coerceIn(0f, (maxHeightPx - fabSizePx).coerceAtLeast(0f))

            when (route) {
                SettingsRoute.Main -> SettingsMainList(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    prefs = prefs,
                    imeEnabled = imeEnabledState,
                    imeSelected = imeSelectedState,
                    context = context,
                    onOpenImeSettings = onOpenImeSettings,
                    onShowImePicker = onShowImePicker,
                    onOpenLanguageLayout = {
                        languageDetailTag = null
                        route = SettingsRoute.LanguageLayout
                    },
                    onOpenAppearance = { route = SettingsRoute.Appearance },
                    onOpenFeedback = { route = SettingsRoute.Feedback },
                    onOpenInputBehavior = { route = SettingsRoute.InputBehavior },
                    onOpenToolbar = { route = SettingsRoute.Toolbar },
                    onOpenSuggestions = { route = SettingsRoute.Suggestions },
                    onOpenDictionaries = { route = SettingsRoute.Dictionaries },
                    onOpenKeyboardSize = { route = SettingsRoute.KeyboardSize },
                    onResetSetup = onResetSetup,
                )

                SettingsRoute.LanguageLayout -> LanguageLayoutSettings(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    prefs = prefs,
                    subtypeManager = subtypeManager,
                    layoutManager = layoutManager,
                    detailTag = languageDetailTag,
                    onDetailTagChange = { languageDetailTag = it },
                    showCreateLayoutDialog = showCreateLayoutDialog,
                    onShowCreateLayoutDialogChange = { showCreateLayoutDialog = it },
                    showImportLayout = showImportLayout,
                    onShowImportLayoutChange = { showImportLayout = it },
                    onOpenLayoutEditor = { tag ->
                        val intent = Intent(context, ModernLayoutEditorActivity::class.java)
                        intent.putExtra("locale_tag", tag)
                        context.startActivity(intent)
                    },
                )

                SettingsRoute.Appearance -> AppearanceSettings(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    prefs = prefs,
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

                SettingsRoute.Suggestions -> SuggestionsSettings(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    prefs = prefs,
                )

                SettingsRoute.Dictionaries -> DictionariesSettings(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    prefs = prefs,
                    subtypeManager = subtypeManager,
                    dictionaryManager = dictionaryManager,
                )

                SettingsRoute.KeyboardSize -> KeyboardSizeSettings(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    prefs = prefs,
                    layoutManager = layoutManager,
                )
            }

            FloatingActionButton(
                onClick = { showQuickInput = true },
                modifier = Modifier
                    .offset { IntOffset(fabX.toInt(), fabY.toInt()) }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            fabOffsetX += dragAmount.x
                            fabOffsetY += dragAmount.y
                            change.consume()
                        }
                    },
            ) {
                Text(stringResource(R.string.settings_quick_input))
            }

            if (showQuickInput) {
                val imeBottomPx = WindowInsets.ime.getBottom(density).toFloat()
                val marginPx = with(density) { 8.dp.toPx() }
                val minOffset = -(maxHeightPx - quickInputHeightPx - marginPx).coerceAtLeast(0f)
                val maxOffset = -marginPx
                val baseOffset = -imeBottomPx - marginPx
                val clampedOffset = (baseOffset + quickInputDragOffsetPx).coerceIn(minOffset, maxOffset)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .offset { IntOffset(0, clampedOffset.toInt()) }
                        .padding(horizontal = 16.dp),
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onSizeChanged { quickInputHeightPx = it.height },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp)
                                    .pointerInput(Unit) {
                                        detectDragGestures { change, dragAmount ->
                                            quickInputDragOffsetPx += dragAmount.y
                                            change.consume()
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(36.dp)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(MaterialTheme.colorScheme.outlineVariant),
                                )
                            }
                            OutlinedTextField(
                                value = quickInputText,
                                onValueChange = { quickInputText = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                                label = { Text(stringResource(R.string.settings_quick_input_label)) },
                                placeholder = { Text(stringResource(R.string.settings_quick_input_placeholder)) },
                                trailingIcon = {
                                    TextButton(
                                        onClick = {
                                            showQuickInput = false
                                            quickInputText = ""
                                            quickInputDragOffsetPx = 0f
                                            focusManager.clearFocus(force = true)
                                            keyboardController?.hide()
                                        },
                                    ) {
                                        Text(stringResource(R.string.common_close))
                                    }
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                ),
                            )
                        }
                    }
                }
                LaunchedEffect(showQuickInput) {
                    if (showQuickInput) {
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsMainList(
    modifier: Modifier,
    prefs: SettingsStore,
    imeEnabled: Boolean,
    imeSelected: Boolean,
    context: android.content.Context,
    onOpenImeSettings: () -> Unit,
    onShowImePicker: () -> Unit,
    onOpenLanguageLayout: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenFeedback: () -> Unit,
    onOpenInputBehavior: () -> Unit,
    onOpenToolbar: () -> Unit,
    onOpenSuggestions: () -> Unit,
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
                titleRes = R.string.settings_suggestions,
                summaryRes = R.string.settings_suggestions_desc,
                onClick = onOpenSuggestions,
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


private val layoutJson =
    Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = false
    }

private fun sanitizeLayoutFileName(layoutId: String): String {
    val normalized = layoutId.trim().lowercase(Locale.ROOT)
    val sanitized = normalized.replace(Regex("[^a-z0-9._-]"), "_")
    return sanitized.ifBlank { "layout_${System.currentTimeMillis()}" }
}

private fun writeUserLayoutSpec(context: android.content.Context, layout: KeyboardLayout) {
    val dir = LayoutManager(context).getUserLayoutDir()
    val safeName = sanitizeLayoutFileName(layout.layoutId)
    val file = File(dir, "$safeName.json")
    val text = layoutJson.encodeToString(KeyboardLayout.serializer(), layout)
    file.writeText(text, Charsets.UTF_8)
}
