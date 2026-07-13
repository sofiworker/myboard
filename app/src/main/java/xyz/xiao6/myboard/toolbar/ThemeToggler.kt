package xyz.xiao6.myboard.toolbar

import xyz.xiao6.myboard.data.repository.SettingsRepository
import xyz.xiao6.myboard.theme.foundation.AppearanceMode
import xyz.xiao6.myboard.theme.foundation.ThemeVariant

class ThemeToggler(
    private val repo: SettingsRepository
) {
    suspend fun toggle(currentVariant: ThemeVariant) {
        val current = repo.getAppearanceSettings().foundation.appearanceMode
        val effectiveDark = when (current) {
            AppearanceMode.DARK -> true
            AppearanceMode.LIGHT -> false
            AppearanceMode.FOLLOW_SYSTEM -> currentVariant == ThemeVariant.DARK
        }
        val newMode = if (effectiveDark) AppearanceMode.LIGHT else AppearanceMode.DARK
        repo.updateAppearanceMode(newMode)
    }

    suspend fun isDarkMode(): Boolean {
        return repo.getAppearanceSettings().foundation.appearanceMode == AppearanceMode.DARK
    }
}
