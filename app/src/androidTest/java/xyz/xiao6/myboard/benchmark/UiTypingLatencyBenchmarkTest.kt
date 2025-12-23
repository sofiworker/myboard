package xyz.xiao6.myboard.benchmark

import android.graphics.Rect
import android.graphics.RectF
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import xyz.xiao6.myboard.manager.LayoutManager
import xyz.xiao6.myboard.model.KeyboardLayout
import xyz.xiao6.myboard.model.RowAlignment
import xyz.xiao6.myboard.ui.BenchmarkInputActivity
import xyz.xiao6.myboard.util.DebugInputLatency
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class UiTypingLatencyBenchmarkTest {
    @Test
    fun uiTapLatencyDoesNotDropCharacters() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val device = UiDevice.getInstance(instrumentation)

        val scenario = ActivityScenario.launch(BenchmarkInputActivity::class.java)
        val editTextRef = arrayOfNulls<EditText>(1)
        scenario.onActivity { activity ->
            val input = activity.findViewById<EditText>(xyz.xiao6.myboard.R.id.benchmarkInputField)
            editTextRef[0] = input
            input.setText("")
            input.requestFocus()
            val imm = activity.getSystemService(InputMethodManager::class.java) ?: return@onActivity
            imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
        }

        val inputField =
            (device.wait(
                Until.findObject(By.res("xyz.xiao6.myboard:id/benchmarkInputField")),
                5000,
            ) as? UiObject2) ?: throw AssertionError("Benchmark input field not found.")
        inputField.click()
        device.waitForIdle()

        val keyboard =
            (device.wait(
                Until.findObject(By.res("xyz.xiao6.myboard:id/keyboardView")),
                500000000,
            ) as? UiObject2) ?: throw AssertionError("KeyboardView not found. Ensure MyBoard IME is enabled and selected.")

        val bounds = keyboard.visibleBounds
        val density = context.resources.displayMetrics.density
        val layout = LayoutManager(context).loadAllFromAssets().getLayout("qwerty")
        val rects = computeKeyRects(layout, bounds, density)

        val keyIdByLabel =
            layout.rows
                .flatMap { it.keys }
                .mapNotNull { key ->
                    val label = key.label?.trim()?.lowercase()
                    if (label.isNullOrBlank()) null else label to key.keyId
                }
                .toMap()

        val pressDurationsMs = listOf(5L, 10L, 20L, 50L)
        for (pressMs in pressDurationsMs) {
            val sequence = "qwertyuiopasdfghjklzxcvbnm"
            val repeatCount = 5
            val expected = sequence.repeat(repeatCount)
            val expectedLength = expected.length

            instrumentation.runOnMainSync {
                editTextRef[0]?.setText("")
            }

            val latencies = ArrayList<Long>(expectedLength)
            val clickToDown = ArrayList<Long>(expectedLength)
            val downToCommit = ArrayList<Long>(expectedLength)
            for (ch in expected) {
                val keyId = keyIdByLabel[ch.toString()]
                    ?: error("Missing key for label=$ch in qwerty layout.")
                val rect = rects[keyId] ?: error("Missing rect for keyId=$keyId.")
                val (tapX, tapY) = rectCenterToScreen(rect, bounds)

                val prevTouchSeq = DebugInputLatency.currentTouchSeq()
                val beforeLen = readLength(instrumentation, editTextRef[0])
                val start = SystemClock.uptimeMillis()
                sendTap(instrumentation, tapX.toFloat(), tapY.toFloat(), pressDurationMs = pressMs)

                val touchDeadline = SystemClock.uptimeMillis() + 500L
                var touchSeq = prevTouchSeq
                while (SystemClock.uptimeMillis() < touchDeadline) {
                    touchSeq = DebugInputLatency.currentTouchSeq()
                    if (touchSeq > prevTouchSeq) break
                    SystemClock.sleep(2)
                }
                if (touchSeq > prevTouchSeq) {
                    val downMs = DebugInputLatency.currentTouchDownMs()
                    clickToDown += (downMs - start).coerceAtLeast(0L)
                    val commitDeadline = SystemClock.uptimeMillis() + 800L
                    while (SystemClock.uptimeMillis() < commitDeadline) {
                        val commitSeq = DebugInputLatency.currentCommitSeq()
                        if (commitSeq >= touchSeq) break
                        SystemClock.sleep(2)
                    }
                    val commitMs = DebugInputLatency.currentCommitMs()
                    if (commitMs >= downMs) {
                        downToCommit += (commitMs - downMs).coerceAtLeast(0L)
                    }
                }

                val timeoutMs = 1000L
                val deadline = SystemClock.uptimeMillis() + timeoutMs
                var afterLen = beforeLen
                while (SystemClock.uptimeMillis() < deadline) {
                    afterLen = readLength(instrumentation, editTextRef[0])
                    if (afterLen > beforeLen) break
                    SystemClock.sleep(5)
                }
                if (afterLen <= beforeLen) {
                    fail("Input did not update within ${timeoutMs}ms at index=${latencies.size}.")
                }
                latencies += (SystemClock.uptimeMillis() - start)
            }

            val finalText = readText(instrumentation, editTextRef[0])
            assertEquals("Committed length mismatch.", expectedLength, finalText.length)
            assertEquals("Committed text mismatch.", expected, finalText)

            val sorted = latencies.sorted()
            val p50 = percentile(sorted, 50.0)
            val p90 = percentile(sorted, 90.0)
            val p95 = percentile(sorted, 95.0)
            val p99 = percentile(sorted, 99.0)
            val max = sorted.lastOrNull() ?: 0L

            android.util.Log.i(
                "UiTypingLatencyBenchmark",
                "press=${pressMs}ms len=$expectedLength p50=${p50}ms p90=${p90}ms p95=${p95}ms p99=${p99}ms max=${max}ms",
            )

            if (clickToDown.isNotEmpty()) {
                val s = clickToDown.sorted()
                val c50 = percentile(s, 50.0)
                val c95 = percentile(s, 95.0)
                val c99 = percentile(s, 99.0)
                android.util.Log.i(
                    "UiTypingLatencyBenchmark",
                    "press=${pressMs}ms clickToDown p50=${c50}ms p95=${c95}ms p99=${c99}ms",
                )
            }
            if (downToCommit.isNotEmpty()) {
                val s = downToCommit.sorted()
                val d50 = percentile(s, 50.0)
                val d95 = percentile(s, 95.0)
                val d99 = percentile(s, 99.0)
                android.util.Log.i(
                    "UiTypingLatencyBenchmark",
                    "press=${pressMs}ms downToCommit p50=${d50}ms p95=${d95}ms p99=${d99}ms",
                )
            }
        }
    }

    private fun readLength(
        instrumentation: android.app.Instrumentation,
        editText: EditText?,
    ): Int {
        var length = 0
        val target = editText ?: return 0
        instrumentation.runOnMainSync {
            length = target.text?.length ?: 0
        }
        return length
    }

    private fun readText(
        instrumentation: android.app.Instrumentation,
        editText: EditText?,
    ): String {
        var text = ""
        val target = editText ?: return ""
        instrumentation.runOnMainSync {
            text = target.text?.toString().orEmpty()
        }
        return text
    }

    private fun percentile(sorted: List<Long>, p: Double): Long {
        if (sorted.isEmpty()) return 0L
        val pos = (p / 100.0) * (sorted.size - 1)
        val lower = floor(pos).toInt()
        val upper = ceil(pos).toInt()
        if (lower == upper) return sorted[lower]
        val weight = pos - lower
        val low = sorted[lower]
        val high = sorted[upper]
        return (low + (high - low) * weight).roundToInt().toLong()
    }

    private fun rectCenterToScreen(rect: RectF, bounds: Rect): Pair<Int, Int> {
        val x = (bounds.left + rect.centerX()).roundToInt().coerceIn(bounds.left, bounds.right - 1)
        val y = (bounds.top + rect.centerY()).roundToInt().coerceIn(bounds.top, bounds.bottom - 1)
        return x to y
    }

    private fun computeKeyRects(
        layout: KeyboardLayout,
        bounds: Rect,
        density: Float,
    ): Map<String, RectF> {
        val padding = layout.defaults.padding
        val leftPx = dpToPx(padding.leftDp, density)
        val topPx = dpToPx(padding.topDp, density)
        val rightPx = dpToPx(padding.rightDp, density)
        val bottomPx = dpToPx(padding.bottomDp, density)
        val availableWidth = (bounds.width() - leftPx - rightPx).coerceAtLeast(0f)
        val availableHeight = (bounds.height() - topPx - bottomPx).coerceAtLeast(0f)

        val rowCount = layout.rows.size.coerceAtLeast(1)
        val verticalGapPx = dpToPx(layout.defaults.verticalGapDp, density)
        val totalGapHeight = verticalGapPx * (rowCount - 1)
        val keyboardContentHeight = (availableHeight - totalGapHeight).coerceAtLeast(0f)

        val rowTops = LinkedHashMap<String, Float>()
        val rowHeights = LinkedHashMap<String, Float>()

        var y = topPx
        layout.rows.forEach { row ->
            val rowHeight = (keyboardContentHeight * row.heightRatio) + dpToPx(row.heightDpOffset, density)
            rowTops[row.rowId] = y
            rowHeights[row.rowId] = rowHeight
            y += rowHeight + verticalGapPx
        }

        val result = LinkedHashMap<String, RectF>()
        layout.rows.forEach { row ->
            val rowTop = rowTops[row.rowId] ?: return@forEach
            val rowHeight = rowHeights[row.rowId] ?: return@forEach

            val rowGapPx = dpToPx(row.horizontalGapDp ?: layout.defaults.horizontalGapDp, density)
            val rowStartPaddingPx = dpToPx(row.startPaddingDp ?: padding.leftDp, density)
            val rowEndPaddingPx = dpToPx(row.endPaddingDp ?: padding.rightDp, density)

            val rowAvailableWidth =
                (availableWidth * row.widthRatio) + dpToPx(row.widthDpOffset, density) - rowStartPaddingPx - rowEndPaddingPx
            val keysInRow = row.keys.sortedBy { it.ui.gridPosition.startCol }
            if (keysInRow.isEmpty()) return@forEach

            val usesWeight = keysInRow.any { it.ui.widthWeight != 1f }
            val startX =
                when (row.alignment) {
                    RowAlignment.LEFT -> leftPx + rowStartPaddingPx
                    RowAlignment.CENTER -> {
                        if (usesWeight) {
                            leftPx + rowStartPaddingPx
                        } else {
                            val colCount =
                                keysInRow.maxOf {
                                    val gp = it.ui.gridPosition
                                    gp.startCol + gp.spanCols
                                }.coerceAtLeast(1)
                            val totalGaps = rowGapPx * (colCount - 1)
                            val cellWidth = ((rowAvailableWidth - totalGaps) / colCount).coerceAtLeast(0f)
                            val rowTotalWidth = (cellWidth * colCount) + totalGaps
                            leftPx + rowStartPaddingPx + ((rowAvailableWidth - rowTotalWidth) / 2f).coerceAtLeast(0f)
                        }
                    }
                    RowAlignment.JUSTIFY -> leftPx + rowStartPaddingPx
                }

            if (usesWeight) {
                val totalGaps = rowGapPx * (keysInRow.size - 1).coerceAtLeast(0)
                val available = (rowAvailableWidth - totalGaps).coerceAtLeast(0f)
                val totalWeight = keysInRow.sumOf { it.ui.widthWeight.toDouble() }.toFloat().coerceAtLeast(0.0001f)
                val unit = available / totalWeight
                var x = startX
                for (key in keysInRow) {
                    val w = unit * key.ui.widthWeight
                    result[key.keyId] = RectF(x, rowTop, x + w, rowTop + rowHeight)
                    x += w + rowGapPx
                }
            } else {
                val colCount =
                    keysInRow.maxOf {
                        val gp = it.ui.gridPosition
                        gp.startCol + gp.spanCols
                    }.coerceAtLeast(1)
                val totalGaps = rowGapPx * (colCount - 1)
                val cellWidth = ((rowAvailableWidth - totalGaps) / colCount).coerceAtLeast(0f)
                for (key in keysInRow) {
                    val gp = key.ui.gridPosition
                    val x = startX + gp.startCol * (cellWidth + rowGapPx)
                    val keyWidth = (cellWidth * gp.spanCols) + rowGapPx * (gp.spanCols - 1)
                    result[key.keyId] = RectF(x, rowTop, x + keyWidth, rowTop + rowHeight)
                }
            }
        }

        return result
    }

    private fun dpToPx(dp: Float, density: Float): Float = dp * density

    private fun sendTap(
        instrumentation: android.app.Instrumentation,
        x: Float,
        y: Float,
        pressDurationMs: Long,
    ) {
        val downTime = SystemClock.uptimeMillis()
        val down =
            MotionEvent.obtain(
                downTime,
                downTime,
                MotionEvent.ACTION_DOWN,
                x,
                y,
                0,
            ).apply { source = InputDevice.SOURCE_TOUCHSCREEN }
        instrumentation.sendPointerSync(down)
        down.recycle()

        val upTime = downTime + pressDurationMs
        val up =
            MotionEvent.obtain(
                downTime,
                upTime,
                MotionEvent.ACTION_UP,
                x,
                y,
                0,
            ).apply { source = InputDevice.SOURCE_TOUCHSCREEN }
        instrumentation.sendPointerSync(up)
        up.recycle()
    }
}
