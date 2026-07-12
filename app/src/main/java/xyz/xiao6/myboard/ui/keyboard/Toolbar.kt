package xyz.xiao6.myboard.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import xyz.xiao6.myboard.R
import xyz.xiao6.myboard.contract.state.PanelType
import xyz.xiao6.myboard.contract.theme.ChromeColors
import xyz.xiao6.myboard.data.entity.ToolbarItemEntity
import xyz.xiao6.myboard.data.entity.ToolbarItemType
import xyz.xiao6.myboard.data.entity.ToolbarLayoutMode

/**
 * 三区布局 Toolbar：左侧固定 ⚙ / 中间可配置按钮 / 右侧固定 ↓ 收起键盘。
 */
@Composable
fun Toolbar(
    items: List<ToolbarItemEntity>,
    layoutMode: ToolbarLayoutMode,
    isDark: Boolean,
    chrome: ChromeColors,
    onSettingsClick: () -> Unit,
    onHideKeyboard: () -> Unit,
    onThemeToggle: () -> Unit,
    onPanelOpen: (PanelType) -> Unit,
    modifier: Modifier = Modifier
) {
    val iconTint = if (isDark) {
        chrome.candidateText.copy(alpha = 0.85f)
    } else {
        chrome.candidateText.copy(alpha = 0.72f)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(chrome.surface)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolbarIconButton(
            icon = Icons.Default.Settings,
            contentDescription = stringResource(R.string.toolbar_settings),
            tint = iconTint,
            onClick = onSettingsClick
        )

        when (layoutMode) {
            ToolbarLayoutMode.SCROLLABLE -> {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(items.size) { index ->
                        ToolbarMiddleButton(
                            type = ToolbarItemType.valueOf(items[index].type),
                            isDark = isDark,
                            tint = iconTint,
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
                            tint = iconTint,
                            onThemeToggle = onThemeToggle,
                            onPanelOpen = onPanelOpen
                        )
                    }
                }
            }
        }

        ToolbarIconButton(
            icon = Icons.Default.KeyboardHide,
            contentDescription = stringResource(R.string.toolbar_hide_keyboard),
            tint = iconTint,
            onClick = onHideKeyboard
        )
    }
}

@Composable
private fun ToolbarMiddleButton(
    type: ToolbarItemType,
    isDark: Boolean,
    tint: Color,
    onThemeToggle: () -> Unit,
    onPanelOpen: (PanelType) -> Unit
) {
    val (icon, desc) = when (type) {
        ToolbarItemType.LOCALE_SWITCH -> Icons.Default.Language to stringResource(R.string.toolbar_locale_switch)
        ToolbarItemType.THEME_TOGGLE ->
            (if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode) to
                stringResource(R.string.toolbar_theme_toggle)
        ToolbarItemType.EMOJI -> Icons.Default.EmojiEmotions to stringResource(R.string.toolbar_emoji)
        ToolbarItemType.SYMBOL -> Icons.Default.Star to stringResource(R.string.toolbar_symbol)
        ToolbarItemType.CLIPBOARD -> Icons.Default.ContentPaste to stringResource(R.string.toolbar_clipboard)
        ToolbarItemType.LAYOUT_SWITCH -> Icons.Default.Keyboard to stringResource(R.string.toolbar_layout_switch)
        ToolbarItemType.VOICE_INPUT -> Icons.Default.Mic to stringResource(R.string.toolbar_voice_input)
    }

    ToolbarIconButton(
        icon = icon,
        contentDescription = desc,
        tint = tint,
        onClick = {
            when (type) {
                ToolbarItemType.LOCALE_SWITCH -> onPanelOpen(PanelType.LOCALE_SWITCH)
                ToolbarItemType.THEME_TOGGLE -> onThemeToggle()
                ToolbarItemType.EMOJI -> onPanelOpen(PanelType.EMOJI)
                ToolbarItemType.SYMBOL -> onPanelOpen(PanelType.SYMBOL)
                ToolbarItemType.CLIPBOARD -> onPanelOpen(PanelType.CLIPBOARD)
                ToolbarItemType.LAYOUT_SWITCH -> onPanelOpen(PanelType.LAYOUT_SWITCH)
                ToolbarItemType.VOICE_INPUT -> { /* reserved */ }
            }
        }
    )
}

@Composable
private fun ToolbarIconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}
