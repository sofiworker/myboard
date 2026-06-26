package xyz.xiao6.myboard.ui.keyboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import xyz.xiao6.myboard.R
import xyz.xiao6.myboard.contract.state.PanelType
import xyz.xiao6.myboard.data.entity.ToolbarItemEntity
import xyz.xiao6.myboard.data.entity.ToolbarLayoutMode
import xyz.xiao6.myboard.data.entity.ToolbarItemType

/**
 * 三区布局 Toolbar：左侧固定 ⚙ / 中间可配置按钮 / 右侧固定 ↓ 收起键盘。
 *
 * @param items 已启用且按 sortOrder 排序的工具栏按钮列表
 * @param layoutMode 中间区布局模式：SCROLLABLE 横向滚动 / FIXED 固定均分
 * @param isDark 当前是否为暗色模式
 * @param onSettingsClick 跳转设置回调
 * @param onHideKeyboard 收起键盘回调
 * @param onThemeToggle 主题切换回调
 * @param onPanelOpen 打开面板回调
 */
@Composable
fun Toolbar(
    items: List<ToolbarItemEntity>,
    layoutMode: ToolbarLayoutMode,
    isDark: Boolean,
    onSettingsClick: () -> Unit,
    onHideKeyboard: () -> Unit,
    onThemeToggle: () -> Unit,
    onPanelOpen: (PanelType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ===== 左侧固定：设置 =====
        ToolbarIconButton(
            icon = Icons.Default.Settings,
            contentDescription = stringResource(R.string.toolbar_settings),
            onClick = onSettingsClick
        )

        // ===== 中间区：可配置按钮 =====
        when (layoutMode) {
            ToolbarLayoutMode.SCROLLABLE -> {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(items.size) { index ->
                        ToolbarMiddleButton(
                            type = ToolbarItemType.valueOf(items[index].type),
                            isDark = isDark,
                            onThemeToggle = onThemeToggle,
                            onPanelOpen = onPanelOpen
                        )
                    }
                }
            }
            ToolbarLayoutMode.FIXED -> {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    items.forEach { item ->
                        ToolbarMiddleButton(
                            type = ToolbarItemType.valueOf(item.type),
                            isDark = isDark,
                            onThemeToggle = onThemeToggle,
                            onPanelOpen = onPanelOpen
                        )
                    }
                }
            }
        }

        // ===== 右侧固定：收起键盘 =====
        ToolbarIconButton(
            icon = Icons.Default.KeyboardHide,
            contentDescription = stringResource(R.string.toolbar_hide_keyboard),
            onClick = onHideKeyboard
        )
    }
}

@Composable
private fun ToolbarMiddleButton(
    type: ToolbarItemType,
    isDark: Boolean,
    onThemeToggle: () -> Unit,
    onPanelOpen: (PanelType) -> Unit
) {
    val (icon, desc) = when (type) {
        ToolbarItemType.LOCALE_SWITCH -> Icons.Default.Language to stringResource(R.string.toolbar_locale_switch)
        ToolbarItemType.THEME_TOGGLE -> (if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode) to stringResource(R.string.toolbar_theme_toggle)
        ToolbarItemType.EMOJI -> Icons.Default.EmojiEmotions to stringResource(R.string.toolbar_emoji)
        ToolbarItemType.SYMBOL -> Icons.Default.Star to stringResource(R.string.toolbar_symbol)
        ToolbarItemType.CLIPBOARD -> Icons.Default.ContentPaste to stringResource(R.string.toolbar_clipboard)
        ToolbarItemType.LAYOUT_SWITCH -> Icons.Default.Keyboard to stringResource(R.string.toolbar_layout_switch)
        ToolbarItemType.VOICE_INPUT -> Icons.Default.Mic to stringResource(R.string.toolbar_voice_input)
    }

    ToolbarIconButton(
        icon = icon,
        contentDescription = desc,
        onClick = {
            when (type) {
                ToolbarItemType.LOCALE_SWITCH -> {
                    onPanelOpen(PanelType.LOCALE_SWITCH)
                }
                ToolbarItemType.THEME_TOGGLE -> onThemeToggle()
                ToolbarItemType.EMOJI -> onPanelOpen(PanelType.EMOJI)
                ToolbarItemType.SYMBOL -> onPanelOpen(PanelType.SYMBOL)
                ToolbarItemType.CLIPBOARD -> onPanelOpen(PanelType.CLIPBOARD)
                ToolbarItemType.LAYOUT_SWITCH -> {
                    onPanelOpen(PanelType.LAYOUT_SWITCH)
                }
                ToolbarItemType.VOICE_INPUT -> { /* 预留 */ }
            }
        }
    )
}

@Composable
private fun ToolbarIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(32.dp)
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}
