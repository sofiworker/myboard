package xyz.xiao6.myboard.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * STT 面板。
 */
@Composable
fun STTPanel(
    isListening: Boolean,
    partialText: String,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1F1F1F))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 标题
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("语音输入", color = Color.White, fontSize = 16.sp)
            IconButton(onClick = onClose) {
                Text("×", color = Color.White, fontSize = 20.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isListening) {
            // 波形动画占位
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(Color(0xFF2D2D2D), CircleShape)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 部分结果
            Text(
                text = partialText.ifEmpty { "正在聆听..." },
                color = Color.White,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 停止按钮
            IconButton(
                onClick = onStop,
                modifier = Modifier
                    .size(64.dp)
                    .background(Color.Red, CircleShape)
            ) {
                Icon(
                    Icons.Default.Stop,
                    contentDescription = "停止",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        } else {
            // 开始按钮
            IconButton(
                onClick = onStart,
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = "开始",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text("点击开始语音输入", color = Color.Gray, fontSize = 12.sp)
        }
    }
}
