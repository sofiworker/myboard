package xyz.xiao6.myboard.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xyz.xiao6.myboard.ui.theme.LocalTheme

@Composable
fun CandidateView(
    suggestions: List<String>,
    onCandidateClick: (String) -> Unit
) {
    val theme = LocalTheme.current ?: return

    if (suggestions.isEmpty()) {
        Spacer(modifier = Modifier.height(48.dp)) // Maintain height even when empty
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(theme.suggestionsBackground.toColor())
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(suggestions) {
                Text(
                    text = it,
                    color = theme.suggestionsForeground.toColor(),
                    modifier = Modifier.clickable { onCandidateClick(it) }
                )
            }
        }
    }
}
