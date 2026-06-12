package tech.dotlab.dot.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Nothing palette: monochrome with a single red accent. */
object DotColors {
    val Black = Color(0xFF000000)
    val White = Color(0xFFFFFFFF)
    val Red = Color(0xFFD71921)
    val DimGrey = Color(0xFF2A2A2A)
    val MidGrey = Color(0xFF6E6E6E)
}

private val DarkScheme = darkColorScheme(
    primary = DotColors.Red,
    onPrimary = DotColors.White,
    background = DotColors.Black,
    onBackground = DotColors.White,
    surface = DotColors.Black,
    onSurface = DotColors.White,
    surfaceVariant = DotColors.DimGrey,
    onSurfaceVariant = DotColors.MidGrey,
    error = DotColors.Red,
)

private val LightScheme = lightColorScheme(
    primary = DotColors.Red,
    onPrimary = DotColors.White,
    background = DotColors.White,
    onBackground = DotColors.Black,
    surface = DotColors.White,
    onSurface = DotColors.Black,
    error = DotColors.Red,
)

@Composable
fun DotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = Typography(),
        content = content,
    )
}
