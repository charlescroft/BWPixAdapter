package com.bwpixadapter.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.bwpixadapter.app.ui.AppRoot
import com.bwpixadapter.app.ui.theme.BwPixTheme
import com.bwpixadapter.app.ui.theme.ThemeManager
import com.bwpixadapter.app.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var themeMode by remember { mutableStateOf(ThemeManager.getThemeMode(this)) }

            BwPixTheme(themeMode = themeMode) {
                AppRoot(
                    themeMode = themeMode,
                    onThemeModeChange = { newMode ->
                        themeMode = newMode
                        ThemeManager.setThemeMode(this, newMode)
                    },
                )
            }
        }
    }
}
