package com.app.mobilldu.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = KomusRed,
    onPrimary = Color.White,
    primaryContainer = KomusRedDark,
    onPrimaryContainer = KomusWhite,
    secondary = KomusGrayDark,
    onSecondary = Color.White,
    background = Color(0xFF1E1E1E),
    surface = Color(0xFF252525),
    onBackground = Color.White,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF333333),
    onSurfaceVariant = Color(0xFFCCCCCC)
)

private val LightColorScheme = lightColorScheme(
    primary = KomusRed,
    onPrimary = Color.White,
    primaryContainer = KomusRedLight,
    onPrimaryContainer = KomusRedDark,
    secondary = KomusGrayDark,
    onSecondary = Color.White,
    background = Color(0xFFF8F9FA),
    surface = Color.White,
    onBackground = KomusTextDark,
    onSurface = KomusTextDark,
    surfaceVariant = KomusGrayLight,
    onSurfaceVariant = KomusTextGray
)

@Composable
fun MobilLduTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is disabled by default to keep corporate branding
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}