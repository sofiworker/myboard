package xyz.xiao6.myboard.model

import xyz.xiao6.myboard.R

/**
 * 按键分类枚举，用于布局编辑器的按键调色板分组展示
 */
enum class EditorKeyCategory(val labelResId: Int) {
    CHARACTER(R.string.editor_category_character),   // 字符键
    COMMAND(R.string.editor_category_command),       // 命令键
    MODIFIER(R.string.editor_category_modifier),     // 修饰键
    NAVIGATION(R.string.editor_category_navigation), // 导航键
    EDITING(R.string.editor_category_editing),       // 编辑键
    FUNCTION(R.string.editor_category_function),     // 功能键
}

/**
 * 扩展后的按键类型枚举，用于布局编辑器
 */
enum class EditorKeyType(
    val category: EditorKeyCategory,
    val labelResId: Int,
    val defaultLabel: String,
    val primaryCode: Int,
    val styleId: String,
    val widthWeight: Float = 1f,
    val iconName: String? = null,
) {
    // ========== 字符键 ==========
    TEXT(
        category = EditorKeyCategory.CHARACTER,
        labelResId = R.string.layout_editor_key_type_text,
        defaultLabel = "a",
        primaryCode = 0,
        styleId = "style_alpha_key",
    ),

    // ========== 命令键 ==========
    SPACE(
        category = EditorKeyCategory.COMMAND,
        labelResId = R.string.layout_editor_key_type_space,
        defaultLabel = "",
        primaryCode = KeyPrimaryCodes.SPACE,
        styleId = "style_function_key_important",
        widthWeight = 3f,
    ),
    ENTER(
        category = EditorKeyCategory.COMMAND,
        labelResId = R.string.layout_editor_key_type_enter,
        defaultLabel = "Enter",
        primaryCode = KeyPrimaryCodes.ENTER,
        styleId = "style_function_key_important",
    ),
    BACKSPACE(
        category = EditorKeyCategory.COMMAND,
        labelResId = R.string.layout_editor_key_type_backspace,
        defaultLabel = "⌫",
        primaryCode = KeyPrimaryCodes.BACKSPACE,
        styleId = "style_function_key",
        iconName = "ic_backspace",
    ),
    TAB(
        category = EditorKeyCategory.COMMAND,
        labelResId = R.string.layout_editor_key_type_tab,
        defaultLabel = "Tab",
        primaryCode = 0,
        styleId = "style_function_key",
    ),

    // ========== 修饰键 ==========
    SHIFT(
        category = EditorKeyCategory.MODIFIER,
        labelResId = R.string.layout_editor_key_type_shift,
        defaultLabel = "⇧",
        primaryCode = KeyPrimaryCodes.SHIFT,
        styleId = "style_function_key",
        iconName = "ic_shift",
    ),
    CAPS_LOCK(
        category = EditorKeyCategory.MODIFIER,
        labelResId = R.string.editor_key_type_caps_lock,
        defaultLabel = "Caps",
        primaryCode = KeyPrimaryCodes.SHIFT,
        styleId = "style_function_key",
        iconName = "ic_caps_lock",
    ),
    ALT(
        category = EditorKeyCategory.MODIFIER,
        labelResId = R.string.editor_key_type_alt,
        defaultLabel = "Alt",
        primaryCode = 0,
        styleId = "style_function_key",
    ),

    // ========== 导航键 ==========
    CURSOR_LEFT(
        category = EditorKeyCategory.NAVIGATION,
        labelResId = R.string.editor_key_type_cursor_left,
        defaultLabel = "←",
        primaryCode = 0,
        styleId = "style_function_key",
        iconName = "ic_arrow_left",
    ),
    CURSOR_RIGHT(
        category = EditorKeyCategory.NAVIGATION,
        labelResId = R.string.editor_key_type_cursor_right,
        defaultLabel = "→",
        primaryCode = 0,
        styleId = "style_function_key",
        iconName = "ic_arrow_right",
    ),
    CURSOR_UP(
        category = EditorKeyCategory.NAVIGATION,
        labelResId = R.string.editor_key_type_cursor_up,
        defaultLabel = "↑",
        primaryCode = 0,
        styleId = "style_function_key",
        iconName = "ic_arrow_up",
    ),
    CURSOR_DOWN(
        category = EditorKeyCategory.NAVIGATION,
        labelResId = R.string.editor_key_type_cursor_down,
        defaultLabel = "↓",
        primaryCode = 0,
        styleId = "style_function_key",
        iconName = "ic_arrow_down",
    ),
    HOME(
        category = EditorKeyCategory.NAVIGATION,
        labelResId = R.string.editor_key_type_home,
        defaultLabel = "Home",
        primaryCode = 0,
        styleId = "style_function_key",
    ),
    END(
        category = EditorKeyCategory.NAVIGATION,
        labelResId = R.string.editor_key_type_end,
        defaultLabel = "End",
        primaryCode = 0,
        styleId = "style_function_key",
    ),

    // ========== 编辑键 ==========
    CUT(
        category = EditorKeyCategory.EDITING,
        labelResId = R.string.editor_key_type_cut,
        defaultLabel = "Cut",
        primaryCode = 0,
        styleId = "style_function_key",
        iconName = "ic_cut",
    ),
    COPY(
        category = EditorKeyCategory.EDITING,
        labelResId = R.string.editor_key_type_copy,
        defaultLabel = "Copy",
        primaryCode = 0,
        styleId = "style_function_key",
        iconName = "ic_copy",
    ),
    PASTE(
        category = EditorKeyCategory.EDITING,
        labelResId = R.string.editor_key_type_paste,
        defaultLabel = "Paste",
        primaryCode = 0,
        styleId = "style_function_key",
        iconName = "ic_paste",
    ),
    SELECT_ALL(
        category = EditorKeyCategory.EDITING,
        labelResId = R.string.editor_key_type_select_all,
        defaultLabel = "Sel",
        primaryCode = 0,
        styleId = "style_function_key",
    ),
    UNDO(
        category = EditorKeyCategory.EDITING,
        labelResId = R.string.editor_key_type_undo,
        defaultLabel = "Undo",
        primaryCode = 0,
        styleId = "style_function_key",
        iconName = "ic_undo",
    ),
    REDO(
        category = EditorKeyCategory.EDITING,
        labelResId = R.string.editor_key_type_redo,
        defaultLabel = "Redo",
        primaryCode = 0,
        styleId = "style_function_key",
        iconName = "ic_redo",
    ),

    // ========== 功能键 ==========
    LOCALE_TOGGLE(
        category = EditorKeyCategory.FUNCTION,
        labelResId = R.string.editor_key_type_locale_toggle,
        defaultLabel = "🌐",
        primaryCode = KeyPrimaryCodes.LOCALE_TOGGLE,
        styleId = "style_function_key",
        iconName = "ic_language",
    ),
    MODE_SWITCH(
        category = EditorKeyCategory.FUNCTION,
        labelResId = R.string.editor_key_type_mode_switch,
        defaultLabel = "123",
        primaryCode = KeyPrimaryCodes.MODE_SWITCH,
        styleId = "style_function_key",
    ),
    LAYER_SWITCH(
        category = EditorKeyCategory.FUNCTION,
        labelResId = R.string.editor_key_type_layer_switch,
        defaultLabel = "?123",
        primaryCode = 0,
        styleId = "style_function_key",
    ),
    SYMBOLS_PANEL(
        category = EditorKeyCategory.FUNCTION,
        labelResId = R.string.editor_key_type_symbols_panel,
        defaultLabel = "☺",
        primaryCode = KeyPrimaryCodes.SYMBOLS_PANEL,
        styleId = "style_function_key",
        iconName = "ic_emoji",
    ),
    ;

    companion object {
        /** 按分类获取所有按键类型 */
        fun byCategory(category: EditorKeyCategory): List<EditorKeyType> {
            return entries.filter { it.category == category }
        }

        /** 获取所有分类及其按键类型 */
        fun groupedByCategory(): Map<EditorKeyCategory, List<EditorKeyType>> {
            return entries.groupBy { it.category }
        }
    }
}

/**
 * 按键模板数据类，用于构建实际的Key对象
 */
data class KeyTemplate(
    val type: EditorKeyType,
    val label: String = type.defaultLabel,
    val longPressText: String = "",
    val widthWeight: Float = type.widthWeight,
)

/**
 * 预设按键行数据类
 */
data class KeyPresetRow(
    val nameResId: Int,
    val keys: List<KeyTemplate>,
)

/**
 * 常用预设按键行
 */
object KeyPresets {
    val numberRow = KeyPresetRow(
        nameResId = R.string.preset_number_row,
        keys = "1234567890".map { KeyTemplate(EditorKeyType.TEXT, it.toString()) },
    )

    val punctuationRow = KeyPresetRow(
        nameResId = R.string.preset_punctuation_row,
        keys = ",.;'/-=[]".map { KeyTemplate(EditorKeyType.TEXT, it.toString()) },
    )

    val navigationRow = KeyPresetRow(
        nameResId = R.string.preset_navigation_row,
        keys = listOf(
            KeyTemplate(EditorKeyType.HOME),
            KeyTemplate(EditorKeyType.CURSOR_LEFT),
            KeyTemplate(EditorKeyType.CURSOR_UP),
            KeyTemplate(EditorKeyType.CURSOR_DOWN),
            KeyTemplate(EditorKeyType.CURSOR_RIGHT),
            KeyTemplate(EditorKeyType.END),
        ),
    )

    val editingRow = KeyPresetRow(
        nameResId = R.string.preset_editing_row,
        keys = listOf(
            KeyTemplate(EditorKeyType.CUT),
            KeyTemplate(EditorKeyType.COPY),
            KeyTemplate(EditorKeyType.PASTE),
            KeyTemplate(EditorKeyType.UNDO),
            KeyTemplate(EditorKeyType.REDO),
        ),
    )

    val allPresets = listOf(numberRow, punctuationRow, navigationRow, editingRow)
}
