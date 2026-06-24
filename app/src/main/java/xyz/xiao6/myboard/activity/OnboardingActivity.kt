package xyz.xiao6.myboard.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.appintro.AppIntro
import com.github.appintro.AppIntroFragment

/**
 * 使用 AppIntro 的引导页面。
 */
class OnboardingActivity : AppIntro() {

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 欢迎页
        addSlide(AppIntroFragment.newInstance(
            title = "欢迎使用 MyBoard",
            description = "全球化智能输入法\n支持多语言、AI 联想、语音输入",
            imageDrawable = android.R.drawable.ic_menu_info_details,
            backgroundColor = android.graphics.Color.parseColor("#1A73E8")
        ))

        // 启用键盘
        addSlide(AppIntroFragment.newInstance(
            title = "启用键盘",
            description = "请在系统设置中启用 MyBoard 输入法\n设置 → 语言和输入法 → 屏幕键盘",
            imageDrawable = android.R.drawable.ic_menu_info_details,
            backgroundColor = android.graphics.Color.parseColor("#34A853")
        ))

        // 选择输入法
        addSlide(AppIntroFragment.newInstance(
            title = "选择输入法",
            description = "点击任意输入框\n在键盘选择器中选择 MyBoard",
            imageDrawable = android.R.drawable.ic_menu_info_details,
            backgroundColor = android.graphics.Color.parseColor("#FBBC05")
        ))

        // 完成
        addSlide(AppIntroFragment.newInstance(
            title = "完成！",
            description = "您已准备好使用 MyBoard\n开始输入吧！",
            imageDrawable = android.R.drawable.ic_menu_info_details,
            backgroundColor = android.graphics.Color.parseColor("#EA4335")
        ))

        isSkipButtonEnabled = true
    }

    override fun onSkipPressed(currentFragment: androidx.fragment.app.Fragment?) {
        finishOnboarding()
    }

    override fun onDonePressed(currentFragment: androidx.fragment.app.Fragment?) {
        finishOnboarding()
    }

    private fun finishOnboarding() {
        val settings = xyz.xiao6.myboard.settings.SettingsManager(this)
        settings.onboardingCompleted = true
        // 引导完成后直接进入设置页面
        startActivity(Intent(this, SettingsActivity::class.java))
        finish()
    }
}
