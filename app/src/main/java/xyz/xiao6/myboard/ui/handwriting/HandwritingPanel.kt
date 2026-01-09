package xyz.xiao6.myboard.ui.handwriting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.xiao6.myboard.model.HandwritingLayoutMode
import xyz.xiao6.myboard.model.HandwritingPosition

/**
 * Full-screen or half-screen handwriting input panel
 * 全屏或半屏手写输入面板
 *
 * @param mode Layout mode (OVERLAY, HALF_SCREEN, FULL_SCREEN)
 * @param position Position for half-screen mode (TOP, BOTTOM)
 * @param onBack Callback when user wants to return to keyboard
 * @param onRecognize Callback to trigger recognition
 * @param onClear Callback to clear the canvas
 * @param modifier Modifier for the panel
 */
@Composable
fun HandwritingPanel(
    mode: HandwritingLayoutMode = HandwritingLayoutMode.HALF_SCREEN,
    position: HandwritingPosition = HandwritingPosition.BOTTOM,
    onBack: () -> Unit,
    onRecognize: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val candidates = remember { mutableStateListOf<String>() }
    var showCandidates by remember { mutableStateOf(true) }

    val panelModifier = when (mode) {
        HandwritingLayoutMode.FULL_SCREEN -> modifier.fillMaxSize()
        HandwritingLayoutMode.HALF_SCREEN -> modifier.then(
            if (position == HandwritingPosition.TOP) {
                Modifier.fillMaxWidth().fillMaxHeight(0.5f)
            } else {
                Modifier.fillMaxWidth().fillMaxHeight(1f)
            }
        )
        HandwritingLayoutMode.OVERLAY -> modifier
            .fillMaxWidth()
            .height(200.dp)
    }

    Box(
        modifier = panelModifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top bar with controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back to keyboard",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }

                Text(
                    text = when (mode) {
                        HandwritingLayoutMode.FULL_SCREEN -> "Full Screen Handwriting"
                        HandwritingLayoutMode.HALF_SCREEN -> "Handwriting Input"
                        HandwritingLayoutMode.OVERLAY -> "Quick Handwriting"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas for handwriting
            // In a real implementation, this would be an Android View with the HandwritingCanvasView
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium,
                    )
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Handwriting Canvas\n(Write here)",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Candidates display
            if (showCandidates && candidates.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    candidates.take(3).forEach { candidate ->
                        Button(
                            onClick = { /* Select candidate */ },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        ) {
                            Text(candidate, fontSize = 16.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { /* Show more options */ },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(),
                ) {
                    Text("Options")
                }

                Button(
                    onClick = onRecognize,
                    modifier = Modifier.weight(2f),
                ) {
                    Text("Recognize", fontSize = 16.sp)
                }
            }
        }
    }
}

/**
 * Compose wrapper for the HandwritingCanvasView
 * 在Compose中包装Android View的HandwritingCanvasView
 */
@Composable
fun HandwritingCanvasView(
    modifier: Modifier = Modifier,
    onStrokeCountChanged: ((Int) -> Unit)? = null,
) {
    // This will integrate with the Android View HandwritingOverlayView
    // For now, placeholder
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent),
    ) {
        Text("Canvas area")
    }
}
