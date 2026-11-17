package com.songkeyuan.schedule.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val Colors = lightColorScheme(
    primary = OceanBlue,
    onPrimary = CardSurface,
    primaryContainer = SkyContainer,
    onPrimaryContainer = OceanBlueDark,
    background = ScheduleBackground,
    onBackground = Ink,
    surface = CardSurface,
    onSurface = Ink,
    onSurfaceVariant = MutedInk,
)

@Composable
fun ClassScheduleTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = ScheduleTypography, content = content)
}
