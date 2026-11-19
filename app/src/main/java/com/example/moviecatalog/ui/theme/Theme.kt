package com.example.moviecatalog.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CinemaNavyLight,
    onPrimary = CinemaOnNavyDark,
    primaryContainer = CinemaNavyContainerDark,
    onPrimaryContainer = CinemaOnNavyContainerDark,
    secondary = CinemaGoldLight,
    onSecondary = CinemaOnGoldDark,
    secondaryContainer = CinemaGoldContainerDark,
    onSecondaryContainer = CinemaOnGoldContainerDark,
    tertiary = CinemaRoseLight,
    onTertiary = CinemaOnRoseDark,
    tertiaryContainer = CinemaRoseContainerDark,
    onTertiaryContainer = CinemaOnRoseContainerDark,
    background = CinemaBackgroundDark,
    surface = CinemaSurfaceDark,
    surfaceContainerLow = CinemaSurfaceContainerLowDark,
    surfaceVariant = CinemaSurfaceVariantDark,
    outlineVariant = CinemaOutlineVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = CinemaNavy,
    onPrimary = CinemaOnNavy,
    primaryContainer = CinemaNavyContainer,
    onPrimaryContainer = CinemaOnNavyContainer,
    secondary = CinemaGold,
    onSecondary = CinemaOnGold,
    secondaryContainer = CinemaGoldContainer,
    onSecondaryContainer = CinemaOnGoldContainer,
    tertiary = CinemaRose,
    onTertiary = CinemaOnRose,
    tertiaryContainer = CinemaRoseContainer,
    onTertiaryContainer = CinemaOnRoseContainer,
    background = CinemaBackgroundLight,
    surface = CinemaSurfaceLight,
    surfaceContainerLow = CinemaSurfaceContainerLowLight,
    surfaceVariant = CinemaSurfaceVariantLight,
    outlineVariant = CinemaOutlineVariantLight
)

@Composable
fun MovieCatalogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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
        shapes = MovieCatalogShapes,
        content = content
    )
}
