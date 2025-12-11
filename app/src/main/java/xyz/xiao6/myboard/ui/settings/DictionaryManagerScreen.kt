package xyz.xiao6.myboard.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import xyz.xiao6.myboard.R
import xyz.xiao6.myboard.data.downloader.DictionaryDownloader
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryManagerScreen() {
    val context = LocalContext.current
    val downloader = DictionaryDownloader(context)
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.dictionary_manager)) }) }
    ) {
        Column(modifier = Modifier.padding(it)) {
            Button(onClick = { 
                scope.launch {
                    try {
                        // In a real app, this URL would come from a config file or a server.
                        downloader.download(
                            "https://raw.githubusercontent.com/florisboard/florisboard/master/app/src/main/assets/ime/en/dictionary.txt",
                            "updated_en_words.txt"
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }) {
                Text(stringResource(R.string.update_dictionaries))
            }
            
            Text(stringResource(R.string.installed_dictionaries))
            val filesDir = context.filesDir
            val dictionaries = filesDir.listFiles { _, name -> name.endsWith(".txt") } ?: emptyArray()
            for (dictionary in dictionaries) {
                Text(dictionary.name)
            }
        }
    }
}
