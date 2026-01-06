package xyz.xiao6.myboard.ui.settings

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.xiao6.myboard.R
import xyz.xiao6.myboard.dictionary.UserDictionaryStore
import xyz.xiao6.myboard.dictionary.format.DictionaryEntry
import xyz.xiao6.myboard.manager.DictionaryManager
import xyz.xiao6.myboard.manager.SubtypeManager
import xyz.xiao6.myboard.model.DictionarySpec
import xyz.xiao6.myboard.store.SettingsStore
import java.io.File
import java.util.Locale

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
fun DictionariesSettings(
    modifier: Modifier = Modifier,
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

private fun copyUriToTempFile(context: android.content.Context, uri: Uri): File {
    val temp = File.createTempFile("myboard_dict_", ".tmp", context.cacheDir)
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

private fun formatLocaleLabel(localeTag: String): String {
    val tag = localeTag.trim().replace('_', '-')
    val display = Locale.forLanguageTag(tag).getDisplayName(Locale.getDefault()).ifBlank { tag }
    return "$display ($tag)"
}
