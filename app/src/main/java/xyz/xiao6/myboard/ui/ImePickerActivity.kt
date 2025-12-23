package xyz.xiao6.myboard.ui

import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService

class ImePickerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val imm = getSystemService<InputMethodManager>()
        imm?.showInputMethodPicker()
        finish()
    }
}
