package xyz.xiao6.myboard.ui.setup

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import xyz.xiao6.myboard.R
import xyz.xiao6.myboard.SettingsActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen() {
    val context = LocalContext.current
    var isImeEnabled by remember { mutableStateOf(isImeEnabled(context)) }
    var isImeSelected by remember { mutableStateOf(isImeSelected(context)) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.setup_wizard)) }) }
    ) {
        Column(modifier = Modifier.padding(it).padding(16.dp)) {
            SetupStep(
                title = stringResource(R.string.step_1_enable_ime),
                isCompleted = isImeEnabled
            ) {
                Button(onClick = { context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }) {
                    Text(stringResource(R.string.go_to_settings))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SetupStep(
                title = stringResource(R.string.step_2_select_ime),
                isCompleted = isImeSelected,
                isEnabled = isImeEnabled
            ) {
                Button(onClick = { (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker() }) {
                    Text(stringResource(R.string.select_input_method))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (isImeEnabled && isImeSelected) {
                Button(
                    onClick = { context.startActivity(Intent(context, SettingsActivity::class.java)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.finish_setup))
                }
            }
        }
    }
}

@Composable
private fun SetupStep(title: String, isCompleted: Boolean, isEnabled: Boolean = true, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (isCompleted) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Completed", tint = MaterialTheme.colorScheme.primary)
            } else {
                Icon(Icons.Default.CheckCircle, contentDescription = "Incomplete", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
            }
            Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            if (!isCompleted) {
                content()
            }
        }
    }
}

private fun isImeEnabled(context: Context): Boolean {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    return imm.enabledInputMethodList.any { it.packageName == context.packageName }
}

private fun isImeSelected(context: Context): Boolean {
    val currentIme = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
    return currentIme.startsWith(context.packageName)
}
