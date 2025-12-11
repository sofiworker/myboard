package xyz.xiao6.myboard.util

import android.content.Context
import android.provider.Settings
import android.view.inputmethod.InputMethodManager

fun isImeEnabled(context: Context): Boolean {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    return imm.enabledInputMethodList.any { it.packageName == context.packageName }
}

fun isImeSelected(context: Context): Boolean {
    val currentIme = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
    return currentIme?.startsWith(context.packageName) ?: false
}
