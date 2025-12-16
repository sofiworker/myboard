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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import xyz.xiao6.myboard.R
import xyz.xiao6.myboard.ime.MyBoardImeService
import xyz.xiao6.myboard.manager.LayoutManager
import xyz.xiao6.myboard.manager.SubtypeManager
import xyz.xiao6.myboard.model.LocaleLayoutProfile
import xyz.xiao6.myboard.store.MyBoardPrefs
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
