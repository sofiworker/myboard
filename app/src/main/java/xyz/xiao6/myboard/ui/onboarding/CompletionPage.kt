package xyz.xiao6.myboard.ui.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.xiao6.myboard.ui.settings.SettingsAccent

/**
 * 第 5 页：完成页。
 * 显示成功状态和"开始使用"按钮。
 */
@Composable
fun CompletionPage(
    isCompleting: Boolean,
    onComplete: () -> Unit
) {
    // 缩放动画
    val infiniteTransition = rememberInfiniteTransition(label = "scale")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale_anim"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(64.dp))

        // 成功图标（带缩放动画）
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier
                .size(96.dp)
                .scale(scale),
            tint = SettingsAccent.Green.color
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "一切就绪！",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "您已准备好使用 MyBoard",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "点击任意输入框即可切换输入法开始输入",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onComplete,
            enabled = !isCompleting,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (isCompleting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("开始使用", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
