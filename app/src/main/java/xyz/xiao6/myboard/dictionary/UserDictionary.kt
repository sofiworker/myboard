package xyz.xiao6.myboard.dictionary

/**
 * 用户词典管理。
 * 提供用户自定义词条的增删查和导出功能。
 */
class UserDictionary(private val userDao: UserDictionaryDao) {

    suspend fun add(pinyin: String, phrase: String, frequency: Int = 1) {
        userDao.insert(
            UserPhraseEntity(
                pinyin = pinyin,
                phrase = phrase,
                frequency = frequency
            )
        )
    }

    suspend fun remove(phrase: String) {
        val all = userDao.getAll()
        val target = all.find { it.phrase == phrase }
        if (target != null) {
            userDao.delete(target)
        }
    }

    suspend fun lookup(pinyin: String, limit: Int = 50): List<UserPhraseEntity> {
        return userDao.lookupByPinyin(pinyin, limit)
    }

    suspend fun getAll(): List<UserPhraseEntity> {
        return userDao.getAll()
    }

    suspend fun count(): Int {
        return userDao.count()
    }

    /**
     * 导出用户词典为文本格式。
     * 格式：每行 "拼音 词组 词频"
     */
    suspend fun exportToText(): String {
        val all = userDao.getAll()
        return all.joinToString("\n") { "${it.pinyin} ${it.phrase} ${it.frequency}" }
    }
}
