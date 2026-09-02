package com.marytwowheelers.spares

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.marytwowheelers.spares.ui.screens.MarySparesApp
import com.marytwowheelers.spares.ui.theme.MarySparesTheme
import com.marytwowheelers.spares.ui.theme.ThemeMode
import com.marytwowheelers.spares.ui.theme.ThemePreference
import com.marytwowheelers.spares.ui.viewmodels.AppViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Determine the effective initial theme immediately on startup
        val initialTheme = ThemePreference.getInitialThemeMode(applicationContext)
        val isSystemDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val initialIsDark = when (initialTheme) {
            ThemeMode.LIGHT  -> false
            ThemeMode.DARK   -> true
            ThemeMode.SYSTEM -> isSystemDark
        }

        // 2. Set matching window background instantly to prevent any flash before Compose draws
        window.setBackgroundDrawableResource(
            if (initialIsDark) R.color.dark_window_background else R.color.light_window_background
        )

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val viewModelFactory = AppViewModelFactory(applicationContext)

        setContent {
            val context = LocalContext.current
            val systemDark = isSystemInDarkTheme()
            // 3. Start state flow with the exact saved initial theme (no default SYSTEM override when user chose LIGHT/DARK)
            val themeMode by ThemePreference.getThemeMode(context).collectAsState(initial = initialTheme)

            val isDark = when (themeMode) {
                ThemeMode.LIGHT  -> false
                ThemeMode.DARK   -> true
                ThemeMode.SYSTEM -> systemDark
            }

            MarySparesTheme(darkTheme = isDark) {
                MarySparesApp(
                    viewModelFactory = viewModelFactory,
                    currentTheme     = themeMode
                )
            }
        }
    }
}