package xyz.xiao6.myboard.ui.setup

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import xyz.xiao6.myboard.R
import xyz.xiao6.myboard.SettingsActivity
import xyz.xiao6.myboard.util.isImeEnabled
import xyz.xiao6.myboard.util.isImeSelected

@Preview
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen() {
    val context = LocalContext.current
    var imeEnabled by remember { mutableStateOf(isImeEnabled(context)) }
    var imeSelected by remember { mutableStateOf(isImeSelected(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                imeEnabled = isImeEnabled(context)
                imeSelected = isImeSelected(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(context) {
        val resolver = context.contentResolver
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                imeEnabled = isImeEnabled(context)
                imeSelected = isImeSelected(context)
            }
        }
        resolver.registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.DEFAULT_INPUT_METHOD), false, observer)
        resolver.registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.ENABLED_INPUT_METHODS), false, observer)
        onDispose {
            resolver.unregisterContentObserver(observer)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.setup_wizard)) }) }
    ) {
        Column(modifier = Modifier.padding(it).padding(16.dp)) {
            SetupStep(
                title = stringResource(R.string.step_1_enable_ime),
                isCompleted = imeEnabled
            ) {
                Button(onClick = { context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }) {
                    Text(stringResource(R.string.go_to_settings))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SetupStep(
                title = stringResource(R.string.step_2_select_ime),
                isCompleted = imeSelected,
                isEnabled = imeEnabled
            ) {
                Button(onClick = { (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker() }) {
                    Text(stringResource(R.string.select_input_method))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (imeEnabled && imeSelected) {
                Button(
                    onClick = {
                        context.startActivity(Intent(context, SettingsActivity::class.java))
                        (context as? Activity)?.finish()
                    },
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
