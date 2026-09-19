package su.afk.yummy.tv.core.designsystem.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import su.afk.yummy.tv.core.model.settings.AppTheme
import su.afk.yummy.tv.core.model.settings.BackgroundStyle

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
    // Светлые варианты акцента (для BackgroundStyle.GRAY/LIGHT): более тёмный/насыщенный тон
    // того же оттенка, читаемый на белом фоне. Нейтрали при этом общие (см. LightNeutrals).
    val primaryLight: Color,
    val onPrimaryLight: Color,
    val primaryContainerLight: Color,
    val onPrimaryContainerLight: Color,
    val secondaryLight: Color,
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
    primaryLight = Color(0xFFB05A00),
    onPrimaryLight = Color(0xFFFFFFFF),
    primaryContainerLight = Color(0xFFFFDCC0),
    onPrimaryContainerLight = Color(0xFF2E1500),
    secondaryLight = Color(0xFF7A5A2E),
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
    primaryLight = Color(0xFFB0295E),
    onPrimaryLight = Color(0xFFFFFFFF),
    primaryContainerLight = Color(0xFFFFD9E4),
    onPrimaryContainerLight = Color(0xFF3E0021),
    secondaryLight = Color(0xFF8E4A63),
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
    primaryLight = Color(0xFF00695C),
    onPrimaryLight = Color(0xFFFFFFFF),
    primaryContainerLight = Color(0xFFB8F0DD),
    onPrimaryContainerLight = Color(0xFF00201A),
    secondaryLight = Color(0xFF3D6B5E),
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
    primaryLight = Color(0xFF00629E),
    onPrimaryLight = Color(0xFFFFFFFF),
    primaryContainerLight = Color(0xFFCFE5FF),
    onPrimaryContainerLight = Color(0xFF001D33),
    secondaryLight = Color(0xFF4A607A),
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
    primaryLight = Color(0xFF3A3D40),
    onPrimaryLight = Color(0xFFFFFFFF),
    primaryContainerLight = Color(0xFFDDE0E3),
    onPrimaryContainerLight = Color(0xFF1A1C1E),
    secondaryLight = Color(0xFF55595C),
)

/** Общие нейтрали светлой схемы (фон/поверхности/текст), задаются режимом фона. */
private data class LightNeutrals(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
)

private val LightNeutralsWhite = LightNeutrals(
    // Фон белый, панели чуть серее — иначе сливаются с белым.
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFF0F0F0),
    surfaceVariant = Color(0xFFE6E6E6),
)

private val LightOnBackground = Color(0xFF1A1A1A)
private val LightOnSurfaceVariant = Color(0xFF444444)
private val LightOutline = Color(0xFF757575)

@Composable
fun YummyTvTheme(
    appTheme: AppTheme = AppTheme.WARM_AMBER,
    backgroundStyle: BackgroundStyle = BackgroundStyle.DARK,
    isTelevision: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val useTvTypography = isTelevision ?: (
        LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK ==
            Configuration.UI_MODE_TYPE_TELEVISION
        )
    val darkTheme = when (backgroundStyle) {
        BackgroundStyle.SYSTEM -> isSystemInDarkTheme()
        BackgroundStyle.LIGHT -> false
        BackgroundStyle.DARK -> true
    }
    val context = LocalContext.current
    val palette = appTheme.palette
    val colorScheme = when {
        appTheme == AppTheme.DYNAMIC && isDynamicColorSupported ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        darkTheme -> palette.toDarkColorScheme()
        else -> palette.toLightColorScheme(LightNeutralsWhite)
    }

    // Иконки статус-бара и навигационной полосы: тёмные на светлом фоне, светлые на тёмном.
    // Тема — единственное место, знающее выбранный BackgroundStyle, поэтому синхронизируем здесь.
    val view = LocalView.current
    if (!view.isInEditMode) {
        val lightBars = !darkTheme
        DisposableEffect(lightBars) {
            view.context.findActivity()?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = lightBars
                    isAppearanceLightNavigationBars = lightBars
                }
            }
            onDispose { }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = if (useTvTypography) YummyTvTypography else YummyMobileTypography,
        content = content,
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Системная палитра (Material You) появилась в Android 12; ниже [AppTheme.DYNAMIC]
 * откатывается на обычную палитру, поэтому выбор темы прячем на старых версиях.
 */
@get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
val isDynamicColorSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

private val AppTheme.palette: YummyTvPalette
    get() = when (this) {
        // DYNAMIC цвета берёт у системы; палитра нужна лишь как фолбэк до Android 12.
        AppTheme.DYNAMIC,
        AppTheme.WARM_AMBER,
        -> WarmAmberPalette

        AppTheme.SAKURA -> SakuraPalette
        AppTheme.MINT -> MintPalette
        AppTheme.OCEAN -> OceanPalette
        AppTheme.GRAPHITE -> GraphitePalette
    }

private fun YummyTvPalette.toDarkColorScheme() = darkColorScheme(
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

private fun YummyTvPalette.toLightColorScheme(neutrals: LightNeutrals) = lightColorScheme(
    background = neutrals.background,
    onBackground = LightOnBackground,
    surface = neutrals.surface,
    onSurface = LightOnBackground,
    surfaceVariant = neutrals.surfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onPrimaryLight,
    // Акцентная «таблетка» выделения (напр. активная вкладка нижнего меню) несёт оттенок палитры.
    secondaryContainer = primaryContainerLight,
    onSecondaryContainer = onPrimaryContainerLight,
    tertiary = primaryContainerLight,
    onTertiary = onPrimaryContainerLight,
    tertiaryContainer = primaryContainerLight,
    onTertiaryContainer = onPrimaryContainerLight,
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = LightOutline,
    outlineVariant = neutrals.surfaceVariant,
    scrim = Color.Black,
    inverseSurface = LightOnBackground,
    inverseOnSurface = neutrals.background,
    inversePrimary = primaryContainerLight,
)
