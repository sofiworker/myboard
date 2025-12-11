package xyz.xiao6.myboard.data.repository

import android.content.Context
import kotlinx.serialization.json.Json
import xyz.xiao6.myboard.data.KeyboardData
import java.io.File
import java.io.IOException

class KeyboardRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val customLayoutsDir = File(context.filesDir, "keyboards/layouts")

    init {
        if (!customLayoutsDir.exists()) {
            customLayoutsDir.mkdirs()
        }
    }

    fun getKeyboardLayout(path: String): KeyboardData? {
        val customFile = File(customLayoutsDir, "$path.json")
        val jsonString = if (customFile.exists()) {
            try {
                customFile.readText()
            } catch (e: IOException) {
                e.printStackTrace()
                return null
            }
        } else {
            try {
                context.assets.open("keyboards/layouts/$path.json").bufferedReader().use { it.readText() }
            } catch (ioException: IOException) {
                ioException.printStackTrace()
                return null
            }
        }
        return json.decodeFromString<KeyboardData>(jsonString)
    }

    fun getAvailableLayouts(): List<String> {
        val assetLayouts = try {
            context.assets.list("keyboards/layouts")?.map { it.removeSuffix(".json") } ?: emptyList()
        } catch (e: IOException) {
            emptyList()
        }

        val customLayouts = customLayoutsDir.listFiles { _, name -> name.endsWith(".json") }?.map {
            it.nameWithoutExtension
        } ?: emptyList()

        return (assetLayouts + customLayouts).distinct().sorted()
    }

    fun isCustomLayout(name: String): Boolean {
        val customFile = File(customLayoutsDir, "$name.json")
        return customFile.exists()
    }

    fun saveKeyboardLayout(name: String, keyboardData: KeyboardData) {
        val customFile = File(customLayoutsDir, "$name.json")
        val jsonString = json.encodeToString(KeyboardData.serializer(), keyboardData)
        try {
            customFile.writeText(jsonString)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
