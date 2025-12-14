package xyz.xiao6.myboard.dictionary.format

/**
 * A single dictionary entry:
 * - [code]: search key (e.g. pinyin "ni hao")
 * - [word]: candidate text (e.g. "你好")
 * - [weight]: optional frequency/priority
 */
data class DictionaryEntry(
    val word: String,
    val code: String,
    val weight: Int = 0,
)

