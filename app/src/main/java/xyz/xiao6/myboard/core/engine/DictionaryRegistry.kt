package xyz.xiao6.myboard.core.engine

import xyz.xiao6.myboard.core.contract.*

/**
 * 字典注册表。
 * 阶段 01 使用 StubDictionaryRegistry，阶段 05 替换真实实现。
 */
interface DictionaryRegistry {
    fun load(key: DictionaryKey): Dictionary?
    fun invalidate(key: DictionaryKey)
}