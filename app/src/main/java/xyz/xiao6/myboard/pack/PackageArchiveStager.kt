package xyz.xiao6.myboard.pack

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.text.Normalizer
import java.util.Locale
import java.util.zip.ZipInputStream
import xyz.xiao6.myboard.contract.manifest.LanguagePackManifest

data class PackageArchiveLimits(
    val maxEntries: Int = 512,
    val maxEntryBytes: Long = 16L * 1024 * 1024,
    val maxTotalBytes: Long = 64L * 1024 * 1024
) {
    init {
        require(maxEntries > 0 && maxEntryBytes > 0 && maxTotalBytes > 0)
    }
}

/**
 * Performs archive-only validation. Manifest decoding is injected so external
 * JSON parsing remains separate from ZIP traversal and resource limits.
 */
class PackageArchiveStager(
    private val limits: PackageArchiveLimits = PackageArchiveLimits()
) {
    fun stage(
        input: InputStream,
        decodeManifest: (ByteArray) -> LanguagePackManifest
    ): PackagePayload {
        val entries = linkedMapOf<String, ByteArray>()
        var totalBytes = 0L

        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) {
                    zip.closeEntry()
                    continue
                }
                check(entries.size < limits.maxEntries) { "Package archive contains too many entries" }
                val path = normalizeArchivePath(entry.name)
                check(path !in entries) { "Duplicate archive entry '$path'" }
                check(!isExecutableContent(path)) { "Executable archive entry '$path' is not allowed" }

                val content = ByteArrayOutputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var entryBytes = 0L
                    while (true) {
                        val count = zip.read(buffer)
                        if (count < 0) break
                        entryBytes += count
                        totalBytes += count
                        check(entryBytes <= limits.maxEntryBytes) { "Archive entry '$path' exceeds the size limit" }
                        check(totalBytes <= limits.maxTotalBytes) { "Package archive exceeds the total size limit" }
                        output.write(buffer, 0, count)
                    }
                    output.toByteArray()
                }
                entries[path] = content
                zip.closeEntry()
            }
        }

        val manifestBytes = requireNotNull(entries.remove(MANIFEST_PATH)) {
            "Package archive is missing $MANIFEST_PATH"
        }
        return PackagePayload(
            manifest = decodeManifest(manifestBytes),
            resources = entries
        )
    }

    private fun normalizeArchivePath(path: String): String {
        val normalized = Normalizer.normalize(path, Normalizer.Form.NFC)
        require(normalized.isNotBlank() && !normalized.startsWith('/') && '\\' !in normalized) {
            "Archive entry must use a package-relative POSIX path"
        }
        val segments = normalized.split('/')
        require(segments.all { it.isNotBlank() && it != "." && it != ".." }) {
            "Archive entry must not escape the package"
        }
        return segments.joinToString("/")
    }

    private fun isExecutableContent(path: String): Boolean {
        val lower = path.lowercase(Locale.ROOT)
        return EXECUTABLE_SUFFIXES.any(lower::endsWith)
    }

    private companion object {
        const val MANIFEST_PATH = "manifest.json"
        val EXECUTABLE_SUFFIXES = setOf(
            ".dex", ".jar", ".class", ".so", ".apk", ".sh", ".bat", ".cmd",
            ".ps1", ".kts", ".js", ".py", ".exe", ".dll"
        )
    }
}
