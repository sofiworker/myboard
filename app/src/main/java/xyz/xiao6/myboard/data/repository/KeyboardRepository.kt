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
        val candidates = listOf(path, alias(path))
            .filterNotNull()
            .distinct()

        var lastError: IOException? = null
        candidates.forEach { candidate ->
            val customFile = File(customLayoutsDir, "$candidate.json")
            val jsonString = if (customFile.exists()) {
                try {
                    customFile.readText()
                } catch (e: IOException) {
                    lastError = e
                    return@forEach
                }
            } else {
                try {
                    context.assets.open("keyboards/layouts/$candidate.json").bufferedReader().use { it.readText() }
                } catch (ioException: IOException) {
                    lastError = ioException
                    return@forEach
                }
            }
            return json.decodeFromString<KeyboardData>(jsonString)
        }

        lastError?.printStackTrace()
        return null
    }

    private fun alias(path: String): String? = when (path) {
        "qwerty" -> "en_qwerty"
        "t9" -> "en_t9"
        else -> null
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
