package xyz.xiao6.myboard.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xyz.xiao6.myboard.R
import xyz.xiao6.myboard.contract.manifest.LanguagePackManifest
import xyz.xiao6.myboard.contract.state.BuiltInSchemas
import xyz.xiao6.myboard.contract.state.LocaleTag
import xyz.xiao6.myboard.contract.state.Schema
import xyz.xiao6.myboard.pack.BuiltInLanguagePacks
import java.util.Locale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import xyz.xiao6.myboard.pack.InstalledLanguagePack

/**
 * 语言与输入法设置页面。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSettingsScreen(
    onBack: () -> Unit,
    viewModel: LanguageSettingsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val allManifests = remember { BuiltInLanguagePacks.all }
    val uiLocaleTag = rememberUiLocaleTag()
    var pendingUninstall by remember { mutableStateOf<String?>(null) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importPackage)
    }

    pendingUninstall?.let { packageId ->
        AlertDialog(
            onDismissRequest = { pendingUninstall = null },
            title = { Text(stringResource(R.string.settings_language_pack_uninstall_title)) },
            text = { Text(stringResource(R.string.settings_language_pack_uninstall_message)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingUninstall = null
                    viewModel.uninstallPackage(packageId)
                }) { Text(stringResource(R.string.settings_language_pack_uninstall)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingUninstall = null }) {
                    Text(stringResource(R.string.common_back))
                }
            }
        )
    }

    when {
        uiState.editingLocale != null -> {
            SchemaSelectionContent(
                locale = uiState.editingLocale!!,
                selectedSchemas = uiState.editingSchemas ?: emptyList(),
                allManifests = allManifests,
                uiLocaleTag = uiLocaleTag,
                onToggleSchema = { viewModel.toggleSchema(it) },
                onDone = { viewModel.confirmEditSchemas() },
                onBack = { viewModel.cancelEditSchemas() }
            )
        }
        uiState.isAddingLanguage -> {
            AddLanguageContent(
                existingLocales = uiState.localeConfigs.keys,
                allManifests = allManifests,
                uiLocaleTag = uiLocaleTag,
                onSelectLocale = { locale, defaultSchema ->
                    viewModel.selectNewLanguage(locale, defaultSchema)
                },
                onBack = { viewModel.cancelAddLanguage() }
            )
        }
        else -> {
            LanguageListContent(
                localeConfigs = uiState.localeConfigs,
                currentLocale = uiState.currentLocale,
                isEditing = uiState.isEditing,
                allManifests = allManifests,
                uiLocaleTag = uiLocaleTag,
                onToggleEditMode = { viewModel.toggleEditMode() },
                onRemoveLocale = { viewModel.removeLocale(it) },
                onSelectLocale = { viewModel.setCurrentLocale(it) },
                onClickLocale = { viewModel.startEditSchemas(it) },
                onAddLanguage = { viewModel.startAddLanguage() },
                installedPackages = uiState.installedPackages,
                packageOperationInProgress = uiState.packageOperationInProgress,
                packageMessage = uiState.packageMessage,
                onImportPackage = {
                    importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "application/x-zip-compressed"))
                },
                onSetPackageEnabled = viewModel::setPackageEnabled,
                onUninstallPackage = { pendingUninstall = it },
                onBack = onBack
            )
        }
    }
}

@Composable
private fun rememberUiLocaleTag(): String {
    val configuration = LocalConfiguration.current
    return remember(configuration) {
        val locale = configuration.locales[0] ?: Locale.getDefault()
        val language = locale.language
        val country = locale.country
        if (country.isNullOrBlank()) language else "$language-$country"
    }
}

private fun resolveDisplayName(
    manifest: LanguagePackManifest?,
    uiLocaleTag: String,
    fallback: String
): String {
    if (manifest == null) return fallback
    val names = manifest.displayName
    val languageOnly = uiLocaleTag.substringBefore('-')
    return names[uiLocaleTag]
        ?: names.entries.firstOrNull { it.key.startsWith(languageOnly) }?.value
        ?: names["en-US"]
        ?: names.values.firstOrNull()
        ?: fallback
}

@Composable
private fun schemaDisplayName(schema: Schema): String = when (schema) {
    BuiltInSchemas.PINYIN -> stringResource(R.string.schema_pinyin)
    BuiltInSchemas.SHUANGPIN_ZIRAN -> stringResource(R.string.schema_shuangpin)
    BuiltInSchemas.T9_PINYIN -> stringResource(R.string.schema_t9_pinyin)
    BuiltInSchemas.DOUBLE_PINYIN -> stringResource(R.string.schema_double_pinyin)
    BuiltInSchemas.LATIN_DIRECT -> stringResource(R.string.schema_latin_direct)
    BuiltInSchemas.ROMAJI -> stringResource(R.string.schema_romaji)
    BuiltInSchemas.VOICE -> stringResource(R.string.schema_voice)
    BuiltInSchemas.HANDWRITING -> stringResource(R.string.schema_handwriting)
    else -> schema.value
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageListContent(
    localeConfigs: Map<LocaleTag, List<Schema>>,
    currentLocale: LocaleTag,
    isEditing: Boolean,
    allManifests: List<LanguagePackManifest>,
    uiLocaleTag: String,
    onToggleEditMode: () -> Unit,
    onRemoveLocale: (LocaleTag) -> Unit,
    onSelectLocale: (LocaleTag) -> Unit,
    onClickLocale: (LocaleTag) -> Unit,
    onAddLanguage: () -> Unit,
    installedPackages: List<InstalledLanguagePack>,
    packageOperationInProgress: Boolean,
    packageMessage: String?,
    onImportPackage: () -> Unit,
    onSetPackageEnabled: (String, Boolean) -> Unit,
    onUninstallPackage: (String) -> Unit,
    onBack: () -> Unit
) {
    SettingsScaffold(
        title = stringResource(R.string.settings_language_and_input),
        onBack = onBack,
        actions = {
            if (localeConfigs.size > 1) {
                TextButton(onClick = onToggleEditMode) {
                    Text(
                        text = if (isEditing) {
                            stringResource(R.string.common_done)
                        } else {
                            stringResource(R.string.common_edit)
                        }
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                SettingsSectionHeader(stringResource(R.string.settings_language_layout_enabled_title))
            }
            item {
                SettingsGroup {
                    localeConfigs.entries.toList().forEachIndexed { index, (locale, schemas) ->
                        val manifest = allManifests.find { it.locale == locale }
                        val displayName = resolveDisplayName(manifest, uiLocaleTag, locale.value)
                        val isCurrent = locale == currentLocale
                        val emptySchemaText = stringResource(R.string.settings_language_no_schema)
                        val schemaNameList = schemas.map { schemaDisplayName(it) }
                        val schemaNames = if (schemaNameList.isEmpty()) {
                            emptySchemaText
                        } else {
                            schemaNameList.joinToString(" · ")
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isEditing) return@clickable
                                    onClickLocale(locale)
                                }
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isEditing && localeConfigs.size > 1) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.common_remove),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clickable { onRemoveLocale(locale) }
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            } else {
                                SettingsAvatar(
                                    text = displayName,
                                    selected = isCurrent
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = displayName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    if (isCurrent && !isEditing) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.settings_language_current_badge),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = schemaNames,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (!isEditing) {
                                if (!isCurrent) {
                                    TextButton(onClick = { onSelectLocale(locale) }) {
                                        Text(stringResource(R.string.settings_language_set_default))
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (index < localeConfigs.size - 1) {
                            SettingsGroupDivider()
                        }
                    }
                }
            }

            if (!isEditing) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    FilledTonalButton(
                        onClick = onAddLanguage,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_language_add))
                    }
                }
            }

            if (!isEditing) {
                item {
                    SettingsSectionHeader(stringResource(R.string.settings_language_packs_title))
                }
                item {
                    FilledTonalButton(
                        onClick = onImportPackage,
                        enabled = !packageOperationInProgress,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_language_pack_import))
                    }
                }
                packageMessage?.let { message ->
                    item {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
                if (installedPackages.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.settings_language_pack_none),
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    item {
                        SettingsGroup {
                            installedPackages.forEachIndexed { index, pack ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            pack.displayName[uiLocaleTag]
                                                ?: pack.displayName["en-US"]
                                                ?: pack.identity.packageId
                                        )
                                        Text(
                                            stringResource(R.string.settings_language_pack_version, pack.identity.version.toString()),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        TextButton(onClick = { onUninstallPackage(pack.identity.packageId) }) {
                                            Text(stringResource(R.string.settings_language_pack_uninstall))
                                        }
                                    }
                                    Switch(
                                        checked = pack.enabled,
                                        enabled = !packageOperationInProgress,
                                        onCheckedChange = { onSetPackageEnabled(pack.identity.packageId, it) }
                                    )
                                }
                                if (index < installedPackages.lastIndex) SettingsGroupDivider()
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddLanguageContent(
    existingLocales: Set<LocaleTag>,
    allManifests: List<LanguagePackManifest>,
    uiLocaleTag: String,
    onSelectLocale: (LocaleTag, Schema) -> Unit,
    onBack: () -> Unit
) {
    var searchText by remember { mutableStateOf("") }

    val availableManifests = remember(searchText, existingLocales) {
        allManifests
            .filter { it.locale !in existingLocales }
            .filter { manifest ->
                if (searchText.isBlank()) return@filter true
                val names = manifest.displayName.values
                names.any { it.contains(searchText, ignoreCase = true) } ||
                    manifest.locale.value.contains(searchText, ignoreCase = true)
            }
    }

    SettingsScaffold(
        title = stringResource(R.string.settings_language_add),
        onBack = onBack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text(stringResource(R.string.settings_language_search)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            SettingsGroup {
                availableManifests.forEachIndexed { index, manifest ->
                    val displayName = resolveDisplayName(manifest, uiLocaleTag, manifest.locale.value)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectLocale(manifest.locale, manifest.defaults.schema)
                            }
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SettingsAvatar(text = displayName)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(displayName, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = manifest.locale.value,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (index < availableManifests.lastIndex) {
                        SettingsGroupDivider()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SchemaSelectionContent(
    locale: LocaleTag,
    selectedSchemas: List<Schema>,
    allManifests: List<LanguagePackManifest>,
    uiLocaleTag: String,
    onToggleSchema: (Schema) -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val manifest = allManifests.find { it.locale == locale }
    val displayName = resolveDisplayName(manifest, uiLocaleTag, locale.value)
    val availableSchemas = remember(manifest) {
        manifest?.capabilities?.map { it.id.schema }?.distinct() ?: emptyList()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(tonalElevation = 2.dp, color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDone,
                        enabled = selectedSchemas.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.common_done))
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            item {
                SettingsSectionHeader(stringResource(R.string.settings_language_schemas_title))
            }
            item {
                SettingsGroup {
                    availableSchemas.forEachIndexed { index, schema ->
                        val isSelected = schema in selectedSchemas
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleSchema(schema) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { onToggleSchema(schema) }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = schemaDisplayName(schema),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        if (index < availableSchemas.lastIndex) {
                            SettingsGroupDivider()
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}
