package xyz.xiao6.myboard.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Slider
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

@Composable
fun FeedbackSettings(
    modifier: Modifier = Modifier,
    prefs: SettingsStore,
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

@Composable
private fun SectionHeader(textRes: Int) {
    androidx.compose.material3.Text(
        text = stringResource(textRes),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
        color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
    )
}
