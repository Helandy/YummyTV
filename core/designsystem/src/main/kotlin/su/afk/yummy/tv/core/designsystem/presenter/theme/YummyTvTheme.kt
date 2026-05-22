package su.afk.yummy.tv.core.designsystem.presenter.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val YummyTvColorScheme = darkColorScheme(
    background = Color(0xFF0F0909),
    onBackground = Color(0xFFF5EEEE),
    surface = Color(0xFF1A0E0E),
    onSurface = Color(0xFFF5EEEE),
    surfaceVariant = Color(0xFF261616),
    onSurfaceVariant = Color(0xFFEBDCDC),
    primary = Color(0xFFE53935),
    onPrimary = Color(0xFF3B0000),
)

@Composable
fun YummyTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = YummyTvColorScheme,
        typography = YummyTvTypography,
        content = content,
    )
}
