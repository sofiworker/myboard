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
import xyz.xiao6.myboard.contract.theme.ChromeColors

/**
 * STT 面板。
 */
@Composable
fun STTPanel(
    chrome: ChromeColors,
    isListening: Boolean,
    partialText: String,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(chrome.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 标题
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("语音输入", color = chrome.candidateText, fontSize = 16.sp)
            IconButton(onClick = onClose) {
                Text("×", color = chrome.candidateText, fontSize = 20.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isListening) {
            // 波形动画占位
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(chrome.surface, CircleShape)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 部分结果
            Text(
                text = partialText.ifEmpty { "正在聆听..." },
                color = chrome.candidateText,
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
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(32.dp)
                )
            }
        } else {
            // 开始按钮
            IconButton(
                onClick = onStart,
                modifier = Modifier
                    .size(64.dp)
                    .background(chrome.candidateHighlight, CircleShape)
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = "开始",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text("点击开始语音输入", color = chrome.keyHint, fontSize = 12.sp)
        }
    }
}
