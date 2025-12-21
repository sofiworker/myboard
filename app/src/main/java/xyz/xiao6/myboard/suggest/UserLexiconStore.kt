package xyz.xiao6.myboard.suggest

import android.content.Context
import java.io.File
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
class UserLexiconStore(
    context: Context,
) {
    private val rootDir = File(context.filesDir, "suggestions")
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        explicitNulls = false
    }

    @Serializable
    data class Snapshot(
        val entries: Map<String, Int> = emptyMap(),
        val blocked: Set<String> = emptySet(),
    )

    @Synchronized
    fun record(localeTag: String, word: String) {
        val normalized = normalize(localeTag)
        if (normalized.isBlank()) return
        val w = word.trim()
        if (w.isBlank()) return
        val snapshot = readSnapshot(normalized)
        if (snapshot.blocked.contains(w)) return
        val next = snapshot.entries.toMutableMap()
        next[w] = (next[w] ?: 0) + 1
        writeSnapshot(normalized, snapshot.copy(entries = next))
    }

    @Synchronized
    fun getPrefixMatches(localeTag: String, prefix: String, limit: Int): List<Pair<String, Int>> {
        val normalized = normalize(localeTag)
        if (normalized.isBlank()) return emptyList()
        val p = prefix.trim()
        if (p.isBlank()) return emptyList()
        val snapshot = readSnapshot(normalized)
        return snapshot.entries
            .asSequence()
            .filter { (w, _) -> w.startsWith(p) && !snapshot.blocked.contains(w) }
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key to it.value }
            .toList()
    }

    @Synchronized
    fun isBlocked(localeTag: String, word: String): Boolean {
        val normalized = normalize(localeTag)
        if (normalized.isBlank()) return false
        val w = word.trim()
        if (w.isBlank()) return false
        return readSnapshot(normalized).blocked.contains(w)
    }

    @Synchronized
    fun block(localeTag: String, word: String) {
        val normalized = normalize(localeTag)
        if (normalized.isBlank()) return
        val w = word.trim()
        if (w.isBlank()) return
        val snapshot = readSnapshot(normalized)
        val blocked = snapshot.blocked.toMutableSet()
        blocked.add(w)
        writeSnapshot(normalized, snapshot.copy(blocked = blocked))
    }

    @Synchronized
    fun adjust(localeTag: String, word: String, delta: Int) {
        val normalized = normalize(localeTag)
        if (normalized.isBlank()) return
        val w = word.trim()
        if (w.isBlank()) return
        val snapshot = readSnapshot(normalized)
        val next = snapshot.entries.toMutableMap()
        val current = (next[w] ?: 0) + delta
        if (current <= 0) {
            next.remove(w)
        } else {
            next[w] = current
        }
        writeSnapshot(normalized, snapshot.copy(entries = next))
    }

    @Synchronized
    fun unblock(localeTag: String, word: String) {
        val normalized = normalize(localeTag)
        if (normalized.isBlank()) return
        val w = word.trim()
        if (w.isBlank()) return
        val snapshot = readSnapshot(normalized)
        val blocked = snapshot.blocked.toMutableSet()
        blocked.remove(w)
        writeSnapshot(normalized, snapshot.copy(blocked = blocked))
    }

    @Synchronized
    fun clear(localeTag: String) {
        val normalized = normalize(localeTag)
        if (normalized.isBlank()) return
        writeSnapshot(normalized, Snapshot())
    }

    @Synchronized
    fun clearBlocked(localeTag: String) {
        val normalized = normalize(localeTag)
        if (normalized.isBlank()) return
        val snapshot = readSnapshot(normalized)
        writeSnapshot(normalized, snapshot.copy(blocked = emptySet()))
    }

    private fun readSnapshot(localeTag: String): Snapshot {
        val file = snapshotFile(localeTag)
        if (!file.exists()) return Snapshot()
        return runCatching { json.decodeFromString(Snapshot.serializer(), file.readText()) }
            .getOrElse { Snapshot() }
    }

    private fun writeSnapshot(localeTag: String, snapshot: Snapshot) {
        rootDir.mkdirs()
        snapshotFile(localeTag).writeText(json.encodeToString(Snapshot.serializer(), snapshot))
    }

    private fun snapshotFile(localeTag: String): File = File(rootDir, "user_lexicon_${localeTag}.json")

    private fun normalize(tag: String): String = tag.trim().replace('_', '-').lowercase()
}
