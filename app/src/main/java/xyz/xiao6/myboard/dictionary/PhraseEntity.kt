package xyz.xiao6.myboard.dictionary

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 系统词典词条实体。
 * 存储拼音到词组的映射及词频信息。
 */
@Entity(tableName = "phrases")
data class PhraseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "pinyin") val pinyin: String,
    @ColumnInfo(name = "phrase") val phrase: String,
    @ColumnInfo(name = "frequency") val frequency: Int = 0,
    @ColumnInfo(name = "type") val type: Int = 0, // 0=系统 1=用户导入 2=热词
    @ColumnInfo(name = "created_at") val createdAt: Long = 0,
    @ColumnInfo(name = "last_used_at") val lastUsedAt: Long = 0
)
