package xyz.xiao6.myboard.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import xyz.xiao6.myboard.R
import xyz.xiao6.myboard.contract.state.BuiltInSchemas
import xyz.xiao6.myboard.contract.state.LocaleTag
import xyz.xiao6.myboard.contract.state.Schema
import xyz.xiao6.myboard.data.db.SettingsDatabase
import xyz.xiao6.myboard.data.repository.SettingsRepository
import xyz.xiao6.myboard.state.BuiltInManifests

/**
 * 语言与输入法设置页面。
 * 参考 GBoard 设计，三屏切换：
 * - 状态 A：已启用语言列表
 * - 状态 B：添加新语言
 * - 状态 C：选择输入方案
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSettingsScreen(
    onBack: () -> Unit,
    viewModel: LanguageSettingsViewModel = viewModel(
        factory = LanguageSettingsViewModel.Factory(
            SettingsRepository(
                SettingsDatabase.getInstance(LocalContext.current).settingsDao()
            )
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    // 所有可用语言（从 BuiltInManifests 获取）
    val allManifests = remember { BuiltInManifests.all }

    when {
        // ===== 状态 C：方案选择 =====
        uiState.editingLocale != null -> {
            SchemaSelectionContent(
                locale = uiState.editingLocale!!,
                selectedSchemas = uiState.editingSchemas ?: emptyList(),
                allManifests = allManifests,
                onToggleSchema = { viewModel.toggleSchema(it) },
                onDone = { viewModel.confirmEditSchemas() },
                onBack = { viewModel.cancelEditSchemas() }
            )
        }
        // ===== 状态 B：添加新语言 =====
        uiState.isAddingLanguage -> {
            AddLanguageContent(
                existingLocales = uiState.localeConfigs.keys,
                allManifests = allManifests,
                onSelectLocale = { locale, defaultSchema ->
                    viewModel.selectNewLanguage(locale, defaultSchema)
                },
                onBack = { viewModel.cancelAddLanguage() }
            )
        }
        // ===== 状态 A：语言列表（主视图） =====
        else -> {
            LanguageListContent(
                localeConfigs = uiState.localeConfigs,
                currentLocale = uiState.currentLocale,
                isEditing = uiState.isEditing,
                allManifests = allManifests,
                onToggleEditMode = { viewModel.toggleEditMode() },
                onRemoveLocale = { viewModel.removeLocale(it) },
                onSelectLocale = { viewModel.setCurrentLocale(it) },
                onClickLocale = { viewModel.startEditSchemas(it) },
                onAddLanguage = { viewModel.startAddLanguage() },
                onBack = onBack
            )
        }
    }
}

// ===== 状态 A：语言列表 =====
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageListContent(
    localeConfigs: Map<LocaleTag, List<Schema>>,
    currentLocale: LocaleTag,
    isEditing: Boolean,
    allManifests: List<xyz.xiao6.myboard.contract.manifest.LanguageManifest>,
    onToggleEditMode: () -> Unit,
    onRemoveLocale: (LocaleTag) -> Unit,
    onSelectLocale: (LocaleTag) -> Unit,
    onClickLocale: (LocaleTag) -> Unit,
    onAddLanguage: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("语言和输入法") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (localeConfigs.size > 1) {
                        TextButton(onClick = onToggleEditMode) {
                            Text(if (isEditing) "完成" else "编辑")
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 已启用语言列表
            items(
                localeConfigs.entries.toList(),
                key = { it.key.value }
            ) { (locale, schemas) ->
                val manifest = allManifests.find { it.locale == locale }
                val displayName = manifest?.displayName?.get("zh-CN")
                    ?: manifest?.displayName?.get("en-US")
                    ?: locale.value
                val isCurrent = locale == currentLocale
                val schemaNames = schemas.joinToString("、") { schema ->
                    when (schema) {
                        BuiltInSchemas.PINYIN -> "拼音"
                        BuiltInSchemas.SHUANGPIN_ZIRAN -> "双拼"
                        BuiltInSchemas.T9_PINYIN -> "T9"
                        BuiltInSchemas.DOUBLE_PINYIN -> "双拼"
                        BuiltInSchemas.LATIN_DIRECT -> "QWERTY"
                        BuiltInSchemas.ROMAJI -> "假名"
                        else -> schema.value
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isEditing) return@clickable
                                onClickLocale(locale)
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isEditing && localeConfigs.size > 1) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "移除",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { onRemoveLocale(locale) }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        } else if (!isEditing) {
                            // 语言图标/标识
                            Text(
                                text = displayName.take(1),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(displayName, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                schemaNames.ifEmpty { "无输入方案" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (!isEditing) {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 添加语言按钮
            if (!isEditing) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    FilledTonalButton(
                        onClick = onAddLanguage,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("添加语言")
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

// ===== 状态 B：添加语言 =====
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddLanguageContent(
    existingLocales: Set<LocaleTag>,
    allManifests: List<xyz.xiao6.myboard.contract.manifest.LanguageManifest>,
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
                names.any { it.contains(searchText, ignoreCase = true) }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("添加语言") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // 搜索栏
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text("搜索语言") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 语言列表
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(availableManifests) { manifest ->
                    val displayName = manifest.displayName["zh-CN"]
                        ?: manifest.displayName["en-US"]
                        ?: manifest.locale.value
                    val defaultSchema = manifest.defaults.schema

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onSelectLocale(manifest.locale, defaultSchema) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = displayName.take(1),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(displayName, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}

// ===== 状态 C：方案选择 =====
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SchemaSelectionContent(
    locale: LocaleTag,
    selectedSchemas: List<Schema>,
    allManifests: List<xyz.xiao6.myboard.contract.manifest.LanguageManifest>,
    onToggleSchema: (Schema) -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val manifest = allManifests.find { it.locale == locale }
    val displayName = manifest?.displayName?.get("zh-CN")
        ?: manifest?.displayName?.get("en-US")
        ?: locale.value

    // 获取该语言所有可用 schema
    val availableSchemas = remember(manifest) {
        manifest?.scripts?.values?.flatMap { it.schemas.keys }?.distinct() ?: emptyList()
    }

    // schema 显示名映射
    fun schemaDisplayName(schema: Schema): String = when (schema) {
        BuiltInSchemas.PINYIN -> "拼音"
        BuiltInSchemas.SHUANGPIN_ZIRAN -> "双拼"
        BuiltInSchemas.T9_PINYIN -> "T9 拼音"
        BuiltInSchemas.DOUBLE_PINYIN -> "自然码双拼"
        BuiltInSchemas.LATIN_DIRECT -> "QWERTY"
        BuiltInSchemas.ROMAJI -> "假名"
        else -> schema.value
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(displayName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        bottomBar = {
            // 底部完成按钮
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = { onDone() },
                        enabled = selectedSchemas.isNotEmpty()
                    ) {
                        Text("完成")
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(availableSchemas) { schema ->
                val isSelected = schema in selectedSchemas
                val name = schemaDisplayName(schema)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onToggleSchema(schema) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleSchema(schema) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}
