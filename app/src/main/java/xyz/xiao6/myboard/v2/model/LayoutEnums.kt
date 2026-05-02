package xyz.xiao6.myboard.v2.model

/**
 * 横向 / 纵向。
 */
enum class Orientation {
    HORIZONTAL,
    VERTICAL
}

/**
 * 环境方向。
 */
enum class EnvOrientation {
    PORTRAIT,
    LANDSCAPE,
    AUTO
}

/**
 * 环境主题。
 */
enum class EnvTheme {
    LIGHT,
    DARK,
    AUTO
}

/**
 * 屏幕密度档位。
 */
enum class Density {
    LDPI,
    MDPI,
    HDPI,
    XHDPI,
    XXHDPI,
    XXXHDPI,
    AUTO
}

/**
 * 容器布局类型。
 */
enum class LayoutType {
    /** 行集合，例如 qwerty 主区 */
    ROWS,

    /** 网格，例如 T9 */
    GRID,

    /** 线性布局 */
    LINEAR,

    /** 绝对布局 */
    ABSOLUTE,

    /** 层叠布局 */
    STACK,

    /** 自定义布局 */
    CUSTOM
}

/**
 * region 的语义角色。
 */
enum class RegionRole {
    /** 主键盘区 */
    KEYBOARD,

    /** 候选区 */
    CANDIDATE,

    /** 工具栏 */
    TOOLBAR,

    /** 侧边栏 */
    SIDEBAR,

    /** 底部区域 */
    FOOTER,

    /** 顶部区域 */
    HEADER,

    /** 弹出区域 */
    POPUP,

    /** 自定义 */
    CUSTOM
}

/**
 * 节点类型。
 */
enum class NodeType {
    /** 行 */
    ROW,

    /** 普通按键 */
    KEY,

    /** 候选项 */
    CANDIDATE,

    /** 功能按钮 */
    BUTTON,

    /** 占位空白 */
    SPACER,

    /** 分隔节点 */
    DIVIDER,

    /** 分组 */
    GROUP,

    /** 自定义 */
    CUSTOM
}

/**
 * 线性布局中的对齐/分布方式。
 */
enum class Gravity {
    START,
    CENTER,
    END,
    SPACE_BETWEEN,
    SPACE_AROUND,
    SPACE_EVENLY,
    STRETCH
}

/**
 * 子项自身对齐方式。
 */
enum class AlignSelf {
    AUTO,
    STRETCH,
    START,
    CENTER,
    END
}

/**
 * 滚动方向。
 */
enum class ScrollDirection {
    HORIZONTAL,
    VERTICAL,
    BOTH
}

/**
 * 动作类型。
 */
enum class ActionType {
    /** 无动作 */
    NO_OP,

    /** 推入 token */
    PUSH_TOKEN,

    /** 提交一段文本 */
    COMMIT_TEXT,

    /** 提交候选 */
    COMMIT_CANDIDATE,

    /** 删除 */
    DELETE,

    /** 向前删除 */
    FORWARD_DELETE,

    /** 回车 */
    ENTER,

    /** 空格 */
    SPACE,

    /** 切换 layer */
    SWITCH_LAYER,

    /** 切换 mode */
    SWITCH_MODE,

    /** 切换 shift */
    TOGGLE_SHIFT,

    /** 打开面板 */
    OPEN_PANEL,

    /** 关闭面板 */
    CLOSE_PANEL,

    /** 下一页 */
    PAGE_NEXT,

    /** 上一页 */
    PAGE_PREV,

    /** 移动光标 */
    MOVE_CURSOR,

    /** 选中 */
    SELECT,

    /** 取消选中 */
    UNSELECT,

    /** 显示 popup */
    SHOW_POPUP,

    /** 隐藏 popup */
    HIDE_POPUP,

    /** 替换文本 */
    REPLACE_TEXT,

    /** 清空输入 */
    CLEAR_INPUT,

    /** 自定义动作 */
    CUSTOM
}

/**
 * 字重。
 */
enum class FontWeight {
    NORMAL,
    MEDIUM,
    SEMIBOLD,
    BOLD
}

/**
 * 修饰键类型。
 */
enum class KeyModifier {
    SHIFT,
    CTRL,
    ALT,
    FN,
    SYMBOL
}

/**
 * IME Action 类型。
 */
enum class ImeActionType {
    NONE,
    DONE,
    GO,
    NEXT,
    SEARCH,
    SEND
}

/**
 * 按钮角色。
 */
enum class ButtonRole {
    NORMAL,
    SHIFT,
    BACKSPACE,
    ENTER,
    SPACE,
    MODE_SWITCH,
    LAYER_SWITCH,
    PANEL_SWITCH,
    PAGE_NEXT,
    PAGE_PREV,
    CLOSE
}

/**
 * 光标移动方向。
 */
enum class CursorMoveDirection {
    LEFT,
    RIGHT,
    UP,
    DOWN,
    HOME,
    END
}