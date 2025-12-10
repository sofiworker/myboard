package xyz.xiao6.myboard.data.repository

import android.content.Context
import kotlinx.serialization.json.Json
import xyz.xiao6.myboard.data.model.KeyboardData

class KeyboardRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    fun getKeyboardLayout(name: String): KeyboardData {
        val inputStream = context.assets.open("$name.json")
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        return json.decodeFromString(KeyboardData.serializer(), jsonString)
    }
}
