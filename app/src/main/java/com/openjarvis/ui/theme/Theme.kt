package com.openjarvis.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * App-wide Material3 theme. Always dark — matches the "Void" palette.
 */
@Composable
fun OpenJarvisTheme(content: @Composable () -> Unit) {
    val darkScheme = darkColorScheme(
        primary = VoidColor.Violet,
        onPrimary = androidx.compose.ui.graphics.Color.White,
        secondary = VoidColor.Cyan,
        onSecondary = androidx.compose.ui.graphics.Color.Black,
        tertiary = VoidColor.Green,
        error = VoidColor.Red,
        background = VoidColor.Void950,
        onBackground = VoidColor.TextPrimary,
        surface = VoidColor.Void900,
        onSurface = VoidColor.TextPrimary,
        surfaceVariant = VoidColor.Void800,
        onSurfaceVariant = VoidColor.TextSecondary,
        outline = VoidColor.BorderSubtle,
        errorContainer = VoidColor.Void800
    )

    MaterialTheme(
        colorScheme = darkScheme,
        content = content
    )
}
