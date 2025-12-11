package xyz.xiao6.myboard.ui.keyboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.google.mlkit.vision.digitalink.recognition.Ink

@Composable
fun HandwritingPad(
    modifier: Modifier = Modifier,
    mode: String,
    onStrokeFinished: (Ink) -> Unit
) {
    val paths = remember { mutableStateListOf<Path>() }
    val currentStroke = remember { mutableStateListOf<Offset>() }

    val canvasModifier = when (mode) {
        "full" -> modifier.fillMaxSize()
        else -> modifier.fillMaxHeight(0.5f) // half
    }

    Box(modifier = canvasModifier.background(Color.White)) {
        Canvas(modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        currentStroke.add(offset)
                    },
                    onDrag = { change, _ ->
                        currentStroke.add(change.position)
                    },
                    onDragEnd = {
                        val inkBuilder = Ink.builder()
                        val strokeBuilder = Ink.Stroke.builder()
                        currentStroke.forEach { offset ->
                            strokeBuilder.addPoint(Ink.Point.create(offset.x, offset.y, System.currentTimeMillis()))
                        }
                        inkBuilder.addStroke(strokeBuilder.build())
                        onStrokeFinished(inkBuilder.build())

                        val newPath = Path()
                        if (currentStroke.isNotEmpty()) {
                            newPath.moveTo(currentStroke.first().x, currentStroke.first().y)
                            currentStroke.forEach { offset ->
                                newPath.lineTo(offset.x, offset.y)
                            }
                            paths.add(newPath)
                        }
                        currentStroke.clear()
                    }
                )
            }) {
            paths.forEach { path ->
                drawPath(
                    path = path,
                    color = Color.Black,
                    style = Stroke(width = 5f)
                )
            }
            if (currentStroke.isNotEmpty()) {
                val path = Path()
                path.moveTo(currentStroke.first().x, currentStroke.first().y)
                currentStroke.forEach { offset ->
                    path.lineTo(offset.x, offset.y)
                }
                drawPath(
                    path = path,
                    color = Color.Black,
                    style = Stroke(width = 5f)
                )
            }
        }
    }
}
