package com.marytwowheelers.spares.ui.theme

import androidx.compose.ui.graphics.Color

// =============================================
// BRAND PALETTE
// =============================================
val BrandPurple = Color(0xFF34005F)         // Deep Royal Purple (primary light)
val BrandPurpleContainer = Color(0xFF4B1E78) // Mid-purple container
val BrandPurpleDim = Color(0xFFDCB8FF)       // Light purple on dark
val BrandPurpleDimContainer = Color(0xFF59318A) // purple container on dark

val BrandGold = Color(0xFFFFD400)            // Golden Yellow
val BrandGoldContainer = Color(0xFFFED400)
val BrandGoldDim = Color(0xFFEAC300)
val OnBrandGold = Color(0xFF3A2E00)          // Dark text on gold

// =============================================
// LIGHT SCHEME TOKENS
// =============================================
val LightBackground    = Color(0xFFF7F7F8)
val LightSurface       = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF1EFF6)
val LightSurfaceLow    = Color(0xFFF4F3F8)
val LightSurfaceHigh   = Color(0xFFE8E5F0)
val LightOutline       = Color(0xFFCDC3D4)
val LightOutlineVariant = Color(0xFFE5E0EC)
val LightOnSurface     = Color(0xFF1A1626)
val LightOnSurfaceVar  = Color(0xFF6A6478)

// =============================================
// DARK SCHEME TOKENS (Refined for AMOLED/Charcoal Dark)
// =============================================
val DarkBackground     = Color(0xFF181A22)
val DarkSurface        = Color(0xFF222530)
val DarkSurfaceVariant = Color(0xFF1E212B)
val DarkSurfaceLow     = Color(0xFF1A1D27)
val DarkSurfaceHigh    = Color(0xFF2B2F3D)
val DarkOutline        = Color(0xFF454C62)
val DarkOutlineVariant = Color(0xFF323748)
val DarkOnSurface      = Color(0xFFF3F4F6)
val DarkOnSurfaceVar   = Color(0xFF9CA3AF)

// Floating Modals & Action Popups
val DarkPopupSurface   = Color(0xFF2C3142) // Elevated lighter slate surface for modal dialogs & action popups
val DarkPopupBorder    = Color(0xFF424A63) // Crisp border for floating action popups
val DarkPopupPill      = Color(0xFF373E54) // Inner elevated containers inside popups

// =============================================
// SEMANTIC STOCK STATUS (same in both themes)
// =============================================
val StockHealthy       = Color(0xFF10B981)
val StockHealthyBg     = Color(0xFFDCFCE7)
val StockLow           = Color(0xFFF59E0B)
val StockLowBg         = Color(0xFFFEF3C7)
val StockOut           = Color(0xFFEF4444)
val StockOutBg         = Color(0xFFFEE2E2)

val StockHealthyDark   = Color(0xFF34D399)
val StockHealthyBgDark = Color(0xFF0D382B)
val StockLowDark       = Color(0xFFFFB726) // Radiant golden amber (crisp, warm, high-contrast)
val StockLowBgDark     = Color(0xFF382914) // Rich dark warm amber surface (no mud)
val StockOutDark       = Color(0xFFFF4D6D) // Vibrant neon crimson-red (no washed out pink)
val StockOutBgDark     = Color(0xFF3E1824) // Deep refined ruby-charcoal surface
val RedActionDark      = Color(0xFFF43F5E) // High-contrast punchy red for action buttons

// =============================================
// LEGACY ALIASES (for AuthScreen compat)
// =============================================
val PurplePrimary      = BrandPurple
val YellowAccent       = BrandGold
val SurfaceContainerLowest = LightSurface
val SurfaceContainerLow = LightSurfaceLow
val SurfaceContainerHigh = LightSurfaceHigh
val SurfaceContainer   = LightSurfaceVariant
val BorderSubtle       = LightOutlineVariant
val OutlineVariant     = LightOutline
val OnYellowContainer  = OnBrandGold
val YellowContainer    = BrandGoldContainer
val PurpleContainer    = BrandPurpleContainer