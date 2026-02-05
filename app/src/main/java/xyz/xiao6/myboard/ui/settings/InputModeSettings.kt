package xyz.xiao6.myboard.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import xyz.xiao6.myboard.R
import xyz.xiao6.myboard.store.SettingsStore

/**
 * Settings for handwriting input configuration
 * 手写输入配置（线条颜色、粗细、识别速度等）
 */
@Composable
fun InputModeSettings(
    modifier: Modifier = Modifier,
    prefs: SettingsStore,
) {
    var strokeWidth by remember { mutableFloatStateOf(prefs.handwritingStrokeWidth) }
    var strokeColor by remember { mutableIntStateOf(prefs.handwritingStrokeColor) }
    var recognitionDelayMs by remember { mutableStateOf(prefs.handwritingRecognitionDelayMs.toFloat()) }
    var autoRecognize by remember { mutableStateOf(prefs.handwritingAutoRecognize) }
    var layoutMode by remember { mutableStateOf(prefs.handwritingLayoutMode) }
    var position by remember { mutableStateOf(prefs.handwritingPosition) }
    
    var voiceEnabled by remember { mutableStateOf(prefs.voiceInputEnabled) }

    LazyColumn(modifier = modifier, contentPadding = WindowInsets.systemBars.asPaddingValues()) {
        item { SectionHeader(textRes = R.string.settings_section_input_mode) }

        // --- Voice Input Section ---
        item {
            Text(
                text = "Voice Input",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        item {
            ListItem(
                headlineContent = { Text("Enable Offline Voice Input") },
                supportingContent = { Text("Long press Space key to trigger") },
                trailingContent = {
                    Switch(
                        checked = voiceEnabled,
                        onCheckedChange = {
                            voiceEnabled = it
                            prefs.voiceInputEnabled = it
                        },
                    )
                },
            )
        }
        item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

        // --- Handwriting Section ---
        item {
            Text(
                text = "Handwriting",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        // Layout Mode
        item {
            Text(
                text = "Layout Mode",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        item {
            InputModeOption(
                text = "Full Screen",
                selected = layoutMode == "FULL_SCREEN",
                onClick = {
                    layoutMode = "FULL_SCREEN"
                    prefs.handwritingLayoutMode = "FULL_SCREEN"
                }
            )
        }
        item {
            InputModeOption(
                text = "Half Screen",
                selected = layoutMode == "HALF_SCREEN",
                onClick = {
                    layoutMode = "HALF_SCREEN"
                    prefs.handwritingLayoutMode = "HALF_SCREEN"
                }
            )
        }

        // Position (for Half Screen)
        if (layoutMode == "HALF_SCREEN") {
            item {
                Text(
                    text = "Position",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            item {
                InputModeOption(
                    text = "Top",
                    selected = position == "TOP",
                    onClick = {
                        position = "TOP"
                        prefs.handwritingPosition = "TOP"
                    }
                )
            }
            item {
                InputModeOption(
                    text = "Bottom",
                    selected = position == "BOTTOM",
                    onClick = {
                        position = "BOTTOM"
                        prefs.handwritingPosition = "BOTTOM"
                    }
                )
            }
        }

        // Auto Recognize
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_handwriting_auto_recognize)) },
                supportingContent = { Text(stringResource(R.string.settings_handwriting_auto_recognize_desc)) },
                trailingContent = {
                    Switch(
                        checked = autoRecognize,
                        onCheckedChange = {
                            autoRecognize = it
                            prefs.handwritingAutoRecognize = it
                        },
                    )
                },
            )
        }

        // Recognition Speed
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "Recognition Speed: ${recognitionDelayMs.toLong()} ms",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = recognitionDelayMs,
                    onValueChange = { recognitionDelayMs = it },
                    onValueChangeFinished = { prefs.handwritingRecognitionDelayMs = recognitionDelayMs.toLong() },
                    valueRange = 100f..2000f,
                    steps = 18
                )
            }
        }

        // Stroke Width
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "Stroke Width: ${strokeWidth.toInt()} px",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = strokeWidth,
                    onValueChange = { strokeWidth = it },
                    onValueChangeFinished = { prefs.handwritingStrokeWidth = strokeWidth },
                    valueRange = 1f..50f,
                    steps = 49
                )
            }
        }

        // Stroke Color
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "Stroke Color",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                ColorPickerRow(
                    selectedColor = strokeColor,
                    onColorSelected = {
                        strokeColor = it
                        prefs.handwritingStrokeColor = it
                    }
                )
            }
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
private fun InputModeOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(text) },
        trailingContent = {
            androidx.compose.material3.RadioButton(
                selected = selected,
                onClick = null,
            )
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun ColorPickerRow(
    selectedColor: Int,
    onColorSelected: (Int) -> Unit
) {
    val colors = listOf(
        Color.Black,
        Color.Red,
        Color.Blue,
        Color.Green,
        Color.Magenta,
        Color.Cyan,
        Color(0xFFFFA500) // Orange
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        colors.forEach { color ->
            ColorCircle(
                color = color,
                isSelected = color.toArgb() == selectedColor,
                onClick = { onColorSelected(color.toArgb()) }
            )
        }
    }
}

@Composable
private fun ColorCircle(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 4.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}