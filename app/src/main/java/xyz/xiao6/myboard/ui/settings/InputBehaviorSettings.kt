package xyz.xiao6.myboard.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
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
fun InputBehaviorSettings(
    modifier: Modifier = Modifier,
    prefs: SettingsStore,
) {
    var clearAfterToken by remember { mutableStateOf(prefs.clearInputAfterTokenClear) }
    var clearDelayMs by remember { mutableStateOf(prefs.clearInputAfterTokenClearDelayMs.toFloat()) }

    LazyColumn(modifier = modifier, contentPadding = WindowInsets.systemBars.asPaddingValues()) {
        item { SectionHeader(textRes = R.string.settings_section_input) }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_clear_input_after_token_clear)) },
                supportingContent = { Text(stringResource(R.string.settings_clear_input_after_token_clear_desc)) },
                trailingContent = {
                    Switch(
                        checked = clearAfterToken,
                        onCheckedChange = {
                            clearAfterToken = it
                            prefs.clearInputAfterTokenClear = it
                        },
                    )
                },
            )
        }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_clear_input_delay)) },
                supportingContent = {
                    Text(stringResource(R.string.settings_clear_input_delay_desc, clearDelayMs.toInt()))
                },
            )
        }
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Slider(
                    value = clearDelayMs,
                    onValueChange = { clearDelayMs = it },
                    enabled = clearAfterToken,
                    valueRange = 120f..1500f,
                    onValueChangeFinished = {
                        prefs.clearInputAfterTokenClearDelayMs = clearDelayMs.toInt()
                    },
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
