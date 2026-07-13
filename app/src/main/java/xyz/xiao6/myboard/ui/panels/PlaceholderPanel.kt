package xyz.xiao6.myboard.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.xiao6.myboard.contract.state.PanelType
import xyz.xiao6.myboard.contract.theme.ChromeColors

/**
 * 占位面板。
 * 用于尚未实现的面板类型。
 *
 * @param onBack 返回上一层（回到键盘主界面），**不是**关闭输入法
 * @param onHideKeyboard 收起整个键盘
 */
@Composable
fun PlaceholderPanel(
    panelType: PanelType,
    chrome: ChromeColors,
    /** 返回键盘主界面（关闭面板），不是关闭输入法 */
    onBack: () -> Unit,
    /** 收起整个键盘 */
    onHideKeyboard: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(chrome.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                panelType.name,
                color = chrome.candidateText,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onBack, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, "返回键盘", modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onHideKeyboard, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.KeyboardHide, "收起键盘", modifier = Modifier.size(18.dp))
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "${panelType.name} 面板（开发中）",
                color = chrome.keyHint,
                fontSize = 12.sp
            )
        }
    }
}
