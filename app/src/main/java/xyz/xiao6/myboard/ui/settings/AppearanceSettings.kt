package xyz.xiao6.myboard.ui.settings

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import xyz.xiao6.myboard.R
import xyz.xiao6.myboard.manager.LayoutManager
import xyz.xiao6.myboard.manager.ThemeManager
import xyz.xiao6.myboard.model.BackgroundStyle
import xyz.xiao6.myboard.model.ImageSpec
import xyz.xiao6.myboard.model.Key
import xyz.xiao6.myboard.model.KeyAction
import xyz.xiao6.myboard.model.KeyActionCase
import xyz.xiao6.myboard.model.KeyStyle
import xyz.xiao6.myboard.model.KeyboardLayout
import xyz.xiao6.myboard.model.TextStyle
import xyz.xiao6.myboard.model.ThemeParser
import xyz.xiao6.myboard.model.ThemeSpec
import xyz.xiao6.myboard.store.SettingsStore
import xyz.xiao6.myboard.ui.theme.ThemeRuntime
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

private enum class ThemePanel {
    PREVIEW,
    BASIC,
    ADVANCED,
}

private const val THEME_ID_BASIC = "custom_basic"
private const val THEME_ID_ADVANCED = "custom_advanced"

@OptIn(ExperimentalSerializationApi::class)
private val themeJson =
    Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = false
    }

@Composable
fun AppearanceSettings(
    modifier: Modifier = Modifier,
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
private fun SectionHeader(textRes: Int) {
    Text(
        text = stringResource(textRes),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
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

private data class ThemeOption(
    val themeId: String,
    val name: String,
)

private data class ThemeImportResult(
    val themeId: String,
    val name: String?,
)

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

private fun importThemeFromUri(context: Context, uri: Uri): ThemeImportResult? {
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

private fun writeThemeSpec(context: Context, spec: ThemeSpec) {
    val dir = ThemeManager(context).getUserThemeDir()
    val safeName = sanitizeThemeFileName(spec.themeId)
    val file = File(dir, "$safeName.json")
    val text = themeJson.encodeToString(ThemeSpec.serializer(), spec)
    file.writeText(text, Charsets.UTF_8)
}

private fun copyThemeImageFromUri(context: Context, uri: Uri, themeId: String): String? {
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

private fun getDisplayName(context: Context, uri: Uri): String? {
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
