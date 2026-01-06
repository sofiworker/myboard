package xyz.xiao6.myboard.ui.settings

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.xiao6.myboard.R
import xyz.xiao6.myboard.manager.LayoutManager
import xyz.xiao6.myboard.manager.SubtypeManager
import xyz.xiao6.myboard.manager.ThemeManager
import xyz.xiao6.myboard.model.KeyboardLayout
import xyz.xiao6.myboard.model.LayoutParser
import xyz.xiao6.myboard.model.LocaleLayoutProfile
import xyz.xiao6.myboard.model.ThemeSpec
import xyz.xiao6.myboard.model.validate
import xyz.xiao6.myboard.store.SettingsStore
import java.io.File
import java.util.Locale

@Composable
fun LanguageLayoutSettings(
    modifier: Modifier = Modifier,
    prefs: SettingsStore,
    subtypeManager: SubtypeManager,
    layoutManager: LayoutManager,
    detailTag: String?,
    onDetailTagChange: (String?) -> Unit,
    showCreateLayoutDialog: Boolean,
    onShowCreateLayoutDialogChange: (Boolean) -> Unit,
    showImportLayout: Boolean,
    onShowImportLayoutChange: (Boolean) -> Unit,
    onOpenLayoutEditor: (String) -> Unit,
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

// Helper functions

private fun formatLocaleLabel(localeTag: String): String {
    val tag = localeTag.trim().replace('_', '-')
    val display = Locale.forLanguageTag(tag).getDisplayName(Locale.getDefault()).ifBlank { tag }
    return "$display ($tag)"
}

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

private fun sanitizeLayoutIdFromName(name: String): String {
    val normalized = name.trim().lowercase(Locale.ROOT)
    return normalized.replace(Regex("[^a-z0-9]+"), "_").trim('_')
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
    val json = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = false
    }
    val text = json.encodeToString(KeyboardLayout.serializer(), layout)
    file.writeText(text, Charsets.UTF_8)
}

// Composable components

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
                    TextButton(
                        onClick = onSetDefault,
                        enabled = enabled,
                    ) {
                        Text(stringResource(R.string.settings_language_layout_set_default))
                    }
                }
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
    val runtime = remember(spec) { xyz.xiao6.myboard.ui.theme.ThemeRuntime(spec) }
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
                    val shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
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

private fun resolveKeyStyle(spec: ThemeSpec, key: xyz.xiao6.myboard.model.Key): xyz.xiao6.myboard.model.KeyStyle? {
    val base = spec.keyStyles[key.ui.styleId]
    val override = spec.keyOverrides[key.keyId]
    if (override == null) return base
    return mergeKeyStyle(base, override)
}

private fun mergeKeyStyle(base: xyz.xiao6.myboard.model.KeyStyle?, override: xyz.xiao6.myboard.model.KeyStyle): xyz.xiao6.myboard.model.KeyStyle {
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
