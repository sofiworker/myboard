package xyz.xiao6.myboard.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.json.Json
import xyz.xiao6.myboard.data.model.theme.ThemeData

class ThemeRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val prefs: SharedPreferences = context.getSharedPreferences("theme", Context.MODE_PRIVATE)

    fun getThemeData(name: String? = null): ThemeData {
        val themeName = name ?: prefs.getString("selected_theme", "default") ?: "default"
        val inputStream = context.assets.open("themes/$themeName.json")
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        val baseTheme = json.decodeFromString(ThemeData.serializer(), jsonString)

        return baseTheme.copy(
            backgroundImageUri = prefs.getString("background_image_uri", null),
            backgroundAlpha = prefs.getFloat("background_alpha", 1.0f)
        )
    }

    fun getAvailableThemes(): List<String> {
        return context.assets.list("themes")?.map { it.removeSuffix(".json") } ?: emptyList()
    }

    fun setSelectedTheme(name: String) {
        prefs.edit().putString("selected_theme", name).apply()
    }

    fun setBackgroundImageUri(uri: String) {
        prefs.edit().putString("background_image_uri", uri).apply()
    }

    fun setBackgroundAlpha(alpha: Float) {
        prefs.edit().putFloat("background_alpha", alpha).apply()
    }
}
