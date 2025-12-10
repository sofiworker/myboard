package xyz.xiao6.myboard.data.repository

import android.content.Context

class DictionaryRepository(private val context: Context) {

    fun getWords(fileName: String): List<String> {
        val inputStream = context.assets.open("dictionary/$fileName")
        return inputStream.bufferedReader().useLines { it.toList() }
    }
}
