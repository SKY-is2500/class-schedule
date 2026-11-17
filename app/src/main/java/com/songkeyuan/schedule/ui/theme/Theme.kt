package com.songkeyuan.schedule.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.os.Build

private val LightColors = lightColorScheme(
    primary = Color(0xFF316754),
    secondary = Color(0xFF316754), onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCEEE4), onSecondaryContainer = Color(0xFF173E2F),
    onPrimary = CardSurface,
    primaryContainer = Color(0xFFDCEEE4),
    onPrimaryContainer = Color(0xFF173E2F),
    background = Color(0xFFF6F8F7),
    onBackground = Ink,
    surface = CardSurface,
    onSurface = Ink,
    onSurfaceVariant = MutedInk,
    surfaceVariant = Color(0xFFE9EEEB),
    outlineVariant = Color(0xFFDCE4DE),
    surfaceContainer = Color(0xFFEDF2EE),
    surfaceContainerLow = Color(0xFFF1F5F2),
)

private val DarkColors = darkColorScheme(
    secondary = Color(0xFFA1D4B8), onSecondary = Color(0xFF123829),
    secondaryContainer = Color(0xFF2A4E3D), onSecondaryContainer = Color(0xFFCEECD9),
    primary = Color(0xFFA1D4B8), onPrimary = Color(0xFF123829),
    primaryContainer = Color(0xFF2A4E3D), onPrimaryContainer = Color(0xFFCEECD9),
    background = Color(0xFF111714), onBackground = Color(0xFFE2E9E3),
    surface = Color(0xFF18201B), onSurface = Color(0xFFE2E9E3),
    onSurfaceVariant = Color(0xFFB7C3BA), surfaceVariant = Color(0xFF2B362F),
    outlineVariant = Color(0xFF344239),
    surfaceContainer = Color(0xFF202B23), surfaceContainerLow = Color(0xFF1C261F),
)

@Composable
fun ClassScheduleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colors = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, typography = ScheduleTypography, content = content)
}
