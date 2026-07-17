package su.afk.yummy.tv.core.designsystem.presenter.theme

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import su.afk.yummy.tv.core.preferences.settings.AppTheme

private data class YummyTvPalette(
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val error: Color = Color(0xFFFFB4AB),
    val onError: Color = Color(0xFF690005),
    val outline: Color = Color(0xFF8E877D),
)

private val WarmAmberPalette = YummyTvPalette(
    background = Color(0xFF0B0D10),
    onBackground = Color(0xFFF4F1EA),
    surface = Color(0xFF14171C),
    surfaceVariant = Color(0xFF1E242B),
    onSurfaceVariant = Color(0xFFC9C2B8),
    primary = Color(0xFFFFB86B),
    onPrimary = Color(0xFF241100),
    primaryContainer = Color(0xFF3D2612),
    onPrimaryContainer = Color(0xFFFFE0BA),
    secondary = Color(0xFFD8C3A5),
)

private val SakuraPalette = YummyTvPalette(
    background = Color(0xFF100D12),
    onBackground = Color(0xFFF7EEF3),
    surface = Color(0xFF19141B),
    surfaceVariant = Color(0xFF251E27),
    onSurfaceVariant = Color(0xFFD7C4CF),
    primary = Color(0xFFFFA8C5),
    onPrimary = Color(0xFF3A0718),
    primaryContainer = Color(0xFF4E1C2C),
    onPrimaryContainer = Color(0xFFFFD8E6),
    secondary = Color(0xFFE3BBCB),
    outline = Color(0xFF9A8390),
)

private val MintPalette = YummyTvPalette(
    background = Color(0xFF07100E),
    onBackground = Color(0xFFEAF6F1),
    surface = Color(0xFF101A17),
    surfaceVariant = Color(0xFF1A2824),
    onSurfaceVariant = Color(0xFFBBD2CA),
    primary = Color(0xFF8FE8C2),
    onPrimary = Color(0xFF002118),
    primaryContainer = Color(0xFF123B30),
    onPrimaryContainer = Color(0xFFB7F7DD),
    secondary = Color(0xFFC3D8CF),
    outline = Color(0xFF7F938C),
)

private val OceanPalette = YummyTvPalette(
    background = Color(0xFF080E14),
    onBackground = Color(0xFFEAF3FA),
    surface = Color(0xFF101820),
    surfaceVariant = Color(0xFF1A2631),
    onSurfaceVariant = Color(0xFFBFD0DD),
    primary = Color(0xFF8DCCFF),
    onPrimary = Color(0xFF001D32),
    primaryContainer = Color(0xFF163851),
    onPrimaryContainer = Color(0xFFD2EBFF),
    secondary = Color(0xFFC0D3E1),
    outline = Color(0xFF8292A0),
)

private val GraphitePalette = YummyTvPalette(
    background = Color(0xFF0C0D0E),
    onBackground = Color(0xFFF0F0EF),
    surface = Color(0xFF161718),
    surfaceVariant = Color(0xFF232426),
    onSurfaceVariant = Color(0xFFC8C8C5),
    primary = Color(0xFFE0DED8),
    onPrimary = Color(0xFF1D1D1B),
    primaryContainer = Color(0xFF363633),
    onPrimaryContainer = Color(0xFFF4F2EC),
    secondary = Color(0xFFD0CEC8),
    outline = Color(0xFF8F8F8A),
)

@Composable
fun YummyTvTheme(
    appTheme: AppTheme = AppTheme.WARM_AMBER,
    isTelevision: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val useTvTypography = isTelevision ?: (
            LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK ==
                    Configuration.UI_MODE_TYPE_TELEVISION
            )
    MaterialTheme(
        colorScheme = appTheme.palette.toColorScheme(),
        typography = if (useTvTypography) YummyTvTypography else YummyMobileTypography,
        content = content,
    )
}

private val AppTheme.palette: YummyTvPalette
    get() = when (this) {
        AppTheme.WARM_AMBER -> WarmAmberPalette
        AppTheme.SAKURA -> SakuraPalette
        AppTheme.MINT -> MintPalette
        AppTheme.OCEAN -> OceanPalette
        AppTheme.GRAPHITE -> GraphitePalette
    }

private fun YummyTvPalette.toColorScheme() = darkColorScheme(
    background = background,
    onBackground = onBackground,
    surface = surface,
    onSurface = onBackground,
    surfaceVariant = surfaceVariant,
    onSurfaceVariant = onSurfaceVariant,
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,
    secondary = secondary,
    onSecondary = onPrimary,
    secondaryContainer = surfaceVariant,
    onSecondaryContainer = onBackground,
    tertiary = primaryContainer,
    onTertiary = onPrimaryContainer,
    tertiaryContainer = primaryContainer,
    onTertiaryContainer = onPrimaryContainer,
    error = error,
    onError = onError,
    errorContainer = onError,
    onErrorContainer = error,
    outline = outline,
    outlineVariant = surfaceVariant,
    scrim = Color.Black,
    inverseSurface = onBackground,
    inverseOnSurface = background,
    inversePrimary = primaryContainer,
)
