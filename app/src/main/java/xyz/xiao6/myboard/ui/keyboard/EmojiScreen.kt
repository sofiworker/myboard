package xyz.xiao6.myboard.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch
import xyz.xiao6.myboard.data.model.EmojiData
import xyz.xiao6.myboard.ui.theme.LocalTheme

@OptIn(ExperimentalPagerApi::class)
@Composable
fun EmojiScreen(
    emojiData: EmojiData?,
    height: Dp,
    onEmojiClick: (String) -> Unit,
    onClose: () -> Unit
) {
    if (emojiData == null) return

    val pagerState = rememberPagerState()
    val scope = rememberCoroutineScope()
    val theme = LocalTheme.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
    ) {
        TabRow(selectedTabIndex = pagerState.currentPage) {
            emojiData.categories.forEachIndexed { index, category ->
                Tab(
                    text = { Text(category.name) },
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } }
                )
            }
        }

        HorizontalPager(
            count = emojiData.categories.size,
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 48.dp),
                modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(emojiData.categories[page].emojis) {
                Box(
                        modifier = Modifier.clickable { onEmojiClick(it) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = it, fontSize = 32.sp)
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(theme?.keyboardBackground?.color ?: MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                emojiData.categories.forEachIndexed { index, category ->
                    val isSelected = index == pagerState.currentPage
                    Text(
                        text = category.name,
                        color = if (isSelected) {
                            theme?.keyForeground?.color ?: MaterialTheme.colorScheme.primary
                        } else {
                            theme?.suggestionsForeground?.color ?: MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.clickable { scope.launch { pagerState.animateScrollToPage(index) } }
                    )
                }
            }
            OutlinedButton(
                onClick = onClose,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = theme?.keyForeground?.color ?: MaterialTheme.colorScheme.primary
                )
            ) {
                Text("ABC")
            }
        }
    }
}
