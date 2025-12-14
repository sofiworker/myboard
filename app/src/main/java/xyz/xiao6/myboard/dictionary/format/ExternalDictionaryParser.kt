package xyz.xiao6.myboard.dictionary.format

import java.io.Reader

/**
 * Parser for an external dictionary source format.
 *
 * Output is a lazy [Sequence] to avoid holding the entire source in memory.
 */
interface ExternalDictionaryParser {
    val formatId: String
    fun parse(reader: Reader): Sequence<DictionaryEntry>
}
