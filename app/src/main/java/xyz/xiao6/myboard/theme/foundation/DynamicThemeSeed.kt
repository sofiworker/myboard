package xyz.xiao6.myboard.theme.foundation

import android.os.Build
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext

object DynamicThemeSeed {
    @Composable
    fun currentSeedColor(): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        val scheme = dynamicLightColorScheme(LocalContext.current)
        val rgb = scheme.primary.toArgb().and(0x00FFFFFF)
        return "#${rgb.toString(16).padStart(6, '0').uppercase()}"
    }
}
