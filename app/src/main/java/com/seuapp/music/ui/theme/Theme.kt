package com.seuapp.music.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CyanNeonColors = darkColorScheme(
    primary = IceCyan,
    onPrimary = OnCyanDark,
    primaryContainer = ElectricCyan,
    onPrimaryContainer = OnCyanDark,

    secondary = SoftPurple,
    onSecondary = OnPurpleDark,
    secondaryContainer = NeonPurple,
    onSecondaryContainer = SoftPurpleLight,

    tertiary = CoralLight,
    onTertiary = OnCoralDark,
    tertiaryContainer = SoftCoral,
    onTertiaryContainer = OnCoralDark,

    background = DeepNavy,
    onBackground = OffWhite,

    surface = DeepNavy,
    onSurface = OffWhite,
    surfaceVariant = SteelNavyLight,
    onSurfaceVariant = SteelText,

    surfaceContainerLowest = DeepNavyDark,
    surfaceContainerLow = NavyGlass,
    surfaceContainer = SteelNavy,
    surfaceContainerHigh = SurfaceHigh,
    surfaceContainerHighest = SurfaceHighest,

    outline = MutedSteel,
    outlineVariant = SteelNavyLight,

    error = ErrorRed,
    onError = OnErrorDark,

    inverseSurface = OffWhite,
    inverseOnSurface = DarkSteel,
    inversePrimary = IceCyanLight,
)

@Composable
fun MusicAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CyanNeonColors,
        typography = Typography,
        content = content
    )
}
