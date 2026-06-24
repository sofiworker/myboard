package xyz.xiao6.myboard.toolbar

import xyz.xiao6.myboard.settings.SettingsManager
import xyz.xiao6.myboard.theme.BuiltInThemes
import xyz.xiao6.myboard.theme.ThemeResolverImpl

/**
 * 夜间模式切换器。
 * 协调 SettingsManager 和 ThemeResolver 的状态同步。
 */
class ThemeToggler(
    private val settingsManager: SettingsManager,
    private val themeResolver: ThemeResolverImpl
) {
    fun toggle() {
        settingsManager.toggleTheme()
        val newTheme = if (isDarkMode()) BuiltInThemes.dark else BuiltInThemes.light
        themeResolver.setTheme(newTheme)
    }

    fun isDarkMode(): Boolean {
        return settingsManager.themeMode == "dark"
    }
}
