package xyz.xiao6.myboard.dictionary

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 词典更新接口。
 * 支持从文件导入词库、检查更新、导出用户词典。
 */
class DictionaryUpdater(private val database: DictionaryDatabase) {

    companion object {
        private const val TAG = "DictionaryUpdater"
        private const val CURRENT_DICT_VERSION = 1
    }

    /**
     * 检查词库是否有可用更新。
     * 当前为 stub 实现，返回本地版本信息。
     * 后续可通过网络接口查询远程版本。
     */
    suspend fun checkUpdate(): Result<DictionaryUpdateInfo> = withContext(Dispatchers.IO) {
        Log.d(TAG, "checkUpdate called (stub)")
        Result.success(
            DictionaryUpdateInfo(
                currentVersion = CURRENT_DICT_VERSION.toString(),
                latestVersion = CURRENT_DICT_VERSION.toString(),
                updateUrl = "",
                releaseNotes = "当前已是最新版本"
            )
        )
    }

    /**
     * 从指定 URL 下载词库更新。
     * 当前为 stub 实现，返回未实现错误。
     * 后续可通过 OkHttp/Retrofit 实现网络下载。
     */
    suspend fun downloadUpdate(url: String): Result<Unit> = withContext(Dispatchers.IO) {
        Log.d(TAG, "downloadUpdate from '$url' called (stub)")
        Result.failure(NotImplementedError("网络词库更新尚未实现，请通过 importFromTextFile 导入本地词库"))
    }

    /**
     * 从文本文件导入词库。
     * 文件格式：每行 "拼音 词组 [词频]"
     */
    suspend fun importFromTextFile(file: File): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val lines = file.readLines()
            val entities = lines.mapNotNull { line ->
                val parts = line.trim().split("\\s+".toRegex())
                if (parts.size >= 2) {
                    PhraseEntity(
                        pinyin = parts[0],
                        phrase = parts[1],
                        frequency = parts.getOrNull(2)?.toIntOrNull() ?: 1,
                        type = 0
                    )
                } else null
            }
            database.dictionaryDao().insertAll(entities)
            Log.d(TAG, "Imported ${entities.size} phrases from ${file.name}")
            Result.success(entities.size)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import dictionary", e)
            Result.failure(e)
        }
    }

    /**
     * 导出用户词典到文本文件。
     */
    suspend fun exportUserDictionary(file: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entities = database.userDictionaryDao().getAll()
            val text = entities.joinToString("\n") { "${it.pinyin} ${it.phrase} ${it.frequency}" }
            file.writeText(text)
            Log.d(TAG, "Exported ${entities.size} user phrases to ${file.name}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export user dictionary", e)
            Result.failure(e)
        }
    }

    /**
     * 清理过期系统词条。
     */
    suspend fun cleanupOldEntries(olderThanDays: Int = 90) {
        val cutoff = System.currentTimeMillis() - olderThanDays * 24 * 60 * 60 * 1000L
        database.dictionaryDao().cleanupOldSystemWords(cutoff)
    }
}

/**
 * 词库更新信息。
 */
data class DictionaryUpdateInfo(
    val currentVersion: String,
    val latestVersion: String,
    val updateUrl: String,
    val releaseNotes: String
)
