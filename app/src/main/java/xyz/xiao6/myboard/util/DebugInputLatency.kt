package xyz.xiao6.myboard.util

import java.util.concurrent.atomic.AtomicLong

object DebugInputLatency {
    private val touchSeq = AtomicLong(0L)
    private val lastTouchDownMs = AtomicLong(0L)
    private val lastCommitSeq = AtomicLong(0L)
    private val lastCommitMs = AtomicLong(0L)

    fun markTouchDown(nowMs: Long): Long {
        lastTouchDownMs.set(nowMs)
        return touchSeq.incrementAndGet()
    }

    fun markCommit(nowMs: Long) {
        lastCommitMs.set(nowMs)
        lastCommitSeq.set(touchSeq.get())
    }

    fun currentTouchSeq(): Long = touchSeq.get()
    fun currentTouchDownMs(): Long = lastTouchDownMs.get()
    fun currentCommitSeq(): Long = lastCommitSeq.get()
    fun currentCommitMs(): Long = lastCommitMs.get()
}
