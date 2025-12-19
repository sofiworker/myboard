package xyz.xiao6.myboard.ui

import android.content.Intent
import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.provider.Settings.Secure
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import xyz.xiao6.myboard.R
import xyz.xiao6.myboard.ime.MyBoardImeService
import xyz.xiao6.myboard.manager.LayoutManager
import xyz.xiao6.myboard.manager.SubtypeManager
import xyz.xiao6.myboard.model.LocaleLayoutProfile
import xyz.xiao6.myboard.store.MyBoardPrefs
import xyz.xiao6.myboard.util.KeyboardSizeConstraints
import java.util.Locale

/** 应用主入口：设置页（若未完成引导会自动跳转到 SetupActivity）。 */
class SettingsActivity : AppCompatActivity() {
    private lateinit var prefs: MyBoardPrefs
    private lateinit var subtypeManager: SubtypeManager
    private lateinit var layoutManager: LayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = MyBoardPrefs(this)
        subtypeManager = SubtypeManager(this).loadAll()
        layoutManager = LayoutManager(this).loadAllFromAssets()

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
    prefs: MyBoardPrefs,
    subtypeManager: SubtypeManager,
    layoutManager: LayoutManager,
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

            SettingsRoute.Dictionaries -> PlaceholderSettingsPage(
                modifier = Modifier.fillMaxSize().padding(padding),
                textRes = R.string.settings_placeholder_dictionaries,
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
    prefs: MyBoardPrefs,
    imeEnabled: Boolean,
    imeSelected: Boolean,
    onOpenImeSettings: () -> Unit,
    onShowImePicker: () -> Unit,
    onOpenLanguageLayout: () -> Unit,
    onOpenFeedback: () -> Unit,
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
private fun KeyboardSizeSettings(
    modifier: Modifier,
    prefs: MyBoardPrefs,
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
    prefs: MyBoardPrefs,
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
    prefs: MyBoardPrefs,
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

private fun formatLocaleLabel(localeTag: String): String {
    val tag = localeTag.trim().replace('_', '-')
    val display = Locale.forLanguageTag(tag).getDisplayName(Locale.getDefault()).ifBlank { tag }
    return "$display ($tag)"
}
