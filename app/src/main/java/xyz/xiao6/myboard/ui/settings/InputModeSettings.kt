package xyz.xiao6.myboard.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import xyz.xiao6.myboard.R
import xyz.xiao6.myboard.store.SettingsStore

/**
 * Settings for input modes (Normal, Swype, Handwriting)
 * 输入模式设置（普通、滑行、手写）
 */
@Composable
fun InputModeSettings(
    modifier: Modifier = Modifier,
    prefs: SettingsStore,
) {
    var selectedMode by remember { mutableStateOf(prefs.inputMode) }
    var swypeEnabled by remember { mutableStateOf(prefs.swypeEnabled) }
    var swypeShowTrail by remember { mutableStateOf(prefs.swypeShowTrail) }
    var handwritingEnabled by remember { mutableStateOf(prefs.handwritingEnabled) }
    var handwritingAutoRecognize by remember { mutableStateOf(prefs.handwritingAutoRecognize) }
    var handwritingLayoutMode by remember { mutableStateOf(prefs.handwritingLayoutMode) }
    var handwritingPosition by remember { mutableStateOf(prefs.handwritingPosition) }

    LazyColumn(modifier = modifier, contentPadding = WindowInsets.systemBars.asPaddingValues()) {
        // Section header
        item { SectionHeader(textRes = R.string.settings_section_input_mode) }

        // Input mode selection
        item {
            Text(
                text = stringResource(R.string.settings_input_mode_title),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
            )
        }

        item {
            InputModeOption(
                text = stringResource(R.string.settings_input_mode_normal),
                selected = selectedMode == "NORMAL",
                onClick = {
                    selectedMode = "NORMAL"
                    prefs.inputMode = "NORMAL"
                },
            )
        }

        item {
            InputModeOption(
                text = stringResource(R.string.settings_input_mode_swype),
                selected = selectedMode == "SWYPE",
                onClick = {
                    selectedMode = "SWYPE"
                    prefs.inputMode = "SWYPE"
                },
            )
        }

        item {
            InputModeOption(
                text = stringResource(R.string.settings_input_mode_handwriting),
                selected = selectedMode == "HANDWRITING",
                onClick = {
                    selectedMode = "HANDWRITING"
                    prefs.inputMode = "HANDWRITING"
                },
            )
        }

        // Swype settings
        if (selectedMode == "SWYPE" || swypeEnabled) {
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_swype_enable)) },
                    supportingContent = { Text(stringResource(R.string.settings_swype_enable_desc)) },
                    trailingContent = {
                        Switch(
                            checked = swypeEnabled,
                            onCheckedChange = {
                                swypeEnabled = it
                                prefs.swypeEnabled = it
                            },
                        )
                    },
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_swype_show_trail)) },
                    supportingContent = { Text(stringResource(R.string.settings_swype_show_trail_desc)) },
                    trailingContent = {
                        Switch(
                            checked = swypeShowTrail,
                            onCheckedChange = {
                                swypeShowTrail = it
                                prefs.swypeShowTrail = it
                            },
                        )
                    },
                )
            }
        }

        // Handwriting settings
        if (selectedMode == "HANDWRITING" || handwritingEnabled) {
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_handwriting_enable)) },
                    supportingContent = { Text(stringResource(R.string.settings_handwriting_enable_desc)) },
                    trailingContent = {
                        Switch(
                            checked = handwritingEnabled,
                            onCheckedChange = {
                                handwritingEnabled = it
                                prefs.handwritingEnabled = it
                            },
                        )
                    },
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_handwriting_auto_recognize)) },
                    supportingContent = { Text(stringResource(R.string.settings_handwriting_auto_recognize_desc)) },
                    trailingContent = {
                        Switch(
                            checked = handwritingAutoRecognize,
                            onCheckedChange = {
                                handwritingAutoRecognize = it
                                prefs.handwritingAutoRecognize = it
                            },
                        )
                    },
                )
            }

            // Handwriting layout mode
            item {
                Text(
                    text = stringResource(R.string.settings_handwriting_layout_mode),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                )
            }

            item {
                InputModeOption(
                    text = stringResource(R.string.settings_handwriting_layout_full_screen),
                    selected = handwritingLayoutMode == "FULL_SCREEN",
                    onClick = {
                        handwritingLayoutMode = "FULL_SCREEN"
                        prefs.handwritingLayoutMode = "FULL_SCREEN"
                    },
                )
            }

            item {
                InputModeOption(
                    text = stringResource(R.string.settings_handwriting_layout_half_screen),
                    selected = handwritingLayoutMode == "HALF_SCREEN",
                    onClick = {
                        handwritingLayoutMode = "HALF_SCREEN"
                        prefs.handwritingLayoutMode = "HALF_SCREEN"
                    },
                )
            }

            item {
                InputModeOption(
                    text = stringResource(R.string.settings_handwriting_layout_overlay),
                    selected = handwritingLayoutMode == "OVERLAY",
                    onClick = {
                        handwritingLayoutMode = "OVERLAY"
                        prefs.handwritingLayoutMode = "OVERLAY"
                    },
                )
            }

            // Position option for half-screen mode
            if (handwritingLayoutMode == "HALF_SCREEN") {
                item {
                    Text(
                        text = stringResource(R.string.settings_handwriting_position),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    )
                }

                item {
                    InputModeOption(
                        text = stringResource(R.string.settings_handwriting_position_top),
                        selected = handwritingPosition == "TOP",
                        onClick = {
                            handwritingPosition = "TOP"
                            prefs.handwritingPosition = "TOP"
                        },
                    )
                }

                item {
                    InputModeOption(
                        text = stringResource(R.string.settings_handwriting_position_bottom),
                        selected = handwritingPosition == "BOTTOM",
                        onClick = {
                            handwritingPosition = "BOTTOM"
                            prefs.handwritingPosition = "BOTTOM"
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun InputModeOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(text) },
        trailingContent = {
            RadioButton(
                selected = selected,
                onClick = null,
            )
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun SectionHeader(textRes: Int) {
    androidx.compose.material3.Text(
        text = stringResource(textRes),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
        color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
    )
}
