package xyz.xiao6.myboard.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 工具栏按钮配置实体。
 * 存储工具栏按钮的类型、启用状态和排序位置。
 *
 * 左侧固定（⚙ 设置）和右侧固定（↓ 收起键盘）不存储在此表中，
 * 它们在 UI 层始终渲染。
 */
@Entity(
    tableName = "toolbar_items",
    indices = [Index("sortOrder")]
)
data class ToolbarItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** [ToolbarItemType] 枚举的 name */
    val type: String,
    val enabled: Boolean = true,
    /** 显示顺序，从 0 开始 */
    val sortOrder: Int = 0
)

/**
 * 工具栏中间区可用按钮类型。
 */
enum class ToolbarItemType {
    LOCALE_SWITCH,
    THEME_TOGGLE,
    EMOJI,
    SYMBOL,
    CLIPBOARD,
    LAYOUT_SWITCH,
    VOICE_INPUT
}

/**
 * 工具栏中间区布局模式。
 */
enum class ToolbarLayoutMode {
    /** 横向滚动（默认，主流输入法风格） */
    SCROLLABLE,
    /** 固定均分屏幕宽度 */
    FIXED
}
