package xyz.xiao6.myboard.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import xyz.xiao6.myboard.data.model.FuzzyPinyinPair
import xyz.xiao6.myboard.data.repository.SettingsRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuzzyPinyinSettingsScreen() {
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }
    val fuzzyPinyinPairs = remember {
        listOf(
            FuzzyPinyinPair("s", "sh", "s <-> sh"),
            FuzzyPinyinPair("c", "ch", "c <-> ch"),
            FuzzyPinyinPair("z", "zh", "z <-> zh"),
            FuzzyPinyinPair("an", "ang", "an <-> ang"),
            FuzzyPinyinPair("en", "eng", "en <-> eng"),
            FuzzyPinyinPair("in", "ing", "in <-> ing")
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Fuzzy Pinyin Settings") }) }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues)) {
            items(fuzzyPinyinPairs) { pair ->
                var isChecked by remember { mutableStateOf(settingsRepository.isFuzzyPinyinEnabled(pair.first, pair.second)) }
                FuzzyPinyinItem(pair = pair, isChecked = isChecked) {
                    isChecked = !isChecked
                    settingsRepository.setFuzzyPinyinEnabled(pair.first, pair.second, isChecked)
                }
            }
        }
    }
}

@Composable
fun FuzzyPinyinItem(pair: FuzzyPinyinPair, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(pair.description, modifier = Modifier.weight(1f))
        Switch(checked = isChecked, onCheckedChange = onCheckedChange)
    }
}
