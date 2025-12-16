package xyz.xiao6.myboard.util

import android.util.Log

object MLog {
    private const val PREFIX = "MyBoard"

    // Keep logging on by default (BuildConfig may be disabled in some AGP setups).
    val enabled: Boolean = true

    fun d(tag: String, msg: String) {
        if (!enabled) return
        Log.i(PREFIX, "[$tag] $msg")
    }

    fun w(tag: String, msg: String, tr: Throwable? = null) {
        if (!enabled) return
        if (tr == null) Log.w(PREFIX, "[$tag] $msg") else Log.w(PREFIX, "[$tag] $msg", tr)
    }

    fun e(tag: String, msg: String, tr: Throwable? = null) {
        if (!enabled) return
        if (tr == null) Log.e(PREFIX, "[$tag] $msg") else Log.e(PREFIX, "[$tag] $msg", tr)
    }
}
