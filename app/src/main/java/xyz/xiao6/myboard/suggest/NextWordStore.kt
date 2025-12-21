package xyz.xiao6.myboard.suggest

import android.content.Context
import java.io.File
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.math.pow

@OptIn(ExperimentalSerializationApi::class)
class NextWordStore(
    context: Context,
) {
    private val rootDir = File(context.filesDir, "suggestions")
    private val halfLifeMs = 30L * 24 * 60 * 60 * 1000
    private val minWeight = 0.05
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        explicitNulls = false
    }

    @Serializable
    data class Snapshot(
        val entries: Map<String, Map<String, Entry>> = emptyMap(),
    )

    @Serializable
    data class Entry(
        val weight: Double = 0.0,
        val updatedAt: Long = 0L,
    )

    @Synchronized
    fun record(localeTag: String, prev: String, next: String) {
        val normalized = normalize(localeTag)
        if (normalized.isBlank()) return
        val p = prev.trim()
        val n = next.trim()
        if (p.isBlank() || n.isBlank()) return
        val now = System.currentTimeMillis()
        val snapshot = readSnapshot(normalized, now)
        val nextMap = snapshot.entries.toMutableMap()
        val counts = nextMap[p]?.toMutableMap() ?: mutableMapOf()
        val existing = counts[n]
        val baseWeight = existing?.let { decayWeight(it.weight, now - it.updatedAt) } ?: 0.0
        counts[n] = Entry(weight = baseWeight + 1.0, updatedAt = now)
        nextMap[p] = counts
        writeSnapshot(normalized, snapshot.copy(entries = nextMap))
    }

    @Synchronized
    fun suggest(localeTag: String, prev: String, limit: Int): List<Pair<String, Double>> {
        val normalized = normalize(localeTag)
        if (normalized.isBlank()) return emptyList()
        val p = prev.trim()
        if (p.isBlank()) return emptyList()
        val now = System.currentTimeMillis()
        val snapshot = readSnapshot(normalized, now)
        val map = snapshot.entries[p] ?: return emptyList()
        return map.entries
            .mapNotNull { (word, entry) ->
                val weight = decayWeight(entry.weight, now - entry.updatedAt)
                if (weight <= minWeight) null else word to weight
            }
            .sortedByDescending { it.second }
            .take(limit)
    }

    @Synchronized
    fun clear(localeTag: String) {
        val normalized = normalize(localeTag)
        if (normalized.isBlank()) return
        writeSnapshot(normalized, Snapshot())
    }

    @Synchronized
    fun demoteWord(localeTag: String, word: String, delta: Int) {
        val normalized = normalize(localeTag)
        if (normalized.isBlank()) return
        val w = word.trim()
        if (w.isBlank()) return
        val now = System.currentTimeMillis()
        val snapshot = readSnapshot(normalized, now)
        val nextMap = snapshot.entries.toMutableMap()
        for ((prev, map) in snapshot.entries) {
            val entry = map[w] ?: continue
            val updated = map.toMutableMap()
            val base = decayWeight(entry.weight, now - entry.updatedAt)
            val nextWeight = base + delta
            if (nextWeight <= minWeight) {
                updated.remove(w)
            } else {
                updated[w] = Entry(weight = nextWeight, updatedAt = now)
            }
            if (updated.isEmpty()) nextMap.remove(prev) else nextMap[prev] = updated
        }
        writeSnapshot(normalized, snapshot.copy(entries = nextMap))
    }

    private fun readSnapshot(localeTag: String, now: Long): Snapshot {
        val file = snapshotFile(localeTag)
        if (!file.exists()) return Snapshot()
        val content = file.readText()
        return runCatching { json.decodeFromString(Snapshot.serializer(), content) }
            .getOrElse { Snapshot() }
    }

    private fun writeSnapshot(localeTag: String, snapshot: Snapshot) {
        rootDir.mkdirs()
        snapshotFile(localeTag).writeText(json.encodeToString(Snapshot.serializer(), snapshot))
    }

    private fun snapshotFile(localeTag: String): File = File(rootDir, "next_word_${localeTag}.json")

    private fun normalize(tag: String): String = tag.trim().replace('_', '-').lowercase()

    private fun decayWeight(weight: Double, deltaMs: Long): Double {
        if (weight <= 0.0 || deltaMs <= 0) return weight
        val steps = deltaMs.toDouble() / halfLifeMs.toDouble()
        return weight * 0.5.pow(steps)
    }
}
