package xyz.xiao6.myboard.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 设置键值对实体。
 * 所有应用设置统一存储在此表中，作为唯一数据源。
 */
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val key: String,
    val stringValue: String = ""
)
