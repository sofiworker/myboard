package xyz.xiao6.myboard.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Toolbar(
    onSettingsClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onEmojiClick: () -> Unit,
    onSymbolClick: () -> Unit,
    onClipboardClick: () -> Unit,
    onVoiceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(Color(0xFFF1F3F4))
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolbarButton(icon = Icons.Default.Language, contentDescription = "语言", onClick = onLanguageClick)
        ToolbarButton(icon = Icons.Default.EmojiEmotions, contentDescription = "Emoji", onClick = onEmojiClick)
        ToolbarButton(icon = Icons.Default.Star, contentDescription = "符号", onClick = onSymbolClick)
        ToolbarButton(icon = Icons.Default.ContentPaste, contentDescription = "剪贴板", onClick = onClipboardClick)
        ToolbarButton(icon = Icons.Default.Mic, contentDescription = "语音", onClick = onVoiceClick)
        ToolbarButton(icon = Icons.Default.Settings, contentDescription = "设置", onClick = onSettingsClick)
    }
}

@Composable
private fun ToolbarButton(
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
            tint = Color(0xFF5F6368),
            modifier = Modifier.size(18.dp)
        )
    }
}
