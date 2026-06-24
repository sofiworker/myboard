package xyz.xiao6.myboard.dictionary

import android.content.Context

/**
 * 词典依赖模块。
 * 统一创建和管理词典相关组件的生命周期。
 */
class DictionaryModule(context: Context) {

    private val database = DictionaryDatabase.getInstance(context)

    val pinyinDictionary: PinyinDictionary by lazy {
        PinyinDictionary(database.dictionaryDao(), database.userDictionaryDao())
    }

    val userDictionary: UserDictionary by lazy {
        UserDictionary(database.userDictionaryDao())
    }

    val adaptiveDictionary: AdaptiveDictionary by lazy {
        AdaptiveDictionary(pinyinDictionary, HotWordCalculator(database.dictionaryDao()))
    }

    val hotWordCalculator: HotWordCalculator by lazy {
        HotWordCalculator(database.dictionaryDao())
    }

    val dictionaryUpdater: DictionaryUpdater by lazy {
        DictionaryUpdater(database)
    }
}
