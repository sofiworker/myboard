package xyz.xiao6.myboard.ui

import android.os.Bundle
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import xyz.xiao6.myboard.R

/** 设置页（Launcher Activity）。 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnEnableIme = findViewById<Button>(R.id.btnEnableIme)
        val btnPickIme = findViewById<Button>(R.id.btnPickIme)
        val status = findViewById<TextView>(R.id.status)

        btnEnableIme.setOnClickListener {
            runCatching {
                startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            }.onFailure {
                Toast.makeText(this, "无法打开系统输入法设置", Toast.LENGTH_SHORT).show()
            }
        }

        btnPickIme.setOnClickListener {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
            if (imm == null) {
                Toast.makeText(this, "InputMethodManager 不可用", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            imm.showInputMethodPicker()
        }

        status.text = "状态：请在系统输入法设置中启用 MyBoard，然后在任意输入框切换到 MyBoard。"
    }
}
