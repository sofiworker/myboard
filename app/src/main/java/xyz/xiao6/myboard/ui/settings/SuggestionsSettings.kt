package xyz.xiao6.myboard.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import xyz.xiao6.myboard.R
import xyz.xiao6.myboard.store.SettingsStore
import xyz.xiao6.myboard.suggest.SuggestionManager
import java.util.Locale

@Composable
fun SuggestionsSettings(
    modifier: Modifier = Modifier,
    prefs: SettingsStore,
) {
    val context = LocalContext.current
    val suggestionManager = remember { SuggestionManager(context, prefs) }
    val localeTag = prefs.userLocaleTag ?: Locale.getDefault().toLanguageTag()
    var enabled by remember { mutableStateOf(prefs.suggestionEnabled) }
    var learningEnabled by remember { mutableStateOf(prefs.suggestionLearningEnabled) }
    var ngramEnabled by remember { mutableStateOf(prefs.suggestionNgramEnabled) }
    var cloudEnabled by remember { mutableStateOf(prefs.suggestionCloudEnabled) }
    var benchmarkDisableCandidates by remember { mutableStateOf(prefs.benchmarkDisableCandidates) }
    var benchmarkDisableKeyPreview by remember { mutableStateOf(prefs.benchmarkDisableKeyPreview) }
    var benchmarkDisableKeyDecorations by remember { mutableStateOf(prefs.benchmarkDisableKeyDecorations) }
    var benchmarkDisableKeyLabels by remember { mutableStateOf(prefs.benchmarkDisableKeyLabels) }
    var debugTouchLoggingEnabled by remember { mutableStateOf(prefs.debugTouchLoggingEnabled) }
    var endpoint by remember { mutableStateOf(prefs.suggestionCloudEndpoint.orEmpty()) }
    var authType by remember { mutableStateOf(prefs.suggestionCloudAuthType) }
    var authValue by remember { mutableStateOf(prefs.suggestionCloudAuthValue.orEmpty()) }
    var headersJson by remember { mutableStateOf(prefs.suggestionCloudHeadersJson.orEmpty()) }

    val authOptions = listOf("NONE", "API_KEY", "BEARER", "CUSTOM_HEADERS")
    var fontSize by remember { mutableStateOf(prefs.candidateFontSizeSp) }
    var fontWeight by remember { mutableStateOf(prefs.candidateFontWeight) }

    Column(modifier = modifier.padding(16.dp)) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_suggestions_enabled)) },
            trailingContent = {
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        enabled = it
                        prefs.suggestionEnabled = it
                    },
                )
            },
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_suggestions_benchmark_disable)) },
            supportingContent = { Text(stringResource(R.string.settings_suggestions_benchmark_disable_desc)) },
            trailingContent = {
                Switch(
                    checked = benchmarkDisableCandidates,
                    onCheckedChange = {
                        benchmarkDisableCandidates = it
                        prefs.benchmarkDisableCandidates = it
                    },
                )
            },
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_benchmark_disable_key_preview)) },
            supportingContent = { Text(stringResource(R.string.settings_benchmark_disable_key_preview_desc)) },
            trailingContent = {
                Switch(
                    checked = benchmarkDisableKeyPreview,
                    onCheckedChange = {
                        benchmarkDisableKeyPreview = it
                        prefs.benchmarkDisableKeyPreview = it
                    },
                )
            },
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_benchmark_disable_key_decorations)) },
            supportingContent = { Text(stringResource(R.string.settings_benchmark_disable_key_decorations_desc)) },
            trailingContent = {
                Switch(
                    checked = benchmarkDisableKeyDecorations,
                    onCheckedChange = {
                        benchmarkDisableKeyDecorations = it
                        prefs.benchmarkDisableKeyDecorations = it
                    },
                )
            },
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_benchmark_disable_key_labels)) },
            supportingContent = { Text(stringResource(R.string.settings_benchmark_disable_key_labels_desc)) },
            trailingContent = {
                Switch(
                    checked = benchmarkDisableKeyLabels,
                    onCheckedChange = {
                        benchmarkDisableKeyLabels = it
                        prefs.benchmarkDisableKeyLabels = it
                    },
                )
            },
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_debug_touch_logging)) },
            supportingContent = { Text(stringResource(R.string.settings_debug_touch_logging_desc)) },
            trailingContent = {
                Switch(
                    checked = debugTouchLoggingEnabled,
                    onCheckedChange = {
                        debugTouchLoggingEnabled = it
                        prefs.debugTouchLoggingEnabled = it
                    },
                )
            },
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_suggestions_learning)) },
            trailingContent = {
                Switch(
                    checked = learningEnabled,
                    onCheckedChange = {
                        learningEnabled = it
                        prefs.suggestionLearningEnabled = it
                    },
                    enabled = enabled,
                )
            },
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_suggestions_ngram)) },
            supportingContent = { Text(stringResource(R.string.settings_suggestions_ngram_desc)) },
            trailingContent = {
                Switch(
                    checked = ngramEnabled,
                    onCheckedChange = {
                        ngramEnabled = it
                        prefs.suggestionNgramEnabled = it
                    },
                    enabled = enabled && learningEnabled,
                )
            },
        )
        Row(modifier = Modifier.padding(top = 8.dp)) {
            TextButton(onClick = { suggestionManager.clearLearning(localeTag) }) {
                Text(text = stringResource(R.string.settings_suggestions_clear_learning))
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = { suggestionManager.clearBlocked(localeTag) }) {
                Text(text = stringResource(R.string.settings_suggestions_clear_blocked))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        SectionHeader(textRes = R.string.settings_candidate_font)

        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_candidate_font_size)) },
            supportingContent = { Text("${fontSize.toInt()} sp") },
        )
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Slider(
                value = fontSize,
                onValueChange = {
                    fontSize = it
                    prefs.candidateFontSizeSp = it
                },
                valueRange = 12f..24f,
            )
        }

        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_candidate_font_weight)) },
            supportingContent = { Text("$fontWeight") },
        )
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Slider(
                value = fontWeight.toFloat(),
                onValueChange = {
                    fontWeight = it.toInt()
                    prefs.candidateFontWeight = it.toInt()
                },
                valueRange = 100f..900f,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_suggestions_cloud)) },
            trailingContent = {
                Switch(
                    checked = cloudEnabled,
                    onCheckedChange = {
                        cloudEnabled = it
                        prefs.suggestionCloudEnabled = it
                    },
                )
            },
        )
        if (cloudEnabled) {
            TextField(
                value = endpoint,
                onValueChange = {
                    endpoint = it
                    prefs.suggestionCloudEndpoint = it
                },
                label = { Text(stringResource(R.string.settings_suggestions_cloud_endpoint)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_suggestions_cloud_auth_type)) },
                supportingContent = { Text(authType) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val idx = authOptions.indexOf(authType).coerceAtLeast(0)
                        val next = authOptions[(idx + 1) % authOptions.size]
                        authType = next
                        prefs.suggestionCloudAuthType = next
                    },
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = authValue,
                onValueChange = {
                    authValue = it
                    prefs.suggestionCloudAuthValue = it
                },
                label = { Text(stringResource(R.string.settings_suggestions_cloud_auth_value)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = headersJson,
                onValueChange = {
                    headersJson = it
                    prefs.suggestionCloudHeadersJson = it
                },
                label = { Text(stringResource(R.string.settings_suggestions_cloud_headers_json)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_suggestions_cloud_placeholder),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
