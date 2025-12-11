package xyz.xiao6.myboard.data.repository

import android.content.Context
import kotlinx.serialization.json.Json
import xyz.xiao6.myboard.data.model.EmojiData
import java.io.IOException

class EmojiRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    fun getEmojiData(): EmojiData? {
        return try {
            val jsonString = context.assets.open("ime/emojis.json").bufferedReader().use { it.readText() }
            json.decodeFromString<EmojiData>(jsonString)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}
