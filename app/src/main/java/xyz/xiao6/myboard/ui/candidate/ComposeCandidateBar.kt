package xyz.xiao6.myboard.ui.candidate

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.xiao6.myboard.core.keyboard.Candidate
import xyz.xiao6.myboard.core.keyboard.CandidateSource

@Composable
fun CandidateBar(
    candidates: List<Candidate>,
    selectedIndex: Int,
    onCandidateClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(Color.White)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            candidates.forEachIndexed { index, candidate ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (index == selectedIndex) Color(0xFF1A73E8).copy(alpha = 0.1f)
                            else Color.Transparent
                        )
                        .clickable { onCandidateClick(index) }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = candidate.text,
                        fontSize = 14.sp,
                        color = if (index == selectedIndex) Color(0xFF1A73E8) else Color(0xFF202124)
                    )
                }
            }
        }
    }
}
