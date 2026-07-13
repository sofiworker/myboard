package xyz.xiao6.myboard.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import xyz.xiao6.myboard.R
import xyz.xiao6.myboard.data.repository.SettingsRepository
import xyz.xiao6.myboard.data.settings.KeyboardHeightPolicy

/**
 * 输入设置页面。
 * 管理双击空格句号、自动大写、按键字号、键盘高度等输入行为。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(
            SettingsRepository(
                xyz.xiao6.myboard.data.db.SettingsDatabase.getInstance(
                    androidx.compose.ui.platform.LocalContext.current
                ).settingsDao()
            )
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val keyboardHeight = KeyboardHeightPolicy.resolve(
        storedHeight = uiState.settings[KeyboardHeightPolicy.KEY_HEIGHT],
        screenHeightDp = configuration.screenHeightDp
    ).heightDp.toFloat()
    val keyboardHorizontalInset = KeyboardHeightPolicy.resolveHorizontalInset(
        storedInset = uiState.settings[KeyboardHeightPolicy.KEY_HORIZONTAL_INSET],
        screenWidthDp = configuration.screenWidthDp
    ).insetDp.toFloat()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_section_input)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
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
            // 键盘布局
            item { SectionHeader(stringResource(R.string.settings_section_keyboard)) }
            item {
                SliderItem(
                    title = stringResource(R.string.settings_keyboard_height),
                    value = keyboardHeight,
                    onValueChange = { viewModel.updateSetting(KeyboardHeightPolicy.KEY_HEIGHT, it.toInt().toString()) },
                    valueRange = KeyboardHeightPolicy.MIN_HEIGHT_DP.toFloat()..KeyboardHeightPolicy.MAX_HEIGHT_DP.toFloat(),
                    suffix = "dp"
                )
            }
            item {
                SliderItem(
                    title = stringResource(R.string.settings_keyboard_horizontal_inset),
                    value = keyboardHorizontalInset,
                    onValueChange = { viewModel.updateSetting(KeyboardHeightPolicy.KEY_HORIZONTAL_INSET, it.toInt().toString()) },
                    valueRange = KeyboardHeightPolicy.MIN_HORIZONTAL_INSET_DP.toFloat()..KeyboardHeightPolicy.MAX_HORIZONTAL_INSET_DP.toFloat(),
                    suffix = "dp"
                )
            }
            item {
                SliderItem(
                    title = stringResource(R.string.settings_key_font_size),
                    value = (uiState.settings[SettingsRepository.KEY_KEY_FONT_SIZE] ?: "18").toFloatOrNull() ?: 18f,
                    onValueChange = {
                        viewModel.updateSetting(SettingsRepository.KEY_KEY_FONT_SIZE, it.toInt().toString())
                    },
                    valueRange = 12f..24f,
                    suffix = "sp"
                )
            }

            // 输入行为
            item { SectionHeader(stringResource(R.string.settings_input_behavior)) }
            item {
                SwitchItem(
                    title = stringResource(R.string.settings_double_space_period),
                    subtitle = stringResource(R.string.settings_double_space_period_desc),
                    icon = Icons.Default.SpaceBar,
                    checked = uiState.settings["double_space_period"]?.toBooleanStrictOrNull() ?: true,
                    onCheckedChange = { viewModel.updateSetting("double_space_period", it.toString()) }
                )
            }
            item {
                SwitchItem(
                    title = stringResource(R.string.settings_auto_capitalize),
                    subtitle = stringResource(R.string.settings_auto_capitalize_desc),
                    icon = Icons.Default.TextFormat,
                    checked = uiState.settings["auto_capitalize"]?.toBooleanStrictOrNull() ?: true,
                    onCheckedChange = { viewModel.updateSetting("auto_capitalize", it.toString()) }
                )
            }

            // 语言
            item { SectionHeader(stringResource(R.string.settings_language_layout)) }
            item {
                NavigationItem(
                    title = stringResource(R.string.settings_current_language),
                    subtitle = uiState.settings["current_locale"] ?: "en-US",
                    icon = Icons.Default.Language,
                    onClick = { }
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}
