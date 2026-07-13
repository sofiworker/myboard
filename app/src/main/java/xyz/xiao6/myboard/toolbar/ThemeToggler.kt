package xyz.xiao6.myboard.toolbar

import xyz.xiao6.myboard.data.repository.SettingsRepository
import xyz.xiao6.myboard.theme.foundation.AppearanceMode

class ThemeToggler(
    private val repo: SettingsRepository
) {
    suspend fun toggle() {
        val current = repo.getAppearanceSettings().foundation.appearanceMode
        val newMode = if (current == AppearanceMode.DARK) {
            AppearanceMode.LIGHT
        } else {
            AppearanceMode.DARK
        }
        repo.updateAppearanceMode(newMode)
    }

    suspend fun isDarkMode(): Boolean {
        return repo.getAppearanceSettings().foundation.appearanceMode == AppearanceMode.DARK
    }
}
