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
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.font.FontWeight
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
import xyz.xiao6.myboard.model.LocaleLayoutProfile
import xyz.xiao6.myboard.model.DictionarySpec
import xyz.xiao6.myboard.model.BackgroundStyle
import xyz.xiao6.myboard.model.ImageSpec
import xyz.xiao6.myboard.model.KeyboardLayout
import xyz.xiao6.myboard.model.Key
import xyz.xiao6.myboard.model.KeyAction
import xyz.xiao6.myboard.model.KeyActionDefault
import xyz.xiao6.myboard.model.KeyIds
import xyz.xiao6.myboard.model.KeyPrimaryCodes
import xyz.xiao6.myboard.model.KeyUI
import xyz.xiao6.myboard.model.Action
import xyz.xiao6.myboard.model.ActionType
import xyz.xiao6.myboard.model.CommandType
import xyz.xiao6.myboard.model.GestureType
import xyz.xiao6.myboard.model.GridPosition
import xyz.xiao6.myboard.model.ModifierKey
import xyz.xiao6.myboard.model.KeyActionCase
import xyz.xiao6.myboard.model.WhenCondition
import xyz.xiao6.myboard.model.KeyboardRow
import xyz.xiao6.myboard.model.LayoutDefaults
import xyz.xiao6.myboard.model.LayoutPadding
import xyz.xiao6.myboard.model.RowAlignment
import xyz.xiao6.myboard.model.KeyStyle
import xyz.xiao6.myboard.model.LayoutParser
import xyz.xiao6.myboard.model.TextStyle
import xyz.xiao6.myboard.model.ThemeSpec
import xyz.xiao6.myboard.model.ThemeParser
import xyz.xiao6.myboard.model.validate
import xyz.xiao6.myboard.store.SettingsStore
import xyz.xiao6.myboard.ui.theme.MyBoardTheme
import xyz.xiao6.myboard.ui.theme.ThemeRuntime
import xyz.xiao6.myboard.util.KeyboardSizeConstraints
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    data object LayoutEditor : SettingsRoute
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

private enum class ThemePanel {
    PREVIEW,
    BASIC,
    ADVANCED,
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
    var layoutEditorState by remember { mutableStateOf<LayoutEditorState?>(null) }
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
    var layoutEditorError by remember { mutableStateOf<String?>(null) }
    var showLayoutNameDialog by remember { mutableStateOf(false) }
    var layoutNameInput by remember { mutableStateOf("") }
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
        SettingsRoute.LayoutEditor -> R.string.settings_layout_editor_title
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
            route == SettingsRoute.LayoutEditor -> route = SettingsRoute.LanguageLayout
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
                                    route == SettingsRoute.LayoutEditor -> route = SettingsRoute.LanguageLayout
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
                    } else if (route == SettingsRoute.LayoutEditor) {
                        TextButton(
                            onClick = {
                                val state = layoutEditorState ?: return@TextButton
                                layoutEditorError = null
                                if (state.name.trim().isBlank()) {
                                    layoutNameInput = ""
                                    showLayoutNameDialog = true
                                } else {
                                    trySaveLayout(
                                        context = context,
                                        state = state,
                                        existingLayoutIds = layoutManager.listLayoutIds().toSet(),
                                        layoutManager = layoutManager,
                                        prefs = prefs,
                                        onSaved = {
                                            layoutEditorState = null
                                            route = SettingsRoute.LanguageLayout
                                        },
                                        onError = { layoutEditorError = it },
                                    )
                                }
                            },
                            enabled = layoutEditorState != null,
                        ) {
                            Text(stringResource(R.string.layout_editor_save))
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
                    onOpenImeSettings = onOpenImeSettings,
                    onShowImePicker = onShowImePicker,
                    onOpenLanguageLayout = {
                        languageDetailTag = null
                        route = SettingsRoute.LanguageLayout
                    },
                    onOpenFeedback = { route = SettingsRoute.Feedback },
                    onOpenInputBehavior = { route = SettingsRoute.InputBehavior },
                    onOpenToolbar = { route = SettingsRoute.Toolbar },
                    onOpenSuggestions = { route = SettingsRoute.Suggestions },
                    onOpenAppearance = { route = SettingsRoute.Appearance },
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
                        val initial = buildLayoutEditorState(tag, layoutManager)
                        layoutEditorState = initial
                        languageDetailTag = tag
                        route = SettingsRoute.LayoutEditor
                    },
                )

                SettingsRoute.LayoutEditor -> {
                    val state = layoutEditorState
                    if (state == null) {
                        route = SettingsRoute.LanguageLayout
                    } else {
                        LayoutEditorScreen(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding),
                            state = state,
                            errorText = layoutEditorError,
                        )
                    }
                }

                SettingsRoute.Feedback -> FeedbackSettings(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    prefs = prefs,
                )

                SettingsRoute.InputBehavior -> InputBehaviorSettings(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    prefs = prefs,
                )

                SettingsRoute.Toolbar -> ToolbarSettings(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    prefs = prefs,
                    toolbarManager = toolbarManager,
                )

                SettingsRoute.Suggestions -> SuggestionsSettings(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    prefs = prefs,
                )

                SettingsRoute.Dictionaries -> DictionariesSettings(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    prefs = prefs,
                    subtypeManager = subtypeManager,
                    dictionaryManager = dictionaryManager,
                )

                SettingsRoute.Appearance -> AppearanceSettings(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    prefs = prefs,
                    layoutManager = layoutManager,
                )

                SettingsRoute.KeyboardSize -> KeyboardSizeSettings(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
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
    onOpenImeSettings: () -> Unit,
    onShowImePicker: () -> Unit,
    onOpenLanguageLayout: () -> Unit,
    onOpenFeedback: () -> Unit,
    onOpenInputBehavior: () -> Unit,
    onOpenToolbar: () -> Unit,
    onOpenSuggestions: () -> Unit,
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

@Composable
private fun AppearanceSettings(
    modifier: Modifier,
    prefs: SettingsStore,
    layoutManager: LayoutManager,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var emojiImages by remember { mutableStateOf(prefs.emojiImageEnabled) }
    var selectedThemeId by remember { mutableStateOf(prefs.keyboardThemeId) }
    var themeOptions by remember { mutableStateOf(emptyList<ThemeOption>()) }
    var themeSpecs by remember { mutableStateOf<Map<String, ThemeSpec>>(emptyMap()) }
    var showThemePicker by remember { mutableStateOf(false) }
    var themeImportError by remember { mutableStateOf<String?>(null) }
    var showBasicBasePicker by remember { mutableStateOf(false) }
    var showAdvancedBasePicker by remember { mutableStateOf(false) }

    val localeTag = prefs.userLocaleTag ?: Locale.getDefault().toLanguageTag()
    val previewLayout =
        remember(localeTag, layoutManager) {
            runCatching { layoutManager.getDefaultLayout(Locale.forLanguageTag(localeTag)) }
                .getOrNull()
        }

    fun refreshThemes() {
        val manager = ThemeManager(context).loadAll()
        val names =
            mapOf(
                "default" to context.getString(R.string.settings_theme_default),
                "sand" to context.getString(R.string.settings_theme_sand),
                "slate" to context.getString(R.string.settings_theme_slate),
            )
        themeOptions =
            manager.listAll().map { spec ->
                ThemeOption(
                    themeId = spec.themeId,
                    name = names[spec.themeId] ?: spec.name ?: spec.themeId,
                )
            }
        themeSpecs = manager.listAll().associateBy { it.themeId }
    }

    DisposableEffect(prefs) {
        val listener = prefs.addOnChangeListener {
            selectedThemeId = prefs.keyboardThemeId
        }
        onDispose { prefs.removeOnChangeListener(listener) }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            themeImportError = null
            val result =
                withContext(Dispatchers.IO) {
                    importThemeFromUri(context, uri)
                }
            if (result == null) {
                themeImportError = context.getString(R.string.settings_theme_import_failed)
                return@launch
            }
            prefs.keyboardThemeId = result.themeId
            selectedThemeId = result.themeId
            refreshThemes()
        }
    }

    val imagePickerTarget = remember { mutableStateOf<ThemeImageTarget?>(null) }
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val target = imagePickerTarget.value ?: return@rememberLauncherForActivityResult
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val saved = copyThemeImageFromUri(context, uri, target.themeId)
            withContext(Dispatchers.Main) {
                target.onImageSelected(saved)
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshThemes()
    }

    val currentThemeId = selectedThemeId?.takeIf { it.isNotBlank() } ?: "default"
    val currentThemeSpec = themeSpecs[currentThemeId] ?: themeSpecs["default"]
    var themePanel by remember { mutableStateOf(ThemePanel.PREVIEW) }

    val basicTokens = remember {
        listOf(
            ThemeColorToken("background", R.string.settings_theme_color_background, android.graphics.Color.parseColor("#F2F2F7")),
            ThemeColorToken("key_bg", R.string.settings_theme_color_key_background, android.graphics.Color.WHITE),
            ThemeColorToken("key_bg_pressed", R.string.settings_theme_color_key_pressed, android.graphics.Color.parseColor("#E5E5EA")),
            ThemeColorToken("key_bg_function", R.string.settings_theme_color_key_function, android.graphics.Color.parseColor("#E5E5EA")),
            ThemeColorToken("key_bg_function_pressed", R.string.settings_theme_color_key_function_pressed, android.graphics.Color.parseColor("#D1D1D6")),
            ThemeColorToken("key_text", R.string.settings_theme_color_key_text, android.graphics.Color.BLACK),
            ThemeColorToken("key_hint", R.string.settings_theme_color_key_hint, android.graphics.Color.parseColor("#8E8E93")),
            ThemeColorToken("accent", R.string.settings_theme_color_accent, android.graphics.Color.parseColor("#007AFF")),
        )
    }
    var basicBaseThemeId by remember { mutableStateOf(currentThemeId) }
    var basicDirty by remember { mutableStateOf(false) }
    val basicOverrides = remember { mutableStateMapOf<String, Int>() }

    LaunchedEffect(basicBaseThemeId, themeSpecs) {
        val baseSpec = themeSpecs[basicBaseThemeId]
        if (baseSpec != null && !basicDirty) {
            basicOverrides.clear()
            for (token in basicTokens) {
                val raw = baseSpec.colors[token.key]
                val resolved =
                    resolveThemeColor(baseSpec, raw, token.fallbackColor)
                basicOverrides[token.key] = resolved
            }
        }
    }

    val basicPreviewSpec = currentThemeSpec?.let { base ->
        val overrides = basicOverrides.mapValues { formatColorHex(it.value) }
        base.copy(
            themeId = THEME_ID_BASIC,
            name = context.getString(R.string.settings_theme_basic_name),
            colors = base.colors + overrides,
        )
    }

    var advancedBaseThemeId by remember { mutableStateOf(currentThemeId) }
    var advancedDirty by remember { mutableStateOf(false) }
    val advancedKeyOverrides = remember { mutableStateMapOf<String, KeyOverrideState>() }
    var advancedLayoutBgColor by remember { mutableStateOf<Int?>(null) }
    var advancedLayoutBgImage by remember { mutableStateOf<String?>(null) }
    var selectedKeyId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(advancedBaseThemeId, themeSpecs) {
        val baseSpec = themeSpecs[advancedBaseThemeId]
        if (baseSpec != null && !advancedDirty) {
            advancedKeyOverrides.clear()
            advancedLayoutBgColor = baseSpec.layout.background?.color?.let {
                resolveThemeColor(baseSpec, it, android.graphics.Color.parseColor("#F2F2F7"))
            }
            advancedLayoutBgImage = baseSpec.layout.background?.image?.assetPath
        }
    }

    val advancedPreviewSpec = themeSpecs[advancedBaseThemeId]?.let { base ->
        buildAdvancedThemeSpec(
            base = base,
            overrides = advancedKeyOverrides,
            layoutBgColor = advancedLayoutBgColor,
            layoutBgImage = advancedLayoutBgImage,
            name = context.getString(R.string.settings_theme_advanced_name),
        )
    }

    if (showThemePicker) {
        val effectiveThemeId = selectedThemeId?.takeIf { it.isNotBlank() } ?: "default"
        AlertDialog(
            onDismissRequest = { showThemePicker = false },
            title = { Text(text = stringResource(R.string.settings_keyboard_theme)) },
            text = {
                Column {
                    themeOptions.forEach { option ->
                        val selected = option.themeId == effectiveThemeId
                        ListItem(
                            headlineContent = { Text(text = option.name) },
                            supportingContent = { Text(option.themeId) },
                            trailingContent = { if (selected) Text("OK") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    prefs.keyboardThemeId = option.themeId
                                    selectedThemeId = option.themeId
                                    showThemePicker = false
                                },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemePicker = false }) {
                    Text(text = stringResource(R.string.common_close))
                }
            },
        )
    }

    if (showBasicBasePicker) {
        ThemeBasePickerDialog(
            themeOptions = themeOptions,
            currentThemeId = basicBaseThemeId,
            onDismiss = { showBasicBasePicker = false },
            onThemeSelected = { themeId ->
                showBasicBasePicker = false
                basicBaseThemeId = themeId
                basicDirty = false
            },
        )
    }

    if (showAdvancedBasePicker) {
        ThemeBasePickerDialog(
            themeOptions = themeOptions,
            currentThemeId = advancedBaseThemeId,
            onDismiss = { showAdvancedBasePicker = false },
            onThemeSelected = { themeId ->
                showAdvancedBasePicker = false
                advancedBaseThemeId = themeId
                advancedDirty = false
            },
        )
    }

    LazyColumn(modifier = modifier, contentPadding = WindowInsets.systemBars.asPaddingValues()) {
        item {
            ThemeHeroCard(
                title = stringResource(R.string.settings_theme_studio_title),
                subtitle = stringResource(R.string.settings_theme_studio_subtitle),
            )
        }
        item {
            ThemeBlockCard(title = stringResource(R.string.settings_theme_active)) {
                val currentId = selectedThemeId?.takeIf { it.isNotBlank() } ?: "default"
                val currentName = themeOptions.firstOrNull { it.themeId == currentId }?.name ?: currentId
                Text(text = currentName, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = currentId,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { showThemePicker = true }) {
                        Text(stringResource(R.string.settings_theme_change))
                    }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/*")) },
                    ) {
                        Text(stringResource(R.string.settings_theme_import))
                    }
                }
                if (!themeImportError.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = themeImportError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        item {
            ThemePanelToggle(
                selected = themePanel,
                onSelect = { themePanel = it },
            )
        }
        when (themePanel) {
            ThemePanel.PREVIEW -> {
                item {
                    ThemePreviewCard(
                        title = stringResource(R.string.settings_theme_preview_title),
                        spec = currentThemeSpec,
                        layout = previewLayout,
                    )
                }
                item {
                    ThemePreviewCard(
                        title = stringResource(R.string.settings_theme_basic_preview),
                        spec = basicPreviewSpec,
                        layout = previewLayout,
                    )
                }
                item {
                    ThemePreviewCard(
                        title = stringResource(R.string.settings_theme_advanced_preview),
                        spec = advancedPreviewSpec,
                        layout = previewLayout,
                    )
                }
            }

            ThemePanel.BASIC -> {
                item {
                    ThemeBlockCard(title = stringResource(R.string.settings_section_theme_basic)) {
                        ThemeBaseSelectorRow(
                            label = stringResource(R.string.settings_theme_base),
                            themeOptions = themeOptions,
                            currentThemeId = basicBaseThemeId,
                            onClick = { showBasicBasePicker = true },
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        ThemePreviewCard(
                            title = stringResource(R.string.settings_theme_basic_preview),
                            spec = basicPreviewSpec,
                            layout = previewLayout,
                        )
                    }
                }
                item {
                    ThemeBlockCard(title = stringResource(R.string.settings_theme_palette_title)) {
                        basicTokens.forEach { token ->
                            val color = basicOverrides[token.key] ?: token.fallbackColor
                            ColorPickerRow(
                                label = stringResource(token.labelRes),
                                color = color,
                                onColorChange = {
                                    basicDirty = true
                                    basicOverrides[token.key] = it
                                },
                                compact = true,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = {
                                val base = themeSpecs[basicBaseThemeId] ?: return@Button
                                val spec = base.copy(
                                    themeId = THEME_ID_BASIC,
                                    name = context.getString(R.string.settings_theme_basic_name),
                                    colors = base.colors + basicOverrides.mapValues { formatColorHex(it.value) },
                                )
                                writeThemeSpec(context, spec)
                                prefs.keyboardThemeId = spec.themeId
                                selectedThemeId = spec.themeId
                                refreshThemes()
                            },
                        ) {
                            Text(stringResource(R.string.settings_theme_basic_apply))
                        }
                        OutlinedButton(
                            onClick = {
                                basicDirty = false
                                basicOverrides.clear()
                                val base = themeSpecs[basicBaseThemeId] ?: return@OutlinedButton
                                for (token in basicTokens) {
                                    val raw = base.colors[token.key]
                                    basicOverrides[token.key] = resolveThemeColor(base, raw, token.fallbackColor)
                                }
                            },
                        ) {
                            Text(stringResource(R.string.settings_theme_reset))
                        }
                    }
                }
            }

            ThemePanel.ADVANCED -> {
                item {
                    ThemeBlockCard(title = stringResource(R.string.settings_section_theme_advanced)) {
                        ThemeBaseSelectorRow(
                            label = stringResource(R.string.settings_theme_base),
                            themeOptions = themeOptions,
                            currentThemeId = advancedBaseThemeId,
                            onClick = { showAdvancedBasePicker = true },
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        ThemePreviewCard(
                            title = stringResource(R.string.settings_theme_advanced_preview),
                            spec = advancedPreviewSpec,
                            layout = previewLayout,
                        )
                    }
                }
                item {
                    ThemeBlockCard(title = stringResource(R.string.settings_theme_background)) {
                        Text(
                            text = stringResource(R.string.settings_theme_background_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        ColorPickerRow(
                            label = stringResource(R.string.settings_theme_background_color),
                            color = advancedLayoutBgColor ?: android.graphics.Color.parseColor("#F2F2F7"),
                            onColorChange = {
                                advancedDirty = true
                                advancedLayoutBgColor = it
                            },
                            compact = true,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        ThemeImagePickerRow(
                            label = stringResource(R.string.settings_theme_background_image),
                            path = advancedLayoutBgImage,
                            onPick = {
                                imagePickerTarget.value =
                                    ThemeImageTarget(
                                        themeId = THEME_ID_ADVANCED,
                                        onImageSelected = { path ->
                                            advancedDirty = true
                                            advancedLayoutBgImage = path
                                        },
                                    )
                                imagePickerLauncher.launch(arrayOf("image/*"))
                            },
                            onClear = {
                                advancedDirty = true
                                advancedLayoutBgImage = null
                            },
                            compact = true,
                        )
                    }
                }
                item {
                    ThemeBlockCard(title = stringResource(R.string.settings_theme_key_preview)) {
                        if (previewLayout != null) {
                            KeyboardPreviewEditor(
                                layout = previewLayout,
                                spec = advancedPreviewSpec,
                                selectedKeyId = selectedKeyId,
                                onSelectKey = { selectedKeyId = it },
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.settings_theme_preview_unavailable),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                item {
                    val keyId = selectedKeyId
                    if (keyId != null) {
                        val currentOverride = advancedKeyOverrides[keyId] ?: KeyOverrideState()
                        ThemeBlockCard(title = stringResource(R.string.settings_theme_key_editing, keyId)) {
                            ColorPickerRow(
                                label = stringResource(R.string.settings_theme_key_background),
                                color = currentOverride.backgroundColor ?: android.graphics.Color.WHITE,
                                onColorChange = {
                                    advancedDirty = true
                                    advancedKeyOverrides[keyId] = currentOverride.copy(backgroundColor = it)
                                },
                                compact = true,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            ColorPickerRow(
                                label = stringResource(R.string.settings_theme_key_text),
                                color = currentOverride.textColor ?: android.graphics.Color.BLACK,
                                onColorChange = {
                                    advancedDirty = true
                                    advancedKeyOverrides[keyId] = currentOverride.copy(textColor = it)
                                },
                                compact = true,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            ThemeImagePickerRow(
                                label = stringResource(R.string.settings_theme_key_background_image),
                                path = currentOverride.backgroundImage,
                                onPick = {
                                    imagePickerTarget.value =
                                        ThemeImageTarget(
                                            themeId = THEME_ID_ADVANCED,
                                            onImageSelected = { path ->
                                                advancedDirty = true
                                                advancedKeyOverrides[keyId] = currentOverride.copy(backgroundImage = path)
                                            },
                                        )
                                    imagePickerLauncher.launch(arrayOf("image/*"))
                                },
                                onClear = {
                                    advancedDirty = true
                                    advancedKeyOverrides[keyId] = currentOverride.copy(backgroundImage = null)
                                },
                                compact = true,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    advancedDirty = true
                                    advancedKeyOverrides.remove(keyId)
                                },
                            ) {
                                Text(stringResource(R.string.settings_theme_key_clear))
                            }
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = {
                                val base = themeSpecs[advancedBaseThemeId] ?: return@Button
                                val spec =
                                    buildAdvancedThemeSpec(
                                        base = base,
                                        overrides = advancedKeyOverrides,
                                        layoutBgColor = advancedLayoutBgColor,
                                        layoutBgImage = advancedLayoutBgImage,
                                        name = context.getString(R.string.settings_theme_advanced_name),
                                    )
                                writeThemeSpec(context, spec)
                                prefs.keyboardThemeId = spec.themeId
                                selectedThemeId = spec.themeId
                                refreshThemes()
                            },
                        ) {
                            Text(stringResource(R.string.settings_theme_advanced_apply))
                        }
                        OutlinedButton(
                            onClick = {
                                advancedDirty = false
                                advancedKeyOverrides.clear()
                                advancedLayoutBgColor = null
                                advancedLayoutBgImage = null
                            },
                        ) {
                            Text(stringResource(R.string.settings_theme_reset))
                        }
                    }
                }
            }
        }

        item { SectionHeader(textRes = R.string.settings_section_emoji) }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_emoji_use_images)) },
                supportingContent = { Text(stringResource(R.string.settings_emoji_use_images_desc)) },
                trailingContent = {
                    Switch(
                        checked = emojiImages,
                        onCheckedChange = {
                            emojiImages = it
                            prefs.emojiImageEnabled = it
                        },
                    )
                },
            )
        }
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
    detailTag: String?,
    onDetailTagChange: (String?) -> Unit,
    showCreateLayoutDialog: Boolean,
    onShowCreateLayoutDialogChange: (Boolean) -> Unit,
    showImportLayout: Boolean,
    onShowImportLayoutChange: (Boolean) -> Unit,
    onOpenLayoutEditor: (localeTag: String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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

    val enabledLayoutsByLocale: SnapshotStateMap<String, List<String>> = remember { mutableStateMapOf() }
    val customLayoutsByLocale: SnapshotStateMap<String, List<String>> = remember { mutableStateMapOf() }
    val preferredLayoutByLocale: SnapshotStateMap<String, String?> = remember { mutableStateMapOf() }
    var searchQuery by remember { mutableStateOf("") }

    val themeManager = remember { ThemeManager(context).loadAll() }
    var currentThemeId by remember { mutableStateOf(prefs.keyboardThemeId) }
    DisposableEffect(prefs) {
        val listener = prefs.addOnChangeListener { currentThemeId = prefs.keyboardThemeId }
        onDispose { prefs.removeOnChangeListener(listener) }
    }
    val themeSpec = currentThemeId?.let { themeManager.getTheme(it) } ?: themeManager.getDefaultTheme()
    var layoutCatalogVersion by remember { mutableStateOf(0) }
    val allLayoutIds = remember(layoutCatalogVersion) { layoutManager.listLayoutIds().sorted() }
    val allLayoutIdSet = remember(allLayoutIds) { allLayoutIds.toSet() }

    fun loadCustomLayouts(tag: String): List<String> {
        val normalized = prefs.getCustomLayoutIds(tag)
            .map { it.trim() }
            .filter { it.isNotBlank() && it in allLayoutIdSet }
            .distinct()
        customLayoutsByLocale[tag] = normalized
        return normalized
    }

    fun ensureLocaleInitialized(tag: String) {
        val profile = subtypeManager.get(tag) ?: return
        val customLayouts = customLayoutsByLocale[tag] ?: loadCustomLayouts(tag)
        val ordered = (profile.layoutIds + customLayouts).distinct()
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

    val filteredProfiles =
        remember(profiles, searchQuery) {
            val query = searchQuery.trim().lowercase(Locale.ROOT)
            if (query.isBlank()) return@remember profiles
            profiles.filter { profile ->
                val label = formatLocaleLabel(profile.localeTag).lowercase(Locale.ROOT)
                label.contains(query) || profile.localeTag.lowercase(Locale.ROOT).contains(query)
            }
        }

    if (showCreateLayoutDialog && profiles.isEmpty()) {
        onShowCreateLayoutDialogChange(false)
    }

    if (showImportLayout && profiles.isEmpty()) {
        onShowImportLayoutChange(false)
    }

    var createLocaleTag by remember { mutableStateOf(detailTag ?: prefs.userLocaleTag ?: profiles.firstOrNull()?.localeTag.orEmpty()) }
    var createLanguageQuery by remember { mutableStateOf("") }
    var createLanguageMenuExpanded by remember { mutableStateOf(false) }
    var createError by remember { mutableStateOf<String?>(null) }
    val filteredCreateProfiles =
        remember(createLanguageQuery, profiles) {
            val query = createLanguageQuery.trim().lowercase(Locale.ROOT)
            if (query.isBlank()) return@remember profiles
            profiles.filter { profile ->
                val label = formatLocaleLabel(profile.localeTag).lowercase(Locale.ROOT)
                label.contains(query) || profile.localeTag.lowercase(Locale.ROOT).contains(query)
            }
        }

    LaunchedEffect(showCreateLayoutDialog) {
        if (showCreateLayoutDialog) {
            createLocaleTag = detailTag ?: prefs.userLocaleTag ?: profiles.firstOrNull()?.localeTag.orEmpty()
            createLanguageQuery = createLocaleTag.takeIf { it.isNotBlank() }?.let(::formatLocaleLabel).orEmpty()
            createError = null
        }
    }

    if (showCreateLayoutDialog) {
        AlertDialog(
            onDismissRequest = { onShowCreateLayoutDialogChange(false) },
            title = { Text(stringResource(R.string.settings_language_layout_create_title)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.settings_language_layout_create_language),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = createLanguageQuery,
                            onValueChange = {
                                createLanguageQuery = it
                                createLanguageMenuExpanded = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(stringResource(R.string.settings_language_layout_create_search)) },
                            singleLine = true,
                        )
                        DropdownMenu(
                            expanded = createLanguageMenuExpanded,
                            onDismissRequest = { createLanguageMenuExpanded = false },
                        ) {
                            filteredCreateProfiles.forEach { profileOption ->
                                DropdownMenuItem(
                                    text = { Text(formatLocaleLabel(profileOption.localeTag)) },
                                    onClick = {
                                        createLocaleTag = profileOption.localeTag
                                        createLanguageQuery = formatLocaleLabel(profileOption.localeTag)
                                        createLanguageMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                    if (createError != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = createError.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val tag = normalizeLocaleTag(createLocaleTag)
                        if (tag.isBlank()) {
                            createError = context.getString(R.string.settings_language_layout_create_missing_language)
                            return@TextButton
                        }
                        createError = null
                        onShowCreateLayoutDialogChange(false)
                        onOpenLayoutEditor(tag)
                    },
                    enabled = normalizeLocaleTag(createLocaleTag).isNotBlank(),
                ) {
                    Text(stringResource(R.string.settings_language_layout_create_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { onShowCreateLayoutDialogChange(false) }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }

    var importUri by remember { mutableStateOf<Uri?>(null) }
    var importLocaleTag by remember { mutableStateOf(detailTag ?: prefs.userLocaleTag ?: profiles.firstOrNull()?.localeTag.orEmpty()) }
    var importLayoutName by remember { mutableStateOf("") }
    var importLanguageMenuExpanded by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf<String?>(null) }
    val importFileName = importUri?.let { resolveDisplayName(context, it) }.orEmpty()
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            importUri = uri
            importError = null
        }
    }

    LaunchedEffect(showImportLayout) {
        if (showImportLayout) {
            importLocaleTag = detailTag ?: prefs.userLocaleTag ?: profiles.firstOrNull()?.localeTag.orEmpty()
            importLayoutName = ""
            importUri = null
            importError = null
        }
    }

    if (detailTag == null) {
        LazyColumn(modifier = modifier, contentPadding = WindowInsets.systemBars.asPaddingValues()) {
            item { SectionHeader(textRes = R.string.settings_language_layout_enabled_title) }
            item {
                EnabledLanguagesRow(
                    enabledTags = enabledLocaleTags,
                    preferredLayouts = preferredLayoutByLocale,
                    layoutManager = layoutManager,
                    onOpen = { tag ->
                        ensureLocaleInitialized(tag)
                        prefs.userLocaleTag = tag
                        onDetailTagChange(tag)
                    },
                    onDisable = { tag ->
                        if (enabledLocaleTags.size <= 1) return@EnabledLanguagesRow
                        enabledLocaleTags = enabledLocaleTags.filterNot { it == tag }
                        if (prefs.userLocaleTag == tag) {
                            prefs.userLocaleTag = enabledLocaleTags.firstOrNull()
                        }
                    },
                )
            }
            item {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    placeholder = { Text(stringResource(R.string.settings_language_layout_search_placeholder)) },
                    singleLine = true,
                )
            }
            item {
                SectionHeader(textRes = R.string.settings_language_layout_language)
            }
            item {
                Text(
                    text = stringResource(R.string.onboarding_layout_multi_select_hint),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (filteredProfiles.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.settings_language_layout_no_results),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(filteredProfiles, key = { it.localeTag }) { p ->
                val tag = p.localeTag
                val isCurrent = tag == currentLocaleTag
                LanguageCard(
                    title = formatLocaleLabel(tag),
                    subtitle = tag,
                    isDefault = isCurrent,
                    onClick = {
                        ensureLocaleInitialized(tag)
                        prefs.userLocaleTag = tag
                        onDetailTagChange(tag)
                    },
                )
            }
        }
    } else {
        val tag = detailTag ?: ""
        val profile = profiles.firstOrNull { it.localeTag == tag }
        if (profile == null) {
            onDetailTagChange(null)
            return
        }
        ensureLocaleInitialized(tag)
        val enabledLayouts = enabledLayoutsByLocale[tag].orEmpty()
        val preferredLayoutId = preferredLayoutByLocale[tag]
        val customLayouts = customLayoutsByLocale[tag] ?: loadCustomLayouts(tag)
        val availableLayouts = (profile.layoutIds + customLayouts).distinct()
        LazyColumn(modifier = modifier, contentPadding = WindowInsets.systemBars.asPaddingValues()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.settings_language_layout_layouts),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    val enabled = tag in enabledLocaleTags
                    TextButton(
                        onClick = {
                            if (enabled && enabledLocaleTags.size <= 1) return@TextButton
                            if (enabled) {
                                enabledLocaleTags = enabledLocaleTags.filterNot { it == tag }
                                onDetailTagChange(null)
                                if (prefs.userLocaleTag == tag) {
                                    prefs.userLocaleTag = enabledLocaleTags.firstOrNull()
                                }
                            } else {
                                enabledLocaleTags = (enabledLocaleTags + tag).distinct()
                                prefs.userLocaleTag = tag
                            }
                        },
                    ) {
                        Text(
                            text = if (enabled) stringResource(R.string.settings_language_layout_disable)
                            else stringResource(R.string.settings_language_layout_enable),
                        )
                    }
                }
            }
            items(availableLayouts, key = { it }) { layoutId ->
                val layout = runCatching { layoutManager.getLayout(layoutId) }.getOrNull()
                val layoutName = layout?.name?.takeIf { it.isNotBlank() } ?: layoutId
                val enabled = layoutId in enabledLayouts
                val preferred = preferredLayoutId == layoutId
                val isCustom = layoutId !in profile.layoutIds && layoutId in customLayouts
                val subtitle =
                    if (isCustom) {
                        stringResource(R.string.settings_language_layout_custom_tag, layoutId)
                    } else {
                        layoutId
                    }
                LayoutThumbnailCard(
                    title = layoutName,
                    subtitle = subtitle,
                    enabled = enabled,
                    preferred = preferred,
                    themeSpec = themeSpec,
                    layout = layout,
                    onToggle = { next ->
                        val set = enabledLayouts.toMutableSet()
                        if (next) set.add(layoutId) else if (set.size > 1) set.remove(layoutId)
                        val nextEnabled = availableLayouts.filter { it in set }.distinct()
                        enabledLayoutsByLocale[tag] = nextEnabled
                        prefs.setEnabledLayoutIds(tag, nextEnabled)

                        val preferredLocal = prefs.getPreferredLayoutId(tag)
                        val nextPreferred = preferredLocal?.takeIf { it in nextEnabled } ?: nextEnabled.firstOrNull()
                        preferredLayoutByLocale[tag] = nextPreferred
                        prefs.setPreferredLayoutId(tag, nextPreferred)
                    },
                    onSetDefault = {
                        if (!enabled) {
                            val set = enabledLayouts.toMutableSet()
                            set.add(layoutId)
                            val nextEnabled = availableLayouts.filter { it in set }.distinct()
                            enabledLayoutsByLocale[tag] = nextEnabled
                            prefs.setEnabledLayoutIds(tag, nextEnabled)
                        }
                        preferredLayoutByLocale[tag] = layoutId
                        prefs.setPreferredLayoutId(tag, layoutId)
                    },
                )
            }
        }
    }

    if (showImportLayout) {
        val normalizedTag = normalizeLocaleTag(importLocaleTag)
        val computedLayoutId = sanitizeLayoutIdFromName(importLayoutName)
        val canImport =
            normalizedTag.isNotBlank() &&
                importLayoutName.trim().isNotBlank() &&
                importUri != null &&
                computedLayoutId.isNotBlank() &&
                computedLayoutId !in allLayoutIdSet
        AlertDialog(
            onDismissRequest = { onShowImportLayoutChange(false) },
            title = { Text(stringResource(R.string.settings_language_layout_import_title)) },
            text = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.settings_language_layout_import_file),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        TextButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/*")) }) {
                            Text(stringResource(R.string.settings_language_layout_import_choose_file))
                        }
                    }
                    if (importFileName.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.settings_language_layout_import_selected, importFileName),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.settings_language_layout_import_language),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = formatLocaleLabel(importLocaleTag),
                            onValueChange = {},
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { importLanguageMenuExpanded = true },
                            readOnly = true,
                            enabled = profiles.isNotEmpty(),
                        )
                        DropdownMenu(
                            expanded = importLanguageMenuExpanded,
                            onDismissRequest = { importLanguageMenuExpanded = false },
                        ) {
                            profiles.forEach { profileOption ->
                                DropdownMenuItem(
                                    text = { Text(formatLocaleLabel(profileOption.localeTag)) },
                                    onClick = {
                                        importLocaleTag = profileOption.localeTag
                                        importLanguageMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = importLayoutName,
                        onValueChange = { importLayoutName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.settings_language_layout_import_name)) },
                        singleLine = true,
                    )
                    if (computedLayoutId.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.settings_language_layout_import_id_preview, computedLayoutId),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (importError != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = importError.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val uri = importUri
                        if (uri == null) {
                            importError = context.getString(R.string.settings_language_layout_import_missing_file)
                            return@TextButton
                        }
                        val tag = normalizeLocaleTag(importLocaleTag)
                        if (tag.isBlank()) {
                            importError = context.getString(R.string.settings_language_layout_import_missing_language)
                            return@TextButton
                        }
                        val name = importLayoutName.trim()
                        if (name.isBlank()) {
                            importError = context.getString(R.string.settings_language_layout_import_missing_name)
                            return@TextButton
                        }
                        val layoutId = sanitizeLayoutIdFromName(name)
                        if (layoutId.isBlank()) {
                            importError = context.getString(R.string.settings_language_layout_import_invalid_name)
                            return@TextButton
                        }
                        if (layoutId in allLayoutIdSet) {
                            importError = context.getString(R.string.settings_language_layout_import_duplicate, layoutId)
                            return@TextButton
                        }
                        scope.launch {
                            val result =
                                withContext(Dispatchers.IO) {
                                    val text =
                                        runCatching {
                                            context.contentResolver.openInputStream(uri)
                                                ?.bufferedReader(Charsets.UTF_8)
                                                ?.use { it.readText() }
                                        }.getOrNull().orEmpty()
                                    if (text.isBlank()) {
                                        return@withContext context.getString(R.string.settings_language_layout_import_json_empty)
                                    }
                                    val parsed = runCatching { LayoutParser.parse(text) }.getOrNull()
                                        ?: return@withContext context.getString(R.string.settings_language_layout_import_json_invalid)
                                    val updated = parsed.copy(
                                        layoutId = layoutId,
                                        name = name,
                                        locale = listOf(tag),
                                    )
                                    val errors = updated.validate()
                                    if (errors.isNotEmpty()) {
                                        return@withContext context.getString(
                                            R.string.settings_language_layout_import_json_invalid_detail,
                                            errors.joinToString(", "),
                                        )
                                    }
                                    writeUserLayoutSpec(context, updated)
                                    null
                                }
                            if (result != null) {
                                importError = result
                                return@launch
                            }
                            layoutManager.reloadAll()
                            layoutCatalogVersion += 1

                            if (tag !in enabledLocaleTags) {
                                enabledLocaleTags = (enabledLocaleTags + tag).distinct()
                            }
                            val nextCustom = (customLayoutsByLocale[tag].orEmpty() + layoutId).distinct()
                            customLayoutsByLocale[tag] = nextCustom
                            prefs.setCustomLayoutIds(tag, nextCustom)

                            val nextEnabled = (enabledLayoutsByLocale[tag].orEmpty() + layoutId).distinct()
                            enabledLayoutsByLocale[tag] = nextEnabled
                            prefs.setEnabledLayoutIds(tag, nextEnabled)

                            val nextPreferred = prefs.getPreferredLayoutId(tag)
                                ?.takeIf { it in nextEnabled } ?: nextEnabled.firstOrNull()
                            preferredLayoutByLocale[tag] = nextPreferred
                            prefs.setPreferredLayoutId(tag, nextPreferred)

                            importLayoutName = ""
                            importUri = null
                            importError = null
                            onShowImportLayoutChange(false)
                        }
                    },
                    enabled = canImport,
                ) {
                    Text(stringResource(R.string.settings_language_layout_import_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { onShowImportLayoutChange(false) }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
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
private fun SuggestionsSettings(
    modifier: Modifier,
    prefs: SettingsStore,
) {
    val context = LocalContext.current
    val suggestionManager = remember { xyz.xiao6.myboard.suggest.SuggestionManager(context, prefs) }
    val localeTag = prefs.userLocaleTag ?: Locale.getDefault().toLanguageTag()
    var enabled by remember { mutableStateOf(prefs.suggestionEnabled) }
    var learningEnabled by remember { mutableStateOf(prefs.suggestionLearningEnabled) }
    var ngramEnabled by remember { mutableStateOf(prefs.suggestionNgramEnabled) }
    var cloudEnabled by remember { mutableStateOf(prefs.suggestionCloudEnabled) }
    var benchmarkDisableCandidates by remember { mutableStateOf(prefs.benchmarkDisableCandidates) }
    var benchmarkDisableKeyPreview by remember { mutableStateOf(prefs.benchmarkDisableKeyPreview) }
    var benchmarkDisableKeyDecorations by remember { mutableStateOf(prefs.benchmarkDisableKeyDecorations) }
    var benchmarkDisableKeyLabels by remember { mutableStateOf(prefs.benchmarkDisableKeyLabels) }
    var debugTouchLoggingEnabled by remember { mutableStateOf(prefs.debugTouchLoggingEnabled) }
    var endpoint by remember { mutableStateOf(prefs.suggestionCloudEndpoint.orEmpty()) }
    var authType by remember { mutableStateOf(prefs.suggestionCloudAuthType) }
    var authValue by remember { mutableStateOf(prefs.suggestionCloudAuthValue.orEmpty()) }
    var headersJson by remember { mutableStateOf(prefs.suggestionCloudHeadersJson.orEmpty()) }

    val authOptions = listOf("NONE", "API_KEY", "BEARER", "CUSTOM_HEADERS")

    Column(modifier = modifier.padding(16.dp)) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_suggestions_enabled)) },
            trailingContent = {
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        enabled = it
                        prefs.suggestionEnabled = it
                    },
                )
            },
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_suggestions_benchmark_disable)) },
            supportingContent = { Text(stringResource(R.string.settings_suggestions_benchmark_disable_desc)) },
            trailingContent = {
                Switch(
                    checked = benchmarkDisableCandidates,
                    onCheckedChange = {
                        benchmarkDisableCandidates = it
                        prefs.benchmarkDisableCandidates = it
                    },
                )
            },
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_benchmark_disable_key_preview)) },
            supportingContent = { Text(stringResource(R.string.settings_benchmark_disable_key_preview_desc)) },
            trailingContent = {
                Switch(
                    checked = benchmarkDisableKeyPreview,
                    onCheckedChange = {
                        benchmarkDisableKeyPreview = it
                        prefs.benchmarkDisableKeyPreview = it
                    },
                )
            },
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_benchmark_disable_key_decorations)) },
            supportingContent = { Text(stringResource(R.string.settings_benchmark_disable_key_decorations_desc)) },
            trailingContent = {
                Switch(
                    checked = benchmarkDisableKeyDecorations,
                    onCheckedChange = {
                        benchmarkDisableKeyDecorations = it
                        prefs.benchmarkDisableKeyDecorations = it
                    },
                )
            },
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_benchmark_disable_key_labels)) },
            supportingContent = { Text(stringResource(R.string.settings_benchmark_disable_key_labels_desc)) },
            trailingContent = {
                Switch(
                    checked = benchmarkDisableKeyLabels,
                    onCheckedChange = {
                        benchmarkDisableKeyLabels = it
                        prefs.benchmarkDisableKeyLabels = it
                    },
                )
            },
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_debug_touch_logging)) },
            supportingContent = { Text(stringResource(R.string.settings_debug_touch_logging_desc)) },
            trailingContent = {
                Switch(
                    checked = debugTouchLoggingEnabled,
                    onCheckedChange = {
                        debugTouchLoggingEnabled = it
                        prefs.debugTouchLoggingEnabled = it
                    },
                )
            },
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_suggestions_learning)) },
            trailingContent = {
                Switch(
                    checked = learningEnabled,
                    onCheckedChange = {
                        learningEnabled = it
                        prefs.suggestionLearningEnabled = it
                    },
                    enabled = enabled,
                )
            },
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_suggestions_ngram)) },
            supportingContent = { Text(stringResource(R.string.settings_suggestions_ngram_desc)) },
            trailingContent = {
                Switch(
                    checked = ngramEnabled,
                    onCheckedChange = {
                        ngramEnabled = it
                        prefs.suggestionNgramEnabled = it
                    },
                    enabled = enabled && learningEnabled,
                )
            },
        )
        Row(modifier = Modifier.padding(top = 8.dp)) {
            TextButton(onClick = { suggestionManager.clearLearning(localeTag) }) {
                Text(text = stringResource(R.string.settings_suggestions_clear_learning))
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = { suggestionManager.clearBlocked(localeTag) }) {
                Text(text = stringResource(R.string.settings_suggestions_clear_blocked))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_suggestions_cloud)) },
            trailingContent = {
                Switch(
                    checked = cloudEnabled,
                    onCheckedChange = {
                        cloudEnabled = it
                        prefs.suggestionCloudEnabled = it
                    },
                )
            },
        )
        if (cloudEnabled) {
            TextField(
                value = endpoint,
                onValueChange = {
                    endpoint = it
                    prefs.suggestionCloudEndpoint = it
                },
                label = { Text(stringResource(R.string.settings_suggestions_cloud_endpoint)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_suggestions_cloud_auth_type)) },
                supportingContent = { Text(authType) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val idx = authOptions.indexOf(authType).coerceAtLeast(0)
                        val next = authOptions[(idx + 1) % authOptions.size]
                        authType = next
                        prefs.suggestionCloudAuthType = next
                    },
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = authValue,
                onValueChange = {
                    authValue = it
                    prefs.suggestionCloudAuthValue = it
                },
                label = { Text(stringResource(R.string.settings_suggestions_cloud_auth_value)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = headersJson,
                onValueChange = {
                    headersJson = it
                    prefs.suggestionCloudHeadersJson = it
                },
                label = { Text(stringResource(R.string.settings_suggestions_cloud_headers_json)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_suggestions_cloud_placeholder),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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

private data class ThemeOption(
    val themeId: String,
    val name: String,
)

private data class ThemeImportResult(
    val themeId: String,
    val name: String?,
)

@OptIn(ExperimentalSerializationApi::class)
private val layoutJson =
    Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = false
    }

private fun sanitizeLayoutIdFromName(name: String): String {
    val normalized = name.trim().lowercase(Locale.ROOT)
    return normalized.replace(Regex("[^a-z0-9]+"), "_").trim('_')
}

private fun trySaveLayout(
    context: android.content.Context,
    state: LayoutEditorState,
    existingLayoutIds: Set<String>,
    layoutManager: LayoutManager,
    prefs: SettingsStore,
    onSaved: () -> Unit,
    onError: (String) -> Unit,
) {
    val name = state.name.trim()
    if (name.isBlank()) {
        onError(context.getString(R.string.layout_editor_error_missing_name))
        return
    }
    val nextId = sanitizeLayoutIdFromName(name)
    if (nextId.isBlank()) {
        onError(context.getString(R.string.layout_editor_error_missing_name))
        return
    }
    if (nextId in existingLayoutIds) {
        onError(context.getString(R.string.layout_editor_error_duplicate_id, nextId))
        return
    }
    val invalidKey = state.rows.flatMap { it.keys }.firstOrNull {
        it.type == EditorKeyType.TEXT && it.label.trim().isBlank()
    }
    if (invalidKey != null) {
        onError(context.getString(R.string.layout_editor_error_empty_label))
        return
    }
    val layout = buildKeyboardLayoutFromEditor(state)
    val errors = layout.validate()
    if (errors.isNotEmpty()) {
        onError(context.getString(R.string.layout_editor_error_invalid_layout, errors.joinToString(", ")))
        return
    }
    writeUserLayoutSpec(context, layout)
    layoutManager.reloadAll()
    val tag = normalizeLocaleTag(layout.locale.firstOrNull().orEmpty())
    if (tag.isNotBlank()) {
        if (tag !in prefs.getEnabledLocaleTags()) {
            prefs.setEnabledLocaleTags(prefs.getEnabledLocaleTags() + tag)
        }
        val nextCustom = (prefs.getCustomLayoutIds(tag) + layout.layoutId).distinct()
        prefs.setCustomLayoutIds(tag, nextCustom)
        val nextEnabled = (prefs.getEnabledLayoutIds(tag) + layout.layoutId).distinct()
        prefs.setEnabledLayoutIds(tag, nextEnabled)
        prefs.setPreferredLayoutId(tag, prefs.getPreferredLayoutId(tag) ?: layout.layoutId)
        prefs.userLocaleTag = tag
    }
    onSaved()
}

private enum class EditorKeyType {
    TEXT,
    SPACE,
    ENTER,
    BACKSPACE,
    SHIFT,
    TAB,
}

private class EditorKeyState(
    val uid: String,
    label: String,
    type: EditorKeyType,
    longPress: String,
) {
    var label by mutableStateOf(label)
    var type by mutableStateOf(type)
    var longPress by mutableStateOf(longPress)
}

private class EditorRowState(
    val rowId: String,
    keys: List<EditorKeyState>,
    heightWeight: Float,
) {
    val keys = mutableStateListOf<EditorKeyState>().apply { addAll(keys) }
    var heightWeight by mutableStateOf(heightWeight)
}

private class LayoutEditorState(
    val localeTag: String,
    name: String,
    columns: Int,
    horizontalGapDp: Float,
    verticalGapDp: Float,
    rows: List<EditorRowState>,
) {
    var name by mutableStateOf(name)
    var columns by mutableStateOf(columns)
    var horizontalGapDp by mutableStateOf(horizontalGapDp)
    var verticalGapDp by mutableStateOf(verticalGapDp)
    val rows = mutableStateListOf<EditorRowState>().apply { addAll(rows) }
}

private fun buildLayoutEditorState(
    localeTag: String,
    layoutManager: LayoutManager,
): LayoutEditorState {
    val layout = runCatching { layoutManager.getDefaultLayout(Locale.forLanguageTag(localeTag)) }.getOrNull()
    val rows =
        if (layout == null) {
            List(4) { idx ->
                EditorRowState(
                    rowId = "row_custom_${idx + 1}",
                    keys = emptyList(),
                    heightWeight = 1f,
                )
            }
        } else {
            layout.rows.mapIndexed { idx, row ->
                val mappedKeys = row.keys.map { mapToEditorKeyState(it) }
                EditorRowState(
                    rowId = row.rowId.ifBlank { "row_custom_${idx + 1}" },
                    keys = mappedKeys,
                    heightWeight = row.heightRatio.coerceAtLeast(0.1f),
                )
            }
        }
    val maxCols = layout?.rows?.maxOfOrNull { it.keys.size }?.coerceIn(2, 14) ?: 10
    val hGap = layout?.defaults?.horizontalGapDp ?: 4f
    val vGap = layout?.defaults?.verticalGapDp ?: 5f
    return LayoutEditorState(
        localeTag = normalizeLocaleTag(localeTag),
        name = "",
        columns = maxCols,
        horizontalGapDp = hGap,
        verticalGapDp = vGap,
        rows = rows,
    )
}

private fun mapToEditorKeyState(key: Key): EditorKeyState {
    val type =
        when {
            key.keyId == KeyIds.BACKSPACE || key.primaryCode == KeyPrimaryCodes.BACKSPACE -> EditorKeyType.BACKSPACE
            key.keyId == KeyIds.SPACE || key.primaryCode == KeyPrimaryCodes.SPACE -> EditorKeyType.SPACE
            key.keyId == KeyIds.ENTER || key.primaryCode == KeyPrimaryCodes.ENTER -> EditorKeyType.ENTER
            key.actions.values.any { it.actionType == ActionType.TOGGLE_MODIFIER } -> EditorKeyType.SHIFT
            key.actions.values.any { action ->
                action.defaultActions.any { it is Action.Command && it.commandType == CommandType.TAB }
            } -> EditorKeyType.TAB
            else -> EditorKeyType.TEXT
        }
    val label =
        key.label
            ?: key.ui.label
            ?: key.primaryCode.takeIf { it > 0 }?.let { kotlin.runCatching { String(Character.toChars(it)) }.getOrNull() }
            ?: ""
    val longPress =
        key.actions[GestureType.LONG_PRESS]?.defaultActions
            ?.firstOrNull { it is Action.CommitText }
            ?.let { (it as Action.CommitText).text }
            .orEmpty()
    return EditorKeyState(
        uid = "k_${System.nanoTime()}",
        label = label,
        type = type,
        longPress = longPress,
    )
}

private fun buildKeyboardLayoutFromEditor(state: LayoutEditorState): KeyboardLayout {
    val layoutId = sanitizeLayoutIdFromName(state.name).ifBlank { "layout_${System.currentTimeMillis()}" }
    val totalWeight = state.rows.sumOf { it.heightWeight.toDouble() }.toFloat().takeIf { it > 0f } ?: 1f
    val rows =
        state.rows.mapIndexed { rowIndex, row ->
            val heightRatio = (row.heightWeight / totalWeight).coerceAtLeast(0.05f)
            val keys = row.keys.mapIndexed { colIndex, keyState ->
                buildKeyFromEditor(
                    keyState = keyState,
                    rowIndex = rowIndex,
                    colIndex = colIndex,
                )
            }
            KeyboardRow(
                rowId = row.rowId.ifBlank { "row_custom_${rowIndex + 1}" },
                heightRatio = heightRatio,
                alignment = RowAlignment.JUSTIFY,
                keys = keys,
            )
        }
    return KeyboardLayout(
        layoutId = layoutId,
        name = state.name.trim(),
        locale = listOf(state.localeTag),
        totalWidthRatio = 1.0f,
        totalHeightRatio = 0.25f,
        defaults = LayoutDefaults(
            horizontalGapDp = state.horizontalGapDp.coerceAtLeast(0f),
            verticalGapDp = state.verticalGapDp.coerceAtLeast(0f),
            padding = LayoutPadding(topDp = 6f, bottomDp = 6f, leftDp = 6f, rightDp = 6f),
        ),
        rows = rows,
    )
}

private data class EditorKeySpec(
    val primaryCode: Int,
    val label: String,
    val actions: Map<GestureType, KeyAction>,
    val styleId: String,
    val widthWeight: Float,
)

private fun buildKeyFromEditor(
    keyState: EditorKeyState,
    rowIndex: Int,
    colIndex: Int,
): Key {
    val spec =
        when (keyState.type) {
            EditorKeyType.SPACE -> {
                val act = KeyAction(
                    actionType = ActionType.COMMAND,
                    defaultActions = listOf(Action.Command(CommandType.SPACE)),
                )
                EditorKeySpec(
                    primaryCode = KeyPrimaryCodes.SPACE,
                    label = "",
                    actions = mapOf(GestureType.TAP to act),
                    styleId = "style_function_key_important",
                    widthWeight = 3f,
                )
            }
            EditorKeyType.ENTER -> {
                val act = KeyAction(
                    actionType = ActionType.COMMAND,
                    defaultActions = listOf(Action.Command(CommandType.ENTER)),
                )
                EditorKeySpec(
                    primaryCode = KeyPrimaryCodes.ENTER,
                    label = keyState.label.ifBlank { "Enter" },
                    actions = mapOf(GestureType.TAP to act),
                    styleId = "style_function_key_important",
                    widthWeight = 1f,
                )
            }
            EditorKeyType.BACKSPACE -> {
                val act = KeyAction(
                    actionType = ActionType.COMMAND,
                    defaultActions = listOf(Action.Command(CommandType.BACKSPACE)),
                )
                EditorKeySpec(
                    primaryCode = KeyPrimaryCodes.BACKSPACE,
                    label = keyState.label.ifBlank { "Bksp" },
                    actions = mapOf(GestureType.TAP to act),
                    styleId = "style_function_key",
                    widthWeight = 1f,
                )
            }
            EditorKeyType.SHIFT -> {
                val act = KeyAction(
                    actionType = ActionType.TOGGLE_MODIFIER,
                    cases = listOf(
                        KeyActionCase(
                            whenCondition = WhenCondition(),
                            doActions = listOf(Action.ToggleModifier(ModifierKey.SHIFT)),
                        ),
                    ),
                )
                EditorKeySpec(
                    primaryCode = KeyPrimaryCodes.SHIFT,
                    label = keyState.label.ifBlank { "Shift" },
                    actions = mapOf(GestureType.TAP to act),
                    styleId = "style_function_key",
                    widthWeight = 1f,
                )
            }
            EditorKeyType.TAB -> {
                val act = KeyAction(
                    actionType = ActionType.COMMAND,
                    defaultActions = listOf(Action.Command(CommandType.TAB)),
                )
                EditorKeySpec(
                    primaryCode = 0,
                    label = keyState.label.ifBlank { "Tab" },
                    actions = mapOf(GestureType.TAP to act),
                    styleId = "style_function_key",
                    widthWeight = 1f,
                )
            }
            EditorKeyType.TEXT -> {
                val text = keyState.label.trim()
                val code = text.firstOrNull()?.code ?: 0
                val tap = KeyAction(
                    actionType = ActionType.PUSH_TOKEN,
                    fallback = KeyActionDefault.PRIMARY_CODE_AS_TOKEN,
                )
                val actions = buildMap<GestureType, KeyAction> {
                    put(GestureType.TAP, tap)
                    val longPress = keyState.longPress.trim()
                    if (longPress.isNotBlank()) {
                        put(
                            GestureType.LONG_PRESS,
                            KeyAction(
                                actionType = ActionType.COMMIT_TEXT,
                                defaultActions = listOf(Action.CommitText(longPress)),
                            ),
                        )
                    }
                }
                EditorKeySpec(
                    primaryCode = code,
                    label = text,
                    actions = actions,
                    styleId = "style_alpha_key",
                    widthWeight = 1f,
                )
            }
        }

    val keyId = buildUniqueKeyId(keyState, rowIndex, colIndex)
    return Key(
        keyId = keyId,
        primaryCode = spec.primaryCode,
        label = spec.label,
        ui = KeyUI(
            label = null,
            styleId = spec.styleId,
            gridPosition = GridPosition(startCol = colIndex, startRow = rowIndex, spanCols = 1),
            widthWeight = spec.widthWeight,
        ),
        actions = spec.actions,
    )
}

private fun buildUniqueKeyId(
    keyState: EditorKeyState,
    rowIndex: Int,
    colIndex: Int,
): String {
    val base =
        when (keyState.type) {
            EditorKeyType.SPACE -> KeyIds.SPACE
            EditorKeyType.ENTER -> KeyIds.ENTER
            EditorKeyType.BACKSPACE -> KeyIds.BACKSPACE
            EditorKeyType.SHIFT -> "key_shift"
            EditorKeyType.TAB -> "key_tab"
            EditorKeyType.TEXT -> {
                val normalized = keyState.label.trim().lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "_")
                "key_${normalized.ifBlank { "key" }}"
            }
        }
    return "${base}_${rowIndex}_${colIndex}"
}

@Composable
private fun LayoutEditorScreen(
    modifier: Modifier,
    state: LayoutEditorState,
    errorText: String?,
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val rowBounds = remember { mutableStateMapOf<String, Rect>() }
    var trashBounds by remember { mutableStateOf<Rect?>(null) }
    var selectedKey by remember { mutableStateOf<EditorKeyState?>(null) }
    var dragState by remember { mutableStateOf<DraggingKeyState?>(null) }
    var rootCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val typeOptions = remember {
        listOf(
            EditorKeyType.TEXT to R.string.layout_editor_key_type_text,
            EditorKeyType.SPACE to R.string.layout_editor_key_type_space,
            EditorKeyType.ENTER to R.string.layout_editor_key_type_enter,
            EditorKeyType.BACKSPACE to R.string.layout_editor_key_type_backspace,
            EditorKeyType.SHIFT to R.string.layout_editor_key_type_shift,
            EditorKeyType.TAB to R.string.layout_editor_key_type_tab,
        )
    }

    fun defaultLabelForType(type: EditorKeyType): String {
        return when (type) {
            EditorKeyType.TEXT -> "a"
            EditorKeyType.SPACE -> context.getString(R.string.layout_editor_key_label_space)
            EditorKeyType.ENTER -> context.getString(R.string.layout_editor_key_label_enter)
            EditorKeyType.BACKSPACE -> context.getString(R.string.layout_editor_key_label_backspace)
            EditorKeyType.SHIFT -> context.getString(R.string.layout_editor_key_label_shift)
            EditorKeyType.TAB -> context.getString(R.string.layout_editor_key_label_tab)
        }
    }

    fun updateColumns(next: Int) {
        val clamped = next.coerceIn(2, 14)
        if (clamped == state.columns) return
        state.columns = clamped
        state.rows.forEach { row ->
            while (row.keys.size > clamped) {
                row.keys.removeAt(row.keys.size - 1)
            }
        }
    }

    fun startDrag(key: EditorKeyState, sourceRowId: String?, positionInWindow: Offset, fromPalette: Boolean) {
        dragState = DraggingKeyState(
            key = key,
            sourceRowId = sourceRowId,
            fromPalette = fromPalette,
            positionInWindow = positionInWindow,
        )
    }

    fun updateDrag(positionInWindow: Offset) {
        dragState = dragState?.copy(positionInWindow = positionInWindow)
    }

    fun handleDrop() {
        val drag = dragState ?: return
        val position = drag.positionInWindow
        val targetRowEntry = rowBounds.entries.firstOrNull { it.value.contains(position) }
        val trash = trashBounds
        val sourceRow = drag.sourceRowId?.let { id -> state.rows.firstOrNull { it.rowId == id } }

        if (targetRowEntry != null) {
            val row = state.rows.firstOrNull { it.rowId == targetRowEntry.key }
            if (row != null) {
                val columns = maxOf(state.columns.coerceAtLeast(1), row.keys.size)
                if (drag.fromPalette && row.keys.size >= columns) {
                    dragState = null
                    return
                }
                if (!drag.fromPalette && sourceRow != null) {
                    sourceRow.keys.remove(drag.key)
                }
                val rect = targetRowEntry.value
                val slotWidth = (rect.width / columns.toFloat()).coerceAtLeast(1f)
                val rawIndex = ((position.x - rect.left) / slotWidth).toInt()
                val insertIndex = rawIndex.coerceIn(0, row.keys.size)
                val keyToInsert =
                    if (drag.fromPalette) {
                        val newKey = EditorKeyState(
                            uid = "k_${System.nanoTime()}",
                            label = defaultLabelForType(drag.key.type),
                            type = drag.key.type,
                            longPress = "",
                        )
                        newKey
                    } else {
                        drag.key
                    }
                row.keys.add(insertIndex, keyToInsert)
            }
        } else if (!drag.fromPalette && trash?.contains(position) == true) {
            sourceRow?.keys?.remove(drag.key)
            if (selectedKey == drag.key) selectedKey = null
        }
        dragState = null
    }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
                .onGloballyPositioned { rootCoords = it },
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.layout_editor_preview_title),
                style = MaterialTheme.typography.titleMedium,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.layout_editor_columns, state.columns),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { updateColumns(state.columns - 1) },
                        enabled = state.columns > 2,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    ) {
                        Text("-")
                    }
                    OutlinedButton(
                        onClick = { updateColumns(state.columns + 1) },
                        enabled = state.columns < 14,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    ) {
                        Text("+")
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(state.verticalGapDp.dp)) {
                val maxWeight = state.rows.maxOfOrNull { it.heightWeight } ?: 1f
                state.rows.forEachIndexed { rowIndex, row ->
                    val rowHeight = (48.dp * (row.heightWeight / maxWeight).coerceIn(0.6f, 1.8f))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val nextIndex = rowIndex + 1
                                    state.rows.add(
                                        nextIndex,
                                        EditorRowState(
                                            rowId = "row_custom_${System.currentTimeMillis()}",
                                            keys = emptyList(),
                                            heightWeight = 1f,
                                        ),
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            ) {
                                Text("+")
                            }
                            OutlinedButton(
                                onClick = {
                                    if (state.rows.size > 1) {
                                        state.rows.removeAt(rowIndex)
                                    }
                                },
                                enabled = state.rows.size > 1,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            ) {
                                Text("-")
                            }
                        }
                        BoxWithConstraints(
                            modifier = Modifier
                                .weight(1f)
                                .onGloballyPositioned { coords ->
                                    rowBounds[row.rowId] = coords.boundsInWindow()
                                },
                        ) {
                            val columns = maxOf(state.columns.coerceAtLeast(1), row.keys.size)
                            val gap = state.horizontalGapDp.dp
                            val totalGap = gap * (columns - 1).coerceAtLeast(0)
                            val keyWidth = ((maxWidth - totalGap) / columns).coerceAtLeast(24.dp)
                            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                                val slots = columns
                                for (index in 0 until slots) {
                                    if (index < row.keys.size) {
                                        val key = row.keys[index]
                                        EditorKeyBox(
                                            key = key,
                                            width = keyWidth,
                                            height = rowHeight,
                                            selected = key == selectedKey,
                                            onSelect = { selectedKey = key },
                                            onStartDrag = { pos -> startDrag(key, row.rowId, pos, false) },
                                            onDrag = { updateDrag(it) },
                                            onDrop = { handleDrop() },
                                            onAddAfter = {
                                                val newKey = EditorKeyState(
                                                    uid = "k_${System.nanoTime()}",
                                                    label = defaultLabelForType(EditorKeyType.TEXT),
                                                    type = EditorKeyType.TEXT,
                                                    longPress = "",
                                                )
                                                row.keys.add(index + 1, newKey)
                                            },
                                            onRemove = {
                                                row.keys.removeAt(index)
                                                if (selectedKey == key) selectedKey = null
                                            },
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(keyWidth, rowHeight)
                                                .border(
                                                    width = 1.dp,
                                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                                    shape = RoundedCornerShape(10.dp),
                                                ),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.layout_editor_horizontal_gap, state.horizontalGapDp.toInt()),
                    style = MaterialTheme.typography.bodySmall,
                )
                Slider(
                    value = state.horizontalGapDp,
                    onValueChange = { state.horizontalGapDp = it },
                    valueRange = 0f..12f,
                )
                Text(
                    text = stringResource(R.string.layout_editor_vertical_gap, state.verticalGapDp.toInt()),
                    style = MaterialTheme.typography.bodySmall,
                )
                Slider(
                    value = state.verticalGapDp,
                    onValueChange = { state.verticalGapDp = it },
                    valueRange = 0f..14f,
                )
                Text(
                    text = stringResource(R.string.layout_editor_row_height),
                    style = MaterialTheme.typography.titleSmall,
                )
                state.rows.forEachIndexed { index, row ->
                    Column {
                        Text(
                            text = stringResource(R.string.layout_editor_row_label, index + 1),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Slider(
                            value = row.heightWeight,
                            onValueChange = { row.heightWeight = it.coerceIn(0.5f, 2f) },
                            valueRange = 0.5f..2f,
                        )
                    }
                }
            }
        }

        Text(
            text = stringResource(R.string.layout_editor_palette_title),
            style = MaterialTheme.typography.titleMedium,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(typeOptions, key = { it.first.name }) { (type, labelRes) ->
                val paletteKey = EditorKeyState(
                    uid = "palette_${type.name}",
                    label = defaultLabelForType(type),
                    type = type,
                    longPress = "",
                )
                EditorKeyBox(
                    key = paletteKey,
                    width = 68.dp,
                    height = 44.dp,
                    selected = false,
                    onSelect = {},
                    onStartDrag = { pos -> startDrag(paletteKey, null, pos, true) },
                    onDrag = { updateDrag(it) },
                    onDrop = { handleDrop() },
                    labelOverride = stringResource(labelRes),
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coords -> trashBounds = coords.boundsInWindow() },
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.layout_editor_trash_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        selectedKey?.let { key ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.layout_editor_key_editor),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    var typeMenuExpanded by remember(key) { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { typeMenuExpanded = true }) {
                            Text(
                                text = stringResource(
                                    typeOptions.first { it.first == key.type }.second,
                                ),
                            )
                        }
                        DropdownMenu(
                            expanded = typeMenuExpanded,
                            onDismissRequest = { typeMenuExpanded = false },
                        ) {
                            typeOptions.forEach { (type, labelRes) ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(labelRes)) },
                                    onClick = {
                                        key.type = type
                                        if (type == EditorKeyType.TEXT) {
                                            if (key.label.isBlank()) {
                                                key.label = defaultLabelForType(type)
                                            }
                                        } else {
                                            key.label = defaultLabelForType(type)
                                            key.longPress = ""
                                        }
                                        typeMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                    if (key.type == EditorKeyType.TEXT) {
                        OutlinedTextField(
                            value = key.label,
                            onValueChange = { key.label = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.layout_editor_key_label)) },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = key.longPress,
                            onValueChange = { key.longPress = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.layout_editor_key_long_press)) },
                            singleLine = true,
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.layout_editor_key_function_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            state.rows.forEach { it.keys.remove(key) }
                            selectedKey = null
                        },
                    ) {
                        Text(stringResource(R.string.layout_editor_key_delete))
                    }
                }
            }
        }

        if (errorText != null) {
            Text(
                text = errorText.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }

        dragState?.let { drag ->
            val rootOffset = rootCoords?.positionInWindow() ?: Offset.Zero
            val local = drag.positionInWindow - rootOffset
            Box(
                modifier = Modifier
                    .offset { IntOffset(local.x.toInt(), local.y.toInt()) }
                    .size(80.dp, 48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(10.dp),
                    )
                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = drag.key.label.ifBlank { drag.key.type.name },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private data class DraggingKeyState(
    val key: EditorKeyState,
    val sourceRowId: String?,
    val fromPalette: Boolean,
    val positionInWindow: Offset,
)

@Composable
private fun EditorKeyBox(
    key: EditorKeyState,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    selected: Boolean,
    onSelect: () -> Unit,
    onStartDrag: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDrop: () -> Unit,
    labelOverride: String? = null,
    onAddAfter: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
) {
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    Box(
        modifier = Modifier
            .size(width, height)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.surface,
            )
            .border(
                width = 1.dp,
                color =
                    if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = RoundedCornerShape(10.dp),
            )
            .onGloballyPositioned { coords = it }
            .pointerInput(key.uid) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val pos = coords?.positionInWindow()?.plus(offset) ?: Offset.Zero
                        onStartDrag(pos)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val pos = coords?.positionInWindow()?.plus(change.position) ?: return@detectDragGestures
                        onDrag(pos)
                    },
                    onDragEnd = {
                        onDrop()
                    },
                )
            }
            .clickable { onSelect() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = labelOverride ?: key.label.ifBlank { key.type.name },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (onAddAfter != null || onRemove != null) {
            Column(
                modifier = Modifier.align(Alignment.TopEnd).padding(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (onAddAfter != null) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .clickable { onAddAfter() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "+",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                if (onRemove != null) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                            .clickable { onRemove() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "-",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
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

private fun importThemeFromUri(context: android.content.Context, uri: Uri): ThemeImportResult? {
    val text =
        runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
        }.getOrNull().orEmpty()
    if (text.isBlank()) return null
    val spec = runCatching { ThemeParser.parseThemeSpec(text) }.getOrNull() ?: return null
    val themeId = spec.themeId.trim()
    if (themeId.isBlank()) return null
    val manager = ThemeManager(context)
    val dir = manager.getUserThemeDir()
    val safeName = sanitizeThemeFileName(themeId)
    File(dir, "$safeName.json").writeText(text, Charsets.UTF_8)
    return ThemeImportResult(themeId = themeId, name = spec.name)
}

private fun sanitizeThemeFileName(themeId: String): String {
    val normalized = themeId.trim().lowercase(Locale.ROOT)
    val sanitized = normalized.replace(Regex("[^a-z0-9._-]"), "_")
    return sanitized.ifBlank { "theme_${System.currentTimeMillis()}" }
}

private const val THEME_ID_BASIC = "custom_basic"
private const val THEME_ID_ADVANCED = "custom_advanced"

private data class ThemeColorToken(
    val key: String,
    val labelRes: Int,
    val fallbackColor: Int,
)

private data class KeyOverrideState(
    val backgroundColor: Int? = null,
    val textColor: Int? = null,
    val backgroundImage: String? = null,
)

private data class ThemeImageTarget(
    val themeId: String,
    val onImageSelected: (String?) -> Unit,
)

@OptIn(ExperimentalSerializationApi::class)
private val themeJson =
    Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = false
    }

private fun writeThemeSpec(context: android.content.Context, spec: ThemeSpec) {
    val dir = ThemeManager(context).getUserThemeDir()
    val safeName = sanitizeThemeFileName(spec.themeId)
    val file = File(dir, "$safeName.json")
    val text = themeJson.encodeToString(ThemeSpec.serializer(), spec)
    file.writeText(text, Charsets.UTF_8)
}

private fun copyThemeImageFromUri(context: android.content.Context, uri: Uri, themeId: String): String? {
    val name = getDisplayName(context, uri) ?: "theme_image_${System.currentTimeMillis()}.png"
    val ext =
        if (name.contains('.')) name.substringAfterLast('.', "")
        else "png"
    val base = name.substringBeforeLast('.', name)
    val safeName = sanitizeThemeFileName("${themeId}_${base}")
    val file = File(ThemeManager(context).getUserThemeDir(), "$safeName.$ext")
    val input = context.contentResolver.openInputStream(uri) ?: return null
    input.use { source ->
        FileOutputStream(file).use { out ->
            source.copyTo(out)
        }
    }
    return "file:${file.absolutePath}"
}

private fun getDisplayName(context: android.content.Context, uri: Uri): String? {
    return context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
    }
}

private fun resolveThemeColor(theme: ThemeSpec, value: String?, fallback: Int): Int {
    val runtime = ThemeRuntime(theme)
    return runtime.resolveColor(value, fallback)
}

private fun buildAdvancedThemeSpec(
    base: ThemeSpec,
    overrides: Map<String, KeyOverrideState>,
    layoutBgColor: Int?,
    layoutBgImage: String?,
    name: String,
): ThemeSpec {
    val keyOverrides =
        overrides.mapNotNull { (keyId, state) ->
            val bg =
                if (state.backgroundColor != null || !state.backgroundImage.isNullOrBlank()) {
                    BackgroundStyle(
                        color = state.backgroundColor?.let(::formatColorHex),
                        image = state.backgroundImage?.let { ImageSpec(assetPath = it) },
                    )
                } else {
                    null
                }
            val label = state.textColor?.let { TextStyle(color = formatColorHex(it)) }
            if (bg == null && label == null) return@mapNotNull null
            keyId to KeyStyle(background = bg, label = label)
        }.toMap()
    val layoutBackground =
        if (layoutBgColor != null || !layoutBgImage.isNullOrBlank()) {
            BackgroundStyle(
                color = layoutBgColor?.let(::formatColorHex),
                image = layoutBgImage?.let { ImageSpec(assetPath = it) },
            )
        } else {
            null
        }
    val layout = base.layout.copy(background = layoutBackground ?: base.layout.background)
    return base.copy(
        themeId = THEME_ID_ADVANCED,
        name = name,
        layout = layout,
        keyOverrides = keyOverrides,
    )
}

private fun formatColorHex(color: Int): String {
    val a = (color shr 24) and 0xFF
    val r = (color shr 16) and 0xFF
    val g = (color shr 8) and 0xFF
    val b = color and 0xFF
    return if (a == 0xFF) {
        String.format("#%02X%02X%02X", r, g, b)
    } else {
        String.format("#%02X%02X%02X%02X", a, r, g, b)
    }
}

private fun parseColorHex(raw: String): Int? {
    val cleaned = raw.trim().removePrefix("#")
    if (cleaned.length != 6 && cleaned.length != 8) return null
    val value = cleaned.toLongOrNull(16) ?: return null
    val hex = if (cleaned.length == 6) "FF$cleaned" else cleaned
    return runCatching { android.graphics.Color.parseColor("#$hex") }.getOrNull()
}

@Composable
private fun ThemePreviewCard(
    title: String,
    spec: ThemeSpec?,
    layout: xyz.xiao6.myboard.model.KeyboardLayout?,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            if (spec != null && layout != null) {
                KeyboardPreviewLayout(
                    layout = layout,
                    spec = spec,
                    selectedKeyId = null,
                    onSelectKey = null,
                )
            } else {
                Text(
                    text = stringResource(R.string.settings_theme_preview_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ThemeHeroCard(
    title: String,
    subtitle: String,
) {
    val gradient =
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.tertiary,
            ),
            start = Offset(0f, 0f),
            end = Offset(900f, 900f),
        )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(gradient)
                .padding(20.dp),
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@Composable
private fun ThemePanelToggle(
    selected: ThemePanel,
    onSelect: (ThemePanel) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        BoxWithConstraints(modifier = Modifier.padding(8.dp)) {
            val spacing = 8.dp
            val buttonWidth = ((maxWidth - spacing * 2) / 3f).coerceAtLeast(0.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                ThemePanelButton(
                    text = stringResource(R.string.settings_theme_tab_preview),
                    selected = selected == ThemePanel.PREVIEW,
                    width = buttonWidth,
                    onClick = { onSelect(ThemePanel.PREVIEW) },
                )
                ThemePanelButton(
                    text = stringResource(R.string.settings_theme_tab_basic),
                    selected = selected == ThemePanel.BASIC,
                    width = buttonWidth,
                    onClick = { onSelect(ThemePanel.BASIC) },
                )
                ThemePanelButton(
                    text = stringResource(R.string.settings_theme_tab_advanced),
                    selected = selected == ThemePanel.ADVANCED,
                    width = buttonWidth,
                    onClick = { onSelect(ThemePanel.ADVANCED) },
                )
            }
        }
    }
}

@Composable
private fun ThemePanelButton(
    text: String,
    selected: Boolean,
    width: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    val bg =
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val fg =
        if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .width(width)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = fg, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ThemeBlockCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun LanguageCard(
    title: String,
    subtitle: String,
    isDefault: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(end = 72.dp)) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (isDefault) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.settings_default),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnabledLanguagesRow(
    enabledTags: List<String>,
    preferredLayouts: Map<String, String?>,
    layoutManager: LayoutManager,
    onOpen: (String) -> Unit,
    onDisable: (String) -> Unit,
) {
    if (enabledTags.isEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.settings_language_layout_no_enabled),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(enabledTags, key = { it }) { tag ->
            val layoutId = preferredLayouts[tag]
            val layoutName =
                layoutId?.let {
                    runCatching { layoutManager.getLayout(it).name }.getOrNull()
                        ?.takeIf { n -> n.isNotBlank() }
                        ?: it
                }.orEmpty()
            Card(
                modifier = Modifier
                    .width(260.dp)
                    .clickable { onOpen(tag) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = formatLocaleLabel(tag), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (layoutName.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.settings_language_layout_enabled_layout, layoutName),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onOpen(tag) }) {
                            Text(stringResource(R.string.settings_language_layout_manage))
                        }
                        OutlinedButton(onClick = { onDisable(tag) }) {
                            Text(stringResource(R.string.settings_language_layout_disable))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LayoutThumbnailCard(
    title: String,
    subtitle: String,
    enabled: Boolean,
    preferred: Boolean,
    themeSpec: ThemeSpec?,
    layout: xyz.xiao6.myboard.model.KeyboardLayout?,
    onToggle: (Boolean) -> Unit,
    onSetDefault: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(end = 72.dp)) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (layout != null && themeSpec != null) {
                Box(modifier = Modifier.fillMaxWidth().heightIn(min = 110.dp)) {
                    KeyboardPreviewLayout(
                        layout = layout,
                        spec = themeSpec,
                        selectedKeyId = null,
                        onSelectKey = null,
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.settings_theme_preview_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (preferred) {
                    Text(
                        text = stringResource(R.string.settings_default),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    TextButton(onClick = onSetDefault) {
                        Text(stringResource(R.string.settings_language_layout_set_default))
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeBaseSelectorRow(
    label: String,
    themeOptions: List<ThemeOption>,
    currentThemeId: String,
    onClick: () -> Unit,
) {
    val currentName = themeOptions.firstOrNull { it.themeId == currentThemeId }?.name ?: currentThemeId
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = { Text(currentName) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
}

@Composable
private fun ThemeBasePickerDialog(
    themeOptions: List<ThemeOption>,
    currentThemeId: String,
    onDismiss: () -> Unit,
    onThemeSelected: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.settings_theme_base)) },
        text = {
            Column {
                themeOptions.forEach { option ->
                    val selected = option.themeId == currentThemeId
                    ListItem(
                        headlineContent = { Text(text = option.name) },
                        supportingContent = { Text(option.themeId) },
                        trailingContent = { if (selected) Text("OK") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onThemeSelected(option.themeId) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.common_close))
            }
        },
    )
}

@Composable
private fun ThemeImagePickerRow(
    label: String,
    path: String?,
    onPick: () -> Unit,
    onClear: () -> Unit,
    compact: Boolean = false,
) {
    val displayName =
        path?.removePrefix("file:")?.let { File(it).name }
            ?: stringResource(R.string.settings_theme_image_none)
    val horizontal = if (compact) 0.dp else 16.dp
    val vertical = if (compact) 0.dp else 8.dp
    Column(modifier = Modifier.padding(horizontal = horizontal, vertical = vertical)) {
        Text(text = label, style = MaterialTheme.typography.bodySmall)
        Text(
            text = displayName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onPick) {
                Text(stringResource(R.string.settings_theme_image_pick))
            }
            OutlinedButton(onClick = onClear, enabled = path != null) {
                Text(stringResource(R.string.settings_theme_image_clear))
            }
        }
    }
}

@Composable
private fun ColorPickerRow(
    label: String,
    color: Int,
    onColorChange: (Int) -> Unit,
    compact: Boolean = false,
) {
    var hexText by remember(color) { mutableStateOf(formatColorHex(color)) }
    val alpha = (color shr 24) and 0xFF
    val red = (color shr 16) and 0xFF
    val green = (color shr 8) and 0xFF
    val blue = color and 0xFF
    val previewShape = RoundedCornerShape(8.dp)
    val horizontal = if (compact) 0.dp else 16.dp
    val vertical = if (compact) 0.dp else 8.dp
    Column(modifier = Modifier.padding(horizontal = horizontal, vertical = vertical)) {
        Text(text = label, style = MaterialTheme.typography.bodySmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(previewShape)
                    .background(Color(color))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, previewShape),
            )
            Spacer(modifier = Modifier.width(12.dp))
            OutlinedTextField(
                value = hexText,
                onValueChange = {
                    hexText = it
                    parseColorHex(it)?.let(onColorChange)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.settings_theme_hex)) },
                singleLine = true,
            )
        }
        ColorChannelSlider(
            label = stringResource(R.string.settings_theme_alpha),
            value = alpha,
            onValueChange = { newAlpha ->
                onColorChange(
                    (newAlpha shl 24) or (red shl 16) or (green shl 8) or blue
                )
            },
        )
        ColorChannelSlider(
            label = stringResource(R.string.settings_theme_red),
            value = red,
            onValueChange = { newRed ->
                onColorChange(
                    (alpha shl 24) or (newRed shl 16) or (green shl 8) or blue
                )
            },
        )
        ColorChannelSlider(
            label = stringResource(R.string.settings_theme_green),
            value = green,
            onValueChange = { newGreen ->
                onColorChange(
                    (alpha shl 24) or (red shl 16) or (newGreen shl 8) or blue
                )
            },
        )
        ColorChannelSlider(
            label = stringResource(R.string.settings_theme_blue),
            value = blue,
            onValueChange = { newBlue ->
                onColorChange(
                    (alpha shl 24) or (red shl 16) or (green shl 8) or newBlue
                )
            },
        )
    }
}

@Composable
private fun ColorChannelSlider(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = "$label: $value", style = MaterialTheme.typography.bodySmall)
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 0f..255f,
        )
    }
}

@Composable
private fun KeyboardPreviewEditor(
    layout: xyz.xiao6.myboard.model.KeyboardLayout,
    spec: ThemeSpec?,
    selectedKeyId: String?,
    onSelectKey: (String) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.settings_theme_key_preview),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (spec != null) {
                KeyboardPreviewLayout(
                    layout = layout,
                    spec = spec,
                    selectedKeyId = selectedKeyId,
                    onSelectKey = onSelectKey,
                )
            } else {
                Text(
                    text = stringResource(R.string.settings_theme_preview_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun KeyboardPreviewLayout(
    layout: xyz.xiao6.myboard.model.KeyboardLayout,
    spec: ThemeSpec,
    selectedKeyId: String?,
    onSelectKey: ((String) -> Unit)?,
) {
    val runtime = remember(spec) { ThemeRuntime(spec) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        layout.rows.forEach { row ->
            val keys = row.keys.sortedBy { it.ui.gridPosition.startCol }
            val usesWeight = keys.any { it.ui.widthWeight != 1f }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                keys.forEach { key ->
                    val style = resolveKeyStyle(spec, key)
                    val bg = runtime.resolveColor(
                        style?.background?.color ?: "colors.key_bg",
                        android.graphics.Color.WHITE,
                    )
                    val fg = runtime.resolveColor(
                        style?.label?.color ?: "colors.key_text",
                        android.graphics.Color.BLACK,
                    )
                    val weight = if (usesWeight) key.ui.widthWeight else key.ui.gridPosition.spanCols.toFloat()
                    val shape = RoundedCornerShape(8.dp)
                    val borderColor =
                        if (selectedKeyId == key.keyId) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .height(36.dp)
                            .clip(shape)
                            .background(Color(bg))
                            .border(1.dp, borderColor, shape)
                            .clickable(enabled = onSelectKey != null) { onSelectKey?.invoke(key.keyId) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = (key.ui.label ?: key.label).orEmpty(),
                            color = Color(fg),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

private fun resolveKeyStyle(spec: ThemeSpec, key: xyz.xiao6.myboard.model.Key): KeyStyle? {
    val base = spec.keyStyles[key.ui.styleId]
    val override = spec.keyOverrides[key.keyId]
    if (override == null) return base
    return mergeKeyStyle(base, override)
}

private fun mergeKeyStyle(base: KeyStyle?, override: KeyStyle): KeyStyle {
    if (base == null) return override
    return base.copy(
        background = override.background ?: base.background,
        backgroundPressed = override.backgroundPressed ?: base.backgroundPressed,
        backgroundDisabled = override.backgroundDisabled ?: base.backgroundDisabled,
        stroke = override.stroke ?: base.stroke,
        strokePressed = override.strokePressed ?: base.strokePressed,
        shadow = override.shadow ?: base.shadow,
        padding = override.padding ?: base.padding,
        cornerRadiusDp = override.cornerRadiusDp ?: base.cornerRadiusDp,
        icon = override.icon ?: base.icon,
        label = override.label ?: base.label,
        hint = override.hint ?: base.hint,
        keyBackground = override.keyBackground ?: base.keyBackground,
        textColor = override.textColor ?: base.textColor,
        textSizeSp = override.textSizeSp ?: base.textSizeSp,
    )
}
