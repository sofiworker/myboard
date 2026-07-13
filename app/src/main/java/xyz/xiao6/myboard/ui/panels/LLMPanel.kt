package xyz.xiao6.myboard.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.xiao6.myboard.contract.theme.ChromeColors

/**
 * LLM 面板。
 */
@Composable
fun LLMPanel(
    chrome: ChromeColors,
    isProcessing: Boolean,
    result: String?,
    onTranslate: () -> Unit,
    onEnhance: () -> Unit,
    onAutocomplete: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(chrome.background)
            .padding(16.dp)
    ) {
        // 标题
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("AI 助手", color = chrome.candidateText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            IconButton(onClick = onClose) {
                Text("×", fontSize = 20.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 功能按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionButton("翻译", onClick = onTranslate, modifier = Modifier.weight(1f))
            ActionButton("美化", onClick = onEnhance, modifier = Modifier.weight(1f))
            ActionButton("补全", onClick = onAutocomplete, modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 结果显示
        if (isProcessing) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (result != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = chrome.surface)
            ) {
                Text(
                    text = result,
                    color = chrome.candidateText,
                    modifier = Modifier.padding(12.dp),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Text(text)
    }
}
