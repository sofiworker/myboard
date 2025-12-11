package xyz.xiao6.myboard.ui.settings

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import xyz.xiao6.myboard.LayoutEditorActivity
import xyz.xiao6.myboard.R
import xyz.xiao6.myboard.data.repository.KeyboardRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayoutManagerScreen() {
    val context = LocalContext.current
    val keyboardRepository = remember { KeyboardRepository(context) }
    val layouts = keyboardRepository.getAvailableLayouts()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(id = R.string.layout_manager)) })
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues)) {
            items(layouts) { layoutName ->
                LayoutListItem(layoutName = layoutName, isCustom = keyboardRepository.isCustomLayout(layoutName)) {
                    // Copy-on-Write
                    if (!keyboardRepository.isCustomLayout(layoutName)) {
                        val layoutData = keyboardRepository.getKeyboardLayout(layoutName)
                        if (layoutData != null) {
                            keyboardRepository.saveKeyboardLayout(layoutName, layoutData)
                        }
                    }

                    val intent = Intent(context, LayoutEditorActivity::class.java).apply {
                        putExtra(LayoutEditorActivity.EXTRA_LAYOUT_NAME, layoutName)
                    }
                    context.startActivity(intent)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayoutListItem(layoutName: String, isCustom: Boolean, onEditClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = layoutName)
            if (isCustom) {
                Badge(modifier = Modifier.padding(start = 8.dp)) { Text("Custom") }
            }
        }
        Button(onClick = onEditClick) {
            Text(text = stringResource(id = R.string.edit))
        }
    }
}
