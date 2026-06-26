package xyz.xiao6.myboard.ui.onboarding

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 第 2 页：启用 MyBoard 输入法。
 * 引导用户到系统设置启用 IME，并自动检测启用状态。
 * 用户可手动跳过此步骤进入下一页。
 */
@Composable
fun ImeEnablePage(
    isImeEnabled: Boolean,
    isChecking: Boolean,
    onRefreshCheck: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // 图标
        Surface(
            modifier = Modifier.size(80.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Keyboard,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = if (isImeEnabled) "输入法已启用 ✓" else "启用 MyBoard 输入法",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isImeEnabled) "已检测到 MyBoard 输入法，即将进入下一步..."
                   else "请在系统设置中启用 MyBoard 输入法，然后点击下方按钮检测。",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (!isImeEnabled) {
            // 打开设置按钮
            Button(
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("打开输入法设置", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 刷新检测按钮
            OutlinedButton(
                onClick = onRefreshCheck,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isChecking
            ) {
                if (isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    if (isChecking) "检测中..." else "已启用，检查状态",
                    fontSize = 16.sp
                )
            }
        } else {
            // 已启用，显示加载状态
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "提示：启用后无需重新打开此页面，系统会自动检测到",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        // "下一步" 按钮 — 即使 IME 未启用也可手动跳过
        Button(
            onClick = onSkip,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f)
            )
        ) {
            Text("下一步", fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "若未启用 MyBoard，后续可在设置中重新配置",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}
