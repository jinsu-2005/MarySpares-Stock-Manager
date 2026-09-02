package com.marytwowheelers.spares.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary              = BrandPurple,
    onPrimary            = androidx.compose.ui.graphics.Color.White,
    primaryContainer     = BrandPurpleContainer,
    onPrimaryContainer   = BrandPurpleDim,
    secondary            = BrandGold,
    onSecondary          = OnBrandGold,
    secondaryContainer   = BrandGoldContainer,
    onSecondaryContainer = OnBrandGold,
    background           = LightBackground,
    onBackground         = LightOnSurface,
    surface              = LightSurface,
    onSurface            = LightOnSurface,
    surfaceVariant       = LightSurfaceVariant,
    onSurfaceVariant     = LightOnSurfaceVar,
    outline              = LightOutline,
    outlineVariant       = LightOutlineVariant,
    error                = StockOut,
    onError              = androidx.compose.ui.graphics.Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary              = BrandPurpleDim,
    onPrimary            = androidx.compose.ui.graphics.Color(0xFF2C0051),
    primaryContainer     = BrandPurpleDimContainer,
    onPrimaryContainer   = BrandPurpleDim,
    secondary            = BrandGoldDim,
    onSecondary          = OnBrandGold,
    secondaryContainer   = androidx.compose.ui.graphics.Color(0xFF3A2E00),
    onSecondaryContainer = BrandGoldDim,
    background           = DarkBackground,
    onBackground         = DarkOnSurface,
    surface              = DarkSurface,
    onSurface            = DarkOnSurface,
    surfaceVariant       = DarkSurfaceVariant,
    onSurfaceVariant     = DarkOnSurfaceVar,
    outline              = DarkOutline,
    outlineVariant       = DarkOutlineVariant,
    error                = StockOutDark,
    onError              = DarkBackground
)

@Composable
fun MarySparesTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT

                val insetsController = WindowCompat.getInsetsController(window, view)
                // In Light Mode: dark status bar icons and text for high contrast against light surfaces
                // In Dark Mode: light/white status bar icons and text for high contrast against dark surfaces
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}