package xyz.xiao6.myboard.toolbar

import xyz.xiao6.myboard.data.repository.SettingsRepository
import xyz.xiao6.myboard.theme.BuiltInThemes
import xyz.xiao6.myboard.theme.ThemeResolverImpl

/**
 * 夜间模式切换器。
 * 协调 SettingsRepository 和 ThemeResolver 的状态同步。
 */
class ThemeToggler(
    private val repo: SettingsRepository,
    private val themeResolver: ThemeResolverImpl
) {
    suspend fun toggle() {
        val current = repo.getSetting("theme_mode") ?: "auto"
        val newMode = if (current == "dark") "light" else "dark"
        repo.updateSetting("theme_mode", newMode)
        val newTheme = if (newMode == "dark") BuiltInThemes.dark else BuiltInThemes.light
        themeResolver.setTheme(newTheme)
    }

    suspend fun isDarkMode(): Boolean {
        return (repo.getSetting("theme_mode") ?: "auto") == "dark"
    }
}
