package xyz.xiao6.myboard.ui.keyboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import xyz.xiao6.myboard.core.input.InputEngine
import xyz.xiao6.myboard.core.keyboard.InputAction
import xyz.xiao6.myboard.core.keyboard.KeyboardState
import xyz.xiao6.myboard.core.keyboard.ShiftState
import xyz.xiao6.myboard.core.layout.GridCalculator
import xyz.xiao6.myboard.core.layout.KeyGeometry
import xyz.xiao6.myboard.core.layout.KeyboardLayout
import xyz.xiao6.myboard.core.theme.BuiltInThemes
import xyz.xiao6.myboard.core.theme.ThemeResolver
import kotlin.math.abs

@Composable
fun ComposableInputView(
    layout: KeyboardLayout,
    state: KeyboardState,
    engine: InputEngine?,
    onAction: (InputAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val themeResolver = remember { ThemeResolver(BuiltInThemes.default) }
    val textMeasurer = rememberTextMeasurer()
    var pressedKeyId by remember { mutableStateOf<String?>(null) }
    var geometries by remember { mutableStateOf<Map<String, KeyGeometry>>(emptyMap()) }

    LaunchedEffect(pressedKeyId) {
        if (pressedKeyId != null) {
            delay(120)
            pressedKeyId = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(themeResolver.resolveKeyBackgroundColor(false)))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val downEvent = awaitPointerEvent(PointerEventPass.Main)
                            val down = downEvent.changes.firstOrNull() ?: continue
                            down.consume()

                            val downPos = down.position
                            val hitEntry = geometries.entries.find { (_, geo) ->
                                Rect(geo.leftPx, geo.topPx, geo.leftPx + geo.widthPx, geo.topPx + geo.heightPx).contains(downPos)
                            }

                            if (hitEntry == null) continue
                            val keyId = hitEntry.key
                            pressedKeyId = keyId

                            var flickUpTriggered = false
                            var released = false

                            while (!released) {
                                val moveEvent = awaitPointerEvent(PointerEventPass.Main)
                                val moveChange = moveEvent.changes.firstOrNull() ?: continue

                                val dy = moveChange.position.y - downPos.y
                                val dx = moveChange.position.x - downPos.x

                                if (!flickUpTriggered && dy < -50f && abs(dx) < abs(dy) * 0.7f) {
                                    flickUpTriggered = true
                                    moveChange.consume()
                                    val hint = getHintForKey(keyId, layout)
                                    if (hint != null) {
                                        onAction(InputAction.CommitText(hint))
                                    }
                                }

                                if (moveChange.pressed) {
                                    moveChange.consume()
                                } else {
                                    released = true
                                }
                            }

                            if (!flickUpTriggered) {
                                onAction(mapKeyToAction(keyId, state))
                            }

                            pressedKeyId = null
                        }
                    }
                }
        ) {
            geometries = GridCalculator().calculate(layout, size.width, size.height)
            drawKeyboard(layout, state, themeResolver, textMeasurer, pressedKeyId, geometries)
        }
    }
}

private fun DrawScope.drawKeyboard(
    layout: KeyboardLayout,
    state: KeyboardState,
    themeResolver: ThemeResolver,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    pressedKeyId: String?,
    geometries: Map<String, KeyGeometry>
) {
    for ((keyId, geo) in geometries) {
        val key = layout.keys[keyId] ?: continue
        val isPressed = keyId == pressedKeyId
        val rect = Rect(geo.leftPx, geo.topPx, geo.leftPx + geo.widthPx, geo.topPx + geo.heightPx)
        val cornerRadius = 6f

        val bgColor = if (isPressed) Color(themeResolver.resolveKeyBackgroundColor(true))
        else Color(themeResolver.resolveKeyBackgroundColor(false))
        drawRoundRect(
            color = bgColor,
            topLeft = Offset(rect.left, rect.top),
            size = Size(rect.width, rect.height),
            cornerRadius = CornerRadius(cornerRadius)
        )

        drawRoundRect(
            color = Color(0xFFDADCE0),
            topLeft = Offset(rect.left, rect.top),
            size = Size(rect.width, rect.height),
            cornerRadius = CornerRadius(cornerRadius),
            style = Stroke(width = 0.5f)
        )

        val displayText = key.icon ?: resolveLabel(key.label, state)
        if (displayText.isNotBlank()) {
            val fontSize = if (key.icon != null) 18.sp else 16.sp
            val textStyle = TextStyle(color = Color(themeResolver.resolveKeyTextColor()), fontSize = fontSize)
            val measured = textMeasurer.measure(displayText, textStyle)
            val x = rect.center.x - measured.size.width / 2f
            val y = rect.center.y - measured.size.height / 2f + 2f
            drawText(measured, topLeft = Offset(x, y))
        }

        val hint = key.hint
        if (hint != null && key.icon == null) {
            val hintTextStyle = TextStyle(color = Color(0xFF9AA0A6), fontSize = 9.sp)
            val hintMeasured = textMeasurer.measure(hint, hintTextStyle)
            drawText(hintMeasured, topLeft = Offset(rect.right - hintMeasured.size.width - 3f, rect.top + 2f))
        }
    }
}

private fun resolveLabel(label: String?, state: KeyboardState): String {
    val base = label ?: ""
    if (base.isBlank() || base.length > 2) return base
    return when (state.shiftState) {
        ShiftState.OFF -> base.lowercase()
        ShiftState.ON, ShiftState.CAPS_LOCK -> base.uppercase()
    }
}

private fun mapKeyToAction(keyId: String, state: KeyboardState): InputAction {
    return when (keyId) {
        "shift" -> InputAction.ToggleShift
        "backspace" -> InputAction.Delete(1)
        "space" -> InputAction.CommitText(" ")
        "enter" -> InputAction.PerformEditorAction("done")
        "comma" -> InputAction.CommitText(",")
        "period" -> InputAction.CommitText(".")
        "sym" -> InputAction.SwitchArrangement("symbols")
        "lang" -> InputAction.SwitchLanguage(if (state.languageId == "en_us") "zh_cn" else "en_us")
        else -> {
            if (keyId.length == 1 && keyId[0].isLetter()) {
                val char = when (state.shiftState) {
                    ShiftState.OFF -> keyId.lowercase()
                    ShiftState.ON, ShiftState.CAPS_LOCK -> keyId.uppercase()
                }
                InputAction.CommitText(char)
            } else {
                InputAction.CommitText("")
            }
        }
    }
}

fun getHintForKey(keyId: String, layout: KeyboardLayout): String? {
    return layout.keys[keyId]?.hint
}
