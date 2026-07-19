package xyz.xiao6.myboard.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xyz.xiao6.myboard.R

private val GroupShape = RoundedCornerShape(16.dp)
private val IconBubbleShape = RoundedCornerShape(10.dp)

/**
 * 设置项图标的彩色 accent（iOS 设置风格）。
 * [light] 用于浅色模式，[dark] 用于深色模式（更亮以保证对比度）。
 * 气泡背景为 accent 色的低透明度版本，深浅色模式下均有良好表现。
 */
enum class SettingsAccent(private val light: Color, private val dark: Color) {
    Blue(Color(0xFF1A73E8), Color(0xFF8AB4F8)),
    Green(Color(0xFF188038), Color(0xFF81C995)),
    Purple(Color(0xFF9334E6), Color(0xFFC58AF9)),
    Orange(Color(0xFFE8710A), Color(0xFFFCAD70)),
    Teal(Color(0xFF009688), Color(0xFF78D9EC)),
    Pink(Color(0xFFD01884), Color(0xFFFF8BCB)),
    Indigo(Color(0xFF3F51B5), Color(0xFF9AA7FF)),
    Amber(Color(0xFFF9AB00), Color(0xFFFDD663)),
    Gray(Color(0xFF5F6368), Color(0xFF9AA0A6));

    /** 当前主题下的 accent 颜色。 */
    val color: Color
        @Composable get() = if (isSystemInDarkTheme()) dark else light

    /** accent 色对应的柔和气泡底色。 */
    val containerColor: Color
        @Composable get() = color.copy(alpha = if (isSystemInDarkTheme()) 0.24f else 0.13f)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScaffold(
    title: String,
    onBack: () -> Unit,
    actions: @Composable (() -> Unit)? = null,
    content: @Composable (padding: androidx.compose.foundation.layout.PaddingValues) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }
                },
                actions = { actions?.invoke() },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        content = content
    )
}

@Composable
fun SettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(start = 4.dp, top = 18.dp, bottom = 8.dp, end = 4.dp)
    )
}

@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = GroupShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        // 裁剪子内容到圆角，避免首尾项的点击涟漪溢出卡片圆角
        Column(modifier = Modifier.clip(GroupShape), content = content)
    }
}

@Composable
fun SettingsGroupDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 58.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    )
}

@Composable
fun SettingsNavItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    onClick: (() -> Unit)? = null,
    showDivider: Boolean = false,
    accent: SettingsAccent? = null
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsIconBubble(icon = icon, accent = accent)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (onClick != null) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        if (showDivider) SettingsGroupDivider()
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showDivider: Boolean = false,
    accent: SettingsAccent? = null
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                SettingsIconBubble(icon = icon, accent = accent)
                Spacer(modifier = Modifier.width(14.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = if (icon != null) 58.dp else 14.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
            )
        }
    }
}

@Composable
fun SettingsSliderItem(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    showDivider: Boolean = false,
    icon: ImageVector? = null,
    accent: SettingsAccent? = null
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                SettingsIconBubble(icon = icon, accent = accent)
                Spacer(modifier = Modifier.width(14.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = valueLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
                Slider(
                    value = value,
                    onValueChange = onValueChange,
                    valueRange = valueRange,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = if (icon != null) 58.dp else 14.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
            )
        }
    }
}

@Composable
fun SettingsIconBubble(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    accent: SettingsAccent? = null
) {
    val container: Color
    val content: Color
    if (accent != null) {
        container = accent.containerColor
        content = accent.color
    } else {
        container = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        content = MaterialTheme.colorScheme.primary
    }
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(IconBubbleShape)
            .background(container),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun SettingsAvatar(
    text: String,
    selected: Boolean = false,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val fg = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.take(1).uppercase(),
            style = MaterialTheme.typography.titleMedium,
            color = fg,
            fontWeight = FontWeight.Bold
        )
    }
}

// Backward-compatible aliases used by older screens
@Composable
fun SectionHeader(title: String) = SettingsSectionHeader(title)

@Composable
fun NavigationItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    onClick: () -> Unit
) {
    SettingsGroup {
        SettingsNavItem(
            title = title,
            subtitle = subtitle,
            icon = icon,
            onClick = onClick
        )
    }
}

@Composable
fun SwitchItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingsGroup {
        SettingsSwitchItem(
            title = title,
            subtitle = subtitle,
            icon = icon,
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun SliderItem(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    suffix: String
) {
    SettingsGroup {
        SettingsSliderItem(
            title = title,
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            valueLabel = "${value.toInt()}$suffix"
        )
    }
}
