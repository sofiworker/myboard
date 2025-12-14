package xyz.xiao6.myboard.dictionary.format

import java.io.BufferedReader
import java.io.Reader

/**
 * Parses Rime `*.dict.yaml` (including rime-ice dictionaries).
 *
 * Expected entry lines (after YAML header):
 *   <word>\t<code>\t<weight?>
 */
class RimeDictYamlExternalParser : ExternalDictionaryParser {
    override val formatId: String = "rime_dict_yaml"

    data class Header(
        val name: String? = null,
        val versionText: String? = null,
    )

    /**
     * Parses both header info (best-effort) and body entries.
     */
    fun parseWithHeader(reader: Reader): Pair<Header, Sequence<DictionaryEntry>> {
        val buffered = if (reader is BufferedReader) reader else BufferedReader(reader)
        val headerLines = ArrayList<String>(256)
        val bodyLines = ArrayList<String>(2048)

        var inBody = false
        while (true) {
            val raw = buffered.readLine() ?: break
            val line = raw.trimEnd()
            if (!inBody) {
                headerLines += line
                if (line.trim() == "...") {
                    inBody = true
                }
                continue
            }
            bodyLines += line
        }

        val header = parseHeader(headerLines)
        val entries = parseBody(bodyLines)
        return header to entries
    }

    override fun parse(reader: Reader): Sequence<DictionaryEntry> = parseWithHeader(reader).second

    private fun parseHeader(lines: List<String>): Header {
        // Intentionally minimal: we only need a couple of fields for metadata defaults.
        var name: String? = null
        var version: String? = null
        for (line in lines) {
            val s = line.trim()
            if (s.startsWith("#") || s.isBlank()) continue
            if (s.startsWith("name:")) name = s.removePrefix("name:").trim().trim('"')
            if (s.startsWith("version:")) version = s.removePrefix("version:").trim().trim('"')
        }
        return Header(name = name?.takeIf { it.isNotBlank() }, versionText = version?.takeIf { it.isNotBlank() })
    }

    private fun parseBody(lines: List<String>): Sequence<DictionaryEntry> = sequence {
        for (lineRaw in lines) {
            val line = lineRaw.trim()
            if (line.isBlank() || line.startsWith("#")) continue
            var parts = line.split('\t')
            if (parts.size < 2) parts = line.split(Regex("\\s+"))
            if (parts.size < 2) continue

            val word = parts[0].trim()
            val code = parts[1].trim()
            if (word.isEmpty() || code.isEmpty()) continue

            val weight = parts.getOrNull(2)?.trim()?.toIntOrNull() ?: 0
            yield(DictionaryEntry(word = word, code = code, weight = weight))
        }
    }
}
