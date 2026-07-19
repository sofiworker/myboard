package xyz.xiao6.myboard.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * HorizontalPager 页面指示器。
 * 选中项为胶囊长条，未选中为圆点，宽高与颜色均有过渡动画。
 */
@Composable
fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            val width by animateDpAsState(
                targetValue = if (isSelected) 22.dp else 8.dp,
                animationSpec = tween(300),
                label = "dot_width"
            )
            val color by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
                animationSpec = tween(300),
                label = "dot_color"
            )
            Box(
                modifier = Modifier
                    .width(width)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}
