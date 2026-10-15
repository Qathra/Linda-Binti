package com.example.lindabinti.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val CalculatorColorScheme = darkColorScheme(
    primary = CalcKeyOrange,
    onPrimary = CalcOnKey,
    secondary = CalcKeyLight,
    onSecondary = CalcOnLightKey,
    background = CalcBackground,
    onBackground = CalcOnKey,
    surface = CalcBackground,
    onSurface = CalcOnKey,
    surfaceVariant = CalcKeyDark,
    onSurfaceVariant = CalcOnKey.copy(alpha = 0.7f)
)

private val ProtectedColorScheme = lightColorScheme(
    primary = LindaPurple,
    onPrimary = LindaOnPurple,
    primaryContainer = LindaLavenderSoft,
    onPrimaryContainer = LindaPurpleDark,
    secondary = LindaPurpleMid,
    onSecondary = LindaOnPurple,
    secondaryContainer = LindaLavender,
    onSecondaryContainer = LindaPurpleDark,
    tertiary = LindaLavenderSoft,
    background = LindaLavender,
    onBackground = LindaText,
    surface = LindaSurface,
    onSurface = LindaText,
    surfaceVariant = LindaLavenderSoft,
    onSurfaceVariant = LindaMuted,
    outline = LindaStroke,
    outlineVariant = LindaCardBorder,
    error = LindaDanger
)

enum class AppThemeMode { Calculator, Protected }

@Composable
fun LINDABINTITheme(
    mode: AppThemeMode = AppThemeMode.Calculator,
    content: @Composable () -> Unit
) {
    val colorScheme = when (mode) {
        AppThemeMode.Calculator -> CalculatorColorScheme
        AppThemeMode.Protected -> ProtectedColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = when (mode) {
                AppThemeMode.Calculator -> CalcBackground.toArgb()
                AppThemeMode.Protected -> LindaPurpleDark.toArgb()
            }
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
