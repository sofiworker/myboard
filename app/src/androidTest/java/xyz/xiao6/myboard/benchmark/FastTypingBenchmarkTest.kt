package xyz.xiao6.myboard.benchmark

import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import xyz.xiao6.myboard.controller.InputMethodController
import xyz.xiao6.myboard.manager.LayoutManager
import xyz.xiao6.myboard.model.GestureType

@RunWith(AndroidJUnit4::class)
class FastTypingBenchmarkTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun fastTypingDoesNotDropCharacters() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext

        val layoutManager = LayoutManager(context).loadAllFromAssets()
        val controller = InputMethodController(layoutManager, scope)

        val committed = StringBuilder()
        val lock = Any()
        controller.onCommitText = { text ->
            synchronized(lock) {
                committed.append(text)
            }
        }

        instrumentation.runOnMainSync {
            controller.loadLayout("qwerty")
        }

        val layout = layoutManager.getLayout("qwerty")
        val keyIdByLabel =
            layout.rows
                .flatMap { it.keys }
                .mapNotNull { key ->
                    val label = key.label?.trim()?.lowercase()
                    if (label.isNullOrBlank()) null else label to key.keyId
                }
                .toMap()

        val sequence = "qwertyuiopasdfghjklzxcvbnm"
        val repeatCount = 200
        val expected = sequence.repeat(repeatCount)
        val expectedLength = expected.length

        val startMs = SystemClock.uptimeMillis()
        instrumentation.runOnMainSync {
            repeat(repeatCount) {
                for (ch in sequence) {
                    val keyId = keyIdByLabel[ch.toString()]
                        ?: error("Missing key for label=$ch in qwerty layout.")
                    controller.onKeyTriggered(keyId, GestureType.TAP)
                }
            }
        }

        val timeoutMs = 10_000L
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            val length = synchronized(lock) { committed.length }
            if (length >= expectedLength) break
            SystemClock.sleep(10)
        }

        val endMs = SystemClock.uptimeMillis()
        val result = synchronized(lock) { committed.toString() }
        val durationMs = (endMs - startMs).coerceAtLeast(1L)
        val cps = expectedLength * 1000.0 / durationMs
        Log.i("FastTypingBenchmark", "len=$expectedLength timeMs=$durationMs cps=%.2f".format(cps))

        assertEquals("Committed length mismatch.", expectedLength, result.length)
        assertEquals("Committed text mismatch.", expected, result)
    }
}
