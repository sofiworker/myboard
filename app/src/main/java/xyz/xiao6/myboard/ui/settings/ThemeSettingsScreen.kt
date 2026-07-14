package xyz.xiao6.myboard.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import xyz.xiao6.myboard.R
import xyz.xiao6.myboard.data.db.SettingsDatabase
import xyz.xiao6.myboard.data.repository.SettingsRepository
import xyz.xiao6.myboard.theme.foundation.AppearanceMode
import xyz.xiao6.myboard.theme.foundation.CornerStyle
import xyz.xiao6.myboard.theme.foundation.FoundationPalette
import xyz.xiao6.myboard.theme.foundation.FoundationPaletteId
import xyz.xiao6.myboard.theme.foundation.KeyContrast
import xyz.xiao6.myboard.theme.foundation.KeyTreatment
import xyz.xiao6.myboard.theme.foundation.PaletteSource
import xyz.xiao6.myboard.theme.foundation.ThemeSeedInput
import xyz.xiao6.myboard.theme.skin.BuiltInSkinCatalog
import xyz.xiao6.myboard.theme.skin.SkinColorPolicy
import xyz.xiao6.myboard.theme.skin.SkinThemeId

@Composable
fun ThemeSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(
            SettingsRepository(
                SettingsDatabase.getInstance(LocalContext.current).settingsDao()
            )
        )
    )
) {
    val appearance by viewModel.appearanceSettings.collectAsState()
    val foundation = appearance.foundation
    val defaultCustomSeed = FoundationPalette.byId(FoundationPaletteId.GBOARD_BLUE).seedColor
    var customSeedInput by rememberSaveable(foundation.customSeedColor) {
        mutableStateOf(foundation.customSeedColor ?: defaultCustomSeed)
    }

    SettingsScaffold(
        title = stringResource(R.string.settings_theme),
        onBack = onBack
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            item { SettingsSectionHeader(stringResource(R.string.settings_theme_mode)) }
            item {
                SettingsGroup {
                    FoundationChoiceItem(
                        label = stringResource(R.string.settings_theme_auto),
                        description = stringResource(R.string.settings_theme_auto_desc),
                        selected = foundation.appearanceMode == AppearanceMode.FOLLOW_SYSTEM,
                        onClick = {
                            viewModel.updateFoundationTheme {
                                it.copy(appearanceMode = AppearanceMode.FOLLOW_SYSTEM)
                            }
                        },
                        showDivider = true
                    )
                    FoundationChoiceItem(
                        label = stringResource(R.string.settings_theme_light),
                        description = stringResource(R.string.settings_theme_light_desc),
                        selected = foundation.appearanceMode == AppearanceMode.LIGHT,
                        onClick = {
                            viewModel.updateFoundationTheme {
                                it.copy(appearanceMode = AppearanceMode.LIGHT)
                            }
                        },
                        showDivider = true
                    )
                    FoundationChoiceItem(
                        label = stringResource(R.string.settings_theme_dark),
                        description = stringResource(R.string.settings_theme_dark_desc),
                        selected = foundation.appearanceMode == AppearanceMode.DARK,
                        onClick = {
                            viewModel.updateFoundationTheme {
                                it.copy(appearanceMode = AppearanceMode.DARK)
                            }
                        }
                    )
                }
            }

            item { SettingsSectionHeader(stringResource(R.string.settings_theme_skin)) }
            item {
                SettingsGroup {
                    FoundationChoiceItem(
                        label = stringResource(R.string.settings_theme_skin_none),
                        description = stringResource(R.string.settings_theme_skin_none_desc),
                        selected = appearance.skinThemeId == null,
                        onClick = { viewModel.updateSkinThemeId(null) },
                        showDivider = true
                    )
                    BuiltInSkinCatalog.all.forEachIndexed { index, meta ->
                        FoundationChoiceItem(
                            label = skinLabel(meta.id),
                            description = skinDescription(meta.id),
                            selected = appearance.skinThemeId == meta.id.id,
                            onClick = { viewModel.updateSkinThemeId(meta.id.id) },
                            showDivider = index != BuiltInSkinCatalog.all.lastIndex
                        )
                    }
                }
            }

            val lockedSkinActive = BuiltInSkinCatalog.metaOf(appearance.skinThemeId)
                ?.colorPolicy == SkinColorPolicy.LOCKED
            if (lockedSkinActive) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.settings_theme_skin_locked_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                }
            }

            item { SettingsSectionHeader(stringResource(R.string.settings_theme_palette)) }
            item {
                SettingsGroup {
                    FoundationPalette.all.forEach { palette ->
                        FoundationPaletteItem(
                            label = paletteLabel(palette.id),
                            seedColor = palette.seedColor,
                            selected = foundation.paletteSource == PaletteSource.PRESET &&
                                foundation.paletteId == palette.id,
                            onClick = {
                                viewModel.updateFoundationTheme {
                                    it.copy(
                                        paletteSource = PaletteSource.PRESET,
                                        paletteId = palette.id
                                    )
                                }
                            },
                            showDivider = true
                        )
                    }
                    FoundationPaletteItem(
                        label = stringResource(R.string.settings_theme_palette_system_dynamic),
                        description = stringResource(R.string.settings_theme_palette_system_dynamic_desc),
                        seedColor = FoundationPalette.byId(FoundationPaletteId.GBOARD_BLUE).seedColor,
                        selected = foundation.paletteSource == PaletteSource.SYSTEM_DYNAMIC,
                        onClick = {
                            viewModel.updateFoundationTheme {
                                it.copy(paletteSource = PaletteSource.SYSTEM_DYNAMIC)
                            }
                        },
                        showDivider = true
                    )
                    FoundationPaletteItem(
                        label = stringResource(R.string.settings_theme_palette_custom),
                        description = stringResource(R.string.settings_theme_palette_custom_desc),
                        seedColor = foundation.customSeedColor
                            ?: defaultCustomSeed,
                        selected = foundation.paletteSource == PaletteSource.CUSTOM_SEED,
                        onClick = {
                            val selectedSeed = ThemeSeedInput.normalizeOrNull(customSeedInput)
                                ?: foundation.customSeedColor
                                ?: defaultCustomSeed
                            customSeedInput = selectedSeed
                            viewModel.updateFoundationTheme {
                                it.copy(
                                    paletteSource = PaletteSource.CUSTOM_SEED,
                                    customSeedColor = selectedSeed
                                )
                            }
                        }
                    )
                    if (foundation.paletteSource == PaletteSource.CUSTOM_SEED) {
                        CustomSeedInputItem(
                            value = customSeedInput,
                            onValueChange = { input ->
                                customSeedInput = input
                                val normalized = ThemeSeedInput.normalizeOrNull(input)
                                if (normalized != null && normalized != foundation.customSeedColor) {
                                    viewModel.updateFoundationTheme {
                                        it.copy(
                                            paletteSource = PaletteSource.CUSTOM_SEED,
                                            customSeedColor = normalized
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }

            item { SettingsSectionHeader(stringResource(R.string.settings_theme_key_treatment)) }
            item {
                SettingsGroup {
                    FoundationChoiceItem(
                        label = stringResource(R.string.settings_theme_key_treatment_filled),
                        selected = foundation.keyTreatment == KeyTreatment.FILLED,
                        onClick = {
                            viewModel.updateFoundationTheme { it.copy(keyTreatment = KeyTreatment.FILLED) }
                        },
                        showDivider = true
                    )
                    FoundationChoiceItem(
                        label = stringResource(R.string.settings_theme_key_treatment_outlined),
                        selected = foundation.keyTreatment == KeyTreatment.OUTLINED,
                        onClick = {
                            viewModel.updateFoundationTheme { it.copy(keyTreatment = KeyTreatment.OUTLINED) }
                        },
                        showDivider = true
                    )
                    FoundationChoiceItem(
                        label = stringResource(R.string.settings_theme_key_treatment_borderless),
                        selected = foundation.keyTreatment == KeyTreatment.BORDERLESS,
                        onClick = {
                            viewModel.updateFoundationTheme { it.copy(keyTreatment = KeyTreatment.BORDERLESS) }
                        }
                    )
                }
            }

            item { SettingsSectionHeader(stringResource(R.string.settings_theme_corner_style)) }
            item {
                SettingsGroup {
                    FoundationChoiceItem(
                        label = stringResource(R.string.settings_theme_corner_compact),
                        selected = foundation.cornerStyle == CornerStyle.COMPACT,
                        onClick = {
                            viewModel.updateFoundationTheme { it.copy(cornerStyle = CornerStyle.COMPACT) }
                        },
                        showDivider = true
                    )
                    FoundationChoiceItem(
                        label = stringResource(R.string.settings_theme_corner_rounded),
                        selected = foundation.cornerStyle == CornerStyle.ROUNDED,
                        onClick = {
                            viewModel.updateFoundationTheme { it.copy(cornerStyle = CornerStyle.ROUNDED) }
                        },
                        showDivider = true
                    )
                    FoundationChoiceItem(
                        label = stringResource(R.string.settings_theme_corner_pill),
                        selected = foundation.cornerStyle == CornerStyle.PILL,
                        onClick = {
                            viewModel.updateFoundationTheme { it.copy(cornerStyle = CornerStyle.PILL) }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                SettingsGroup {
                    SettingsSwitchItem(
                        title = stringResource(R.string.settings_theme_key_contrast),
                        checked = foundation.keyContrast == KeyContrast.HIGH,
                        onCheckedChange = { checked ->
                            viewModel.updateFoundationTheme {
                                it.copy(keyContrast = if (checked) KeyContrast.HIGH else KeyContrast.NORMAL)
                            }
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun CustomSeedInputItem(
    value: String,
    onValueChange: (String) -> Unit
) {
    val normalized = ThemeSeedInput.normalizeOrNull(value)
    val isError = value.isNotBlank() && normalized == null

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        label = { Text(stringResource(R.string.settings_theme_custom_seed_label)) },
        singleLine = true,
        isError = isError,
        supportingText = {
            if (isError) {
                Text(stringResource(R.string.settings_theme_custom_seed_error))
            }
        }
    )
}

@Composable
private fun FoundationPaletteItem(
    label: String,
    seedColor: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    showDivider: Boolean = false
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ThemeSeedSwatch(seedColor = seedColor)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
                if (!description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        if (showDivider) SettingsGroupDivider()
    }
}

@Composable
private fun FoundationChoiceItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    showDivider: Boolean = false
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Spacer(modifier = Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                if (!description.isNullOrBlank()) {
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (showDivider) SettingsGroupDivider()
    }
}

@Composable
private fun ThemeSeedSwatch(seedColor: String) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(26.dp)
                .clip(CircleShape)
                .background(parseHex(seedColor))
        )
    }
}

@Composable
private fun paletteLabel(id: FoundationPaletteId): String {
    return when (id) {
        FoundationPaletteId.GBOARD_BLUE -> stringResource(R.string.settings_theme_palette_gboard_blue)
        FoundationPaletteId.MINT -> stringResource(R.string.settings_theme_palette_mint)
        FoundationPaletteId.ROSE -> stringResource(R.string.settings_theme_palette_rose)
        FoundationPaletteId.VIOLET -> stringResource(R.string.settings_theme_palette_violet)
        FoundationPaletteId.GRAPHITE -> stringResource(R.string.settings_theme_palette_graphite)
    }
}

@Composable
private fun skinLabel(id: SkinThemeId): String {
    return when (id) {
        SkinThemeId.PURE_FLAT -> stringResource(R.string.settings_theme_skin_pure_flat)
    }
}

@Composable
private fun skinDescription(id: SkinThemeId): String {
    return when (id) {
        SkinThemeId.PURE_FLAT -> stringResource(R.string.settings_theme_skin_pure_flat_desc)
    }
}

private fun parseHex(hex: String): Color {
    val raw = hex.trim().removePrefix("#")
    val argb = when (raw.length) {
        6 -> "FF$raw"
        8 -> raw
        else -> return Color.Gray
    }
    if (!argb.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
        return Color.Gray
    }
    return Color(argb.toLong(16).toInt())
}
