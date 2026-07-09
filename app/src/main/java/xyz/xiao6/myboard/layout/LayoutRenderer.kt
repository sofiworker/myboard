package xyz.xiao6.myboard.layout

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import xyz.xiao6.myboard.contract.input.*
import xyz.xiao6.myboard.contract.layout.*
import xyz.xiao6.myboard.contract.manifest.*
import xyz.xiao6.myboard.contract.theme.*
import xyz.xiao6.myboard.contract.engine.*
import xyz.xiao6.myboard.contract.bridge.*
import xyz.xiao6.myboard.contract.registry.*
import xyz.xiao6.myboard.contract.panel.*
import xyz.xiao6.myboard.contract.language.*
import xyz.xiao6.myboard.contract.state.*

/**
 * 新布局渲染器。
 * 消费 MeasuredLayout + ThemeResolver 进行渲染。
 */
@Composable
fun LayoutRenderer(
    measuredLayout: MeasuredLayout,
    context: KeyboardContext,
    themeResolver: ThemeResolver,
    onAction: (InputAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val androidContext = LocalContext.current
    val labelLookup = remember(androidContext) {
        { key: String ->
            val resId = androidContext.resources.getIdentifier(key, "string", androidContext.packageName)
            if (resId == 0) null else androidContext.getString(resId)
        }
    }
    var pressedKeyId by remember { mutableStateOf<String?>(null) }
    val actionDispatcher = remember { ActionDispatcher() }
    
    LaunchedEffect(pressedKeyId) {
        if (pressedKeyId != null) {
            delay(120)
            pressedKeyId = null
        }
    }
    
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(measuredLayout) {
                awaitPointerEventScope {
                    while (true) {
                        val downEvent = awaitPointerEvent(PointerEventPass.Main)
                        val down = downEvent.changes.firstOrNull() ?: continue
                        down.consume()
                        
                        val downPos = down.position
                        
                        val hitKey = findHitKey(downPos, measuredLayout)
                        if (hitKey == null) continue
                        
                        pressedKeyId = hitKey.key.id
                        
                        var flickUpTriggered = false
                        var released = false
                        
                        while (!released) {
                            val moveEvent = awaitPointerEvent(PointerEventPass.Main)
                            val moveChange = moveEvent.changes.firstOrNull() ?: continue
                            
                            val dy = moveChange.position.y - downPos.y
                            val dx = moveChange.position.x - downPos.x
                            
                            if (!flickUpTriggered && dy < -50f && kotlin.math.abs(dx) < kotlin.math.abs(dy) * 0.7f) {
                                flickUpTriggered = true
                                moveChange.consume()
                                val hint = hitKey.key.content.hint[HintPosition.TOP_CENTER]
                                    ?: hitKey.key.content.hint[HintPosition.BOTTOM_CENTER]
                                if (hint != null) {
                                    onAction(InputAction.PushToken(hint))
                                }
                            }
                            
                            if (moveChange.pressed) {
                                moveChange.consume()
                            } else {
                                released = true
                            }
                        }
                        
                        if (!flickUpTriggered) {
                            val action = actionDispatcher.dispatch(hitKey.key, GestureType.TAP, context)
                            onAction(action)
                        }
                        
                        pressedKeyId = null
                    }
                }
            }
    ) {
        drawMeasuredLayout(measuredLayout, themeResolver, textMeasurer, pressedKeyId, labelLookup)
    }
}

private fun findHitKey(pos: Offset, layout: MeasuredLayout): MeasuredKey? {
    for (key in layout.keys) {
        if (key.rect.contains(pos.x, pos.y)) {
            return key
        }
    }
    return null
}

private fun DrawScope.drawMeasuredLayout(
    layout: MeasuredLayout,
    themeResolver: ThemeResolver,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    pressedKeyId: String?,
    labelLookup: (String) -> String?
) {
    for (measuredKey in layout.keys) {
        val key = measuredKey.key
        val isPressed = key.id == pressedKeyId

        val styleRef = key.styleRef ?: "key_default"
        val style = themeResolver.resolveKeyStyle(styleRef)

        val bgColor = if (isPressed) style.pressedBackground else style.background

        val cornerRadius = style.cornerRadius

        drawRoundRect(
            color = bgColor,
            topLeft = Offset(measuredKey.rect.left, measuredKey.rect.top),
            size = Size(measuredKey.rect.width(), measuredKey.rect.height()),
            cornerRadius = CornerRadius(cornerRadius)
        )

        drawRoundRect(
            color = Color(0x1A000000),
            topLeft = Offset(measuredKey.rect.left, measuredKey.rect.top),
            size = Size(measuredKey.rect.width(), measuredKey.rect.height()),
            cornerRadius = CornerRadius(cornerRadius),
            style = Stroke(width = 0.5f)
        )

        val label = LayoutTextResolver.resolve(measuredKey.resolvedContent.label, labelLookup)
        if (label != null && label.isNotBlank()) {
            val textColor = if (isPressed) style.pressedTextColor
                            else style.textColor
            val fontSize = style.fontSize.sp
            val textStyle = TextStyle(color = textColor, fontSize = fontSize)
            val measured = textMeasurer.measure(label, textStyle)
            val x = measuredKey.rect.centerX() - measured.size.width / 2f
            val y = measuredKey.rect.centerY() - measured.size.height / 2f
            drawText(measured, topLeft = Offset(x, y))
        }

        for ((position, hintText) in measuredKey.resolvedContent.hint) {
            val resolvedHint = LayoutTextResolver.resolve(hintText, labelLookup)
            if (!resolvedHint.isNullOrBlank()) {
                val hintColor = themeResolver.resolveKeyStyle("key_default").iconTint
                val hintStyle = TextStyle(color = hintColor, fontSize = 9.sp)
                val hintMeasured = textMeasurer.measure(resolvedHint, hintStyle)

                val (hx, hy) = resolveHintPosition(position, measuredKey.rect, hintMeasured.size.width.toFloat(), hintMeasured.size.height.toFloat())
                drawText(hintMeasured, topLeft = Offset(hx, hy))
            }
        }
    }
}

private fun resolveHintPosition(
    position: HintPosition,
    rect: RectF,
    textW: Float,
    textH: Float
): Pair<Float, Float> {
    val margin = 3f
    return when (position) {
        HintPosition.TOP_LEFT -> Pair(rect.left + margin, rect.top + margin)
        HintPosition.TOP_CENTER -> Pair(rect.centerX() - textW / 2, rect.top + margin)
        HintPosition.TOP_RIGHT -> Pair(rect.right - textW - margin, rect.top + margin)
        HintPosition.CENTER_LEFT -> Pair(rect.left + margin, rect.centerY() - textH / 2)
        HintPosition.CENTER -> Pair(rect.centerX() - textW / 2, rect.centerY() - textH / 2)
        HintPosition.CENTER_RIGHT -> Pair(rect.right - textW - margin, rect.centerY() - textH / 2)
        HintPosition.BOTTOM_LEFT -> Pair(rect.left + margin, rect.bottom - textH - margin)
        HintPosition.BOTTOM_CENTER -> Pair(rect.centerX() - textW / 2, rect.bottom - textH - margin)
        HintPosition.BOTTOM_RIGHT -> Pair(rect.right - textW - margin, rect.bottom - textH - margin)
    }
}
