package su.afk.yummy.tv.core.designsystem.presenter.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val YummyBackground = Color(0xFF0B0D10)
private val YummyOnBackground = Color(0xFFF4F1EA)
private val YummySurface = Color(0xFF14171C)
private val YummySurfaceVariant = Color(0xFF1E242B)
private val YummyOnSurfaceVariant = Color(0xFFC9C2B8)
private val YummyPrimary = Color(0xFFFFB86B)
private val YummyOnPrimary = Color(0xFF241100)
private val YummyPrimaryContainer = Color(0xFF3D2612)
private val YummyOnPrimaryContainer = Color(0xFFFFE0BA)
private val YummySecondary = Color(0xFFD8C3A5)
private val YummyError = Color(0xFFFFB4AB)
private val YummyOnError = Color(0xFF690005)
private val YummyOutline = Color(0xFF8E877D)
private val YummyScrim = Color(0xFF000000)

private val YummyTvColorScheme = darkColorScheme(
    background = YummyBackground,
    onBackground = YummyOnBackground,
    surface = YummySurface,
    onSurface = YummyOnBackground,
    surfaceVariant = YummySurfaceVariant,
    onSurfaceVariant = YummyOnSurfaceVariant,
    primary = YummyPrimary,
    onPrimary = YummyOnPrimary,
    primaryContainer = YummyPrimaryContainer,
    onPrimaryContainer = YummyOnPrimaryContainer,
    secondary = YummySecondary,
    onSecondary = YummyOnPrimary,
    secondaryContainer = YummySurfaceVariant,
    onSecondaryContainer = YummyOnBackground,
    tertiary = YummyPrimaryContainer,
    onTertiary = YummyOnPrimaryContainer,
    tertiaryContainer = YummyPrimaryContainer,
    onTertiaryContainer = YummyOnPrimaryContainer,
    error = YummyError,
    onError = YummyOnError,
    errorContainer = YummyOnError,
    onErrorContainer = YummyError,
    outline = YummyOutline,
    outlineVariant = YummySurfaceVariant,
    scrim = YummyScrim,
    inverseSurface = YummyOnBackground,
    inverseOnSurface = YummyBackground,
    inversePrimary = YummyPrimaryContainer,
)

@Composable
fun YummyTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = YummyTvColorScheme,
        typography = YummyTvTypography,
        content = content,
    )
}
