package xyz.xiao6.myboard.dictionary

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 用户自定义词条实体。
 * 独立于系统词典，支持导出和迁移。
 */
@Entity(tableName = "user_phrases")
data class UserPhraseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "pinyin") val pinyin: String,
    @ColumnInfo(name = "phrase") val phrase: String,
    @ColumnInfo(name = "frequency") val frequency: Int = 1,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "last_used_at") val lastUsedAt: Long = System.currentTimeMillis()
)
