package com.coolappstore.everhaptics.by.svhp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.coolappstore.everhaptics.by.svhp.data.ThemeMode

private val HapticksDarkColorScheme = darkColorScheme(
    primary = HapticksSage,
    onPrimary = HapticksOnPrimary,
    primaryContainer = HapticksOlive,
    onPrimaryContainer = HapticksOliveOnContainer,
    secondary = HapticksSageDim,
    onSecondary = HapticksOnPrimary,
    secondaryContainer = HapticksOliveDim,
    onSecondaryContainer = HapticksOliveOnContainer,
    tertiary = HapticksCopper,
    onTertiary = HapticksOnPrimary,
    tertiaryContainer = HapticksCopperContainer,
    onTertiaryContainer = HapticksOnCopperContainer,
    background = HapticksBlack,
    onBackground = HapticksOnSurface,
    surface = HapticksBlack,
    onSurface = HapticksOnSurface,
    surfaceVariant = HapticksSurface,
    onSurfaceVariant = HapticksOnSurfaceMuted,
    surfaceContainerLowest = HapticksSurfaceLow,
    surfaceContainerLow = HapticksSurface,
    surfaceContainer = HapticksSurfaceContainer,
    surfaceContainerHigh = HapticksSurfaceHigh,
    surfaceContainerHighest = HapticksSurfaceHighest,
    outline = HapticksOutline,
    outlineVariant = HapticksOutlineVariant,
)

private val HapticksLightColorScheme = lightColorScheme(
    primary = HapticksOlive,
    onPrimary = Color.White,
    primaryContainer = HapticksSage,
    onPrimaryContainer = HapticksOliveDim,
    secondary = HapticksOliveDim,
    onSecondary = Color.White,
    secondaryContainer = HapticksSageDim,
    onSecondaryContainer = HapticksOlive,
    tertiary = HapticksCopper,
    onTertiary = Color.White,
    tertiaryContainer = HapticksOnCopperContainer,
    onTertiaryContainer = HapticksCopperContainer,
    background = Color(0xFFFDFCFF),
    onBackground = HapticksBlack,
    surface = Color(0xFFFDFCFF),
    onSurface = HapticksBlack,
    surfaceVariant = Color(0xFFE1E2EC),
    onSurfaceVariant = Color(0xFF44474F),
    outline = Color(0xFF74777F),
)

private fun ColorScheme.withAmoledSurfaces(): ColorScheme {
    val black = Color.Black
    return copy(
        background = black,
    )
}

@Composable
fun HapticksTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    useDynamicColors: Boolean = true,
    amoledBlack: Boolean = false,
    seedColor: Int? = null,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val context = LocalContext.current
    val baseColorScheme = remember(darkTheme, useDynamicColors, seedColor, context) {
        when {
            useDynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> {
                if (seedColor != null) {
                    val seed = Color(seedColor)
                    HapticksDarkColorScheme.copy(
                        primary = seed,
                        primaryContainer = seed.copy(alpha = 0.35f),
                        secondary = seed.copy(alpha = 0.8f),
                        secondaryContainer = seed.copy(alpha = 0.25f),
                        tertiary = seed.copy(red = seed.blue, blue = seed.red, alpha = 0.85f),
                        tertiaryContainer = seed.copy(red = seed.blue, blue = seed.red, alpha = 0.25f),
                    )
                } else {
                    HapticksDarkColorScheme
                }
            }
            else -> {
                if (seedColor != null) {
                    val seed = Color(seedColor)
                    HapticksLightColorScheme.copy(
                        primary = seed,
                        primaryContainer = seed.copy(alpha = 0.2f),
                        secondary = seed.copy(alpha = 0.75f),
                        secondaryContainer = seed.copy(alpha = 0.15f),
                        tertiary = seed.copy(red = seed.blue, blue = seed.red, alpha = 0.75f),
                        tertiaryContainer = seed.copy(red = seed.blue, blue = seed.red, alpha = 0.15f),
                    )
                } else {
                    HapticksLightColorScheme
                }
            }
        }
    }
    val colorScheme = remember(baseColorScheme, amoledBlack, darkTheme) {
        if (amoledBlack && darkTheme) baseColorScheme.withAmoledSurfaces() else baseColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = HapticksTypography,
        shapes = HapticksShapes,
        content = content,
    )
}
