package xyz.xiao6.myboard.data.repository

import android.content.Context
import kotlinx.serialization.json.Json
import xyz.xiao6.myboard.data.model.theme.ThemeData

class ThemeRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    fun getThemeData(name: String): ThemeData {
        val inputStream = context.assets.open("themes/$name.json")
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        return json.decodeFromString(ThemeData.serializer(), jsonString)
    }
}
