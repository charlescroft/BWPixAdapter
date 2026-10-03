package com.bwpixadapter.app.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

object ThemeManager {
    private const val PREF_NAME = "bwpix_theme"
    private const val KEY_MODE = "theme_mode"

    fun getThemeMode(context: Context): ThemeMode {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val name = sp.getString(KEY_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        return try {
            ThemeMode.valueOf(name)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODE, mode.name)
            .apply()
    }
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF0B5ED7),
    onPrimary = Color.White,
    secondary = Color(0xFF263238),
    background = Color(0xFFF5F6F8),
    surface = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6EA8FE),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFEEEEEE),
)

@Composable
fun BwPixTheme(themeMode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    MaterialTheme(
        colorScheme = if (isDark) DarkColors else LightColors,
        content = content,
    )
}
