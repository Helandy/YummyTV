package su.afk.yummy.tv.core.designsystem.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
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
    // Светлые варианты акцента (для BackgroundStyle.LIGHT): более тёмный/насыщенный тон
    // того же оттенка, читаемый на светлом фоне. Нейтрали светлой схемы тоже тонированы под тему.
    val primaryLight: Color,
    val onPrimaryLight: Color,
    val primaryContainerLight: Color,
    val onPrimaryContainerLight: Color,
    val secondaryLight: Color,
    val lightBackground: Color,
    val lightSurface: Color,
    val lightSurfaceVariant: Color,
    val lightOnBackground: Color,
    val lightOnSurfaceVariant: Color,
    val lightOutline: Color,
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
    lightBackground = Color(0xFFFFF8F1),
    lightSurface = Color(0xFFF8ECDD),
    lightSurfaceVariant = Color(0xFFEFDFCB),
    lightOnBackground = Color(0xFF2A2017),
    lightOnSurfaceVariant = Color(0xFF5A4C3D),
    lightOutline = Color(0xFF8C7B68),
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
    lightBackground = Color(0xFFFFF7F9),
    lightSurface = Color(0xFFFAE9EF),
    lightSurfaceVariant = Color(0xFFF2DCE5),
    lightOnBackground = Color(0xFF2B1820),
    lightOnSurfaceVariant = Color(0xFF5D4350),
    lightOutline = Color(0xFF8F7280),
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
    lightBackground = Color(0xFFF6FCF9),
    lightSurface = Color(0xFFE6F3EE),
    lightSurfaceVariant = Color(0xFFD6E8E1),
    lightOnBackground = Color(0xFF12201C),
    lightOnSurfaceVariant = Color(0xFF3F544D),
    lightOutline = Color(0xFF6F867E),
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
    lightBackground = Color(0xFFF7FAFE),
    lightSurface = Color(0xFFE9F0F8),
    lightSurfaceVariant = Color(0xFFDAE5F0),
    lightOnBackground = Color(0xFF121C26),
    lightOnSurfaceVariant = Color(0xFF43525F),
    lightOutline = Color(0xFF73839A),
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
    lightBackground = Color(0xFFF7F7F6),
    lightSurface = Color(0xFFEDEDEB),
    lightSurfaceVariant = Color(0xFFE0E0DD),
    lightOnBackground = Color(0xFF1A1A19),
    lightOnSurfaceVariant = Color(0xFF484846),
    lightOutline = Color(0xFF767673),
)

private val LavenderPalette = YummyTvPalette(
    background = Color(0xFF0E0C14),
    onBackground = Color(0xFFF1EEF8),
    surface = Color(0xFF17141F),
    surfaceVariant = Color(0xFF231F2E),
    onSurfaceVariant = Color(0xFFCFC8DD),
    primary = Color(0xFFC5B3FF),
    onPrimary = Color(0xFF2B1066),
    primaryContainer = Color(0xFF3A2B6B),
    onPrimaryContainer = Color(0xFFE7DEFF),
    secondary = Color(0xFFD3C9E8),
    primaryLight = Color(0xFF6246C8),
    onPrimaryLight = Color(0xFFFFFFFF),
    primaryContainerLight = Color(0xFFE8DEFF),
    onPrimaryContainerLight = Color(0xFF1F0A5C),
    secondaryLight = Color(0xFF625A7C),
    lightBackground = Color(0xFFFBF8FF),
    lightSurface = Color(0xFFF1ECFA),
    lightSurfaceVariant = Color(0xFFE5DEF2),
    lightOnBackground = Color(0xFF1C1826),
    lightOnSurfaceVariant = Color(0xFF4B4559),
    lightOutline = Color(0xFF7B748C),
    outline = Color(0xFF8F87A0),
)

private val RubyPalette = YummyTvPalette(
    background = Color(0xFF120A0B),
    onBackground = Color(0xFFF8EDEE),
    surface = Color(0xFF1C1213),
    surfaceVariant = Color(0xFF2A1B1D),
    onSurfaceVariant = Color(0xFFDCC4C6),
    primary = Color(0xFFFF8A94),
    onPrimary = Color(0xFF4A0010),
    primaryContainer = Color(0xFF5C1A25),
    onPrimaryContainer = Color(0xFFFFD9DC),
    secondary = Color(0xFFE8BDC1),
    primaryLight = Color(0xFFB3162D),
    onPrimaryLight = Color(0xFFFFFFFF),
    primaryContainerLight = Color(0xFFFFDAD9),
    onPrimaryContainerLight = Color(0xFF410008),
    secondaryLight = Color(0xFF8A4A52),
    lightBackground = Color(0xFFFFF8F7),
    lightSurface = Color(0xFFFBEAEA),
    lightSurfaceVariant = Color(0xFFF3DCDD),
    lightOnBackground = Color(0xFF2A1517),
    lightOnSurfaceVariant = Color(0xFF5A4144),
    lightOutline = Color(0xFF8E7377),
    outline = Color(0xFF9C8184),
)

private val ForestPalette = YummyTvPalette(
    background = Color(0xFF080F09),
    onBackground = Color(0xFFECF4EA),
    surface = Color(0xFF111A12),
    surfaceVariant = Color(0xFF1C291D),
    onSurfaceVariant = Color(0xFFC2D2BF),
    primary = Color(0xFFA6DC8B),
    onPrimary = Color(0xFF0B2A06),
    primaryContainer = Color(0xFF21421C),
    onPrimaryContainer = Color(0xFFC6F2B4),
    secondary = Color(0xFFC4D6BA),
    primaryLight = Color(0xFF2E6B24),
    onPrimaryLight = Color(0xFFFFFFFF),
    primaryContainerLight = Color(0xFFCDEFBF),
    onPrimaryContainerLight = Color(0xFF072100),
    secondaryLight = Color(0xFF52634A),
    lightBackground = Color(0xFFF8FBF4),
    lightSurface = Color(0xFFEAF1E4),
    lightSurfaceVariant = Color(0xFFDBE6D3),
    lightOnBackground = Color(0xFF161E14),
    lightOnSurfaceVariant = Color(0xFF43503E),
    lightOutline = Color(0xFF74806F),
    outline = Color(0xFF82937F),
)

private val SunsetPalette = YummyTvPalette(
    background = Color(0xFF130B0A),
    onBackground = Color(0xFFFBEEE9),
    surface = Color(0xFF1E1311),
    surfaceVariant = Color(0xFF2D1D1A),
    onSurfaceVariant = Color(0xFFE2C8C0),
    primary = Color(0xFFFF8F6B),
    onPrimary = Color(0xFF4A1500),
    primaryContainer = Color(0xFF612A18),
    onPrimaryContainer = Color(0xFFFFDBCD),
    secondary = Color(0xFFEBC3B5),
    primaryLight = Color(0xFFC43E1C),
    onPrimaryLight = Color(0xFFFFFFFF),
    primaryContainerLight = Color(0xFFFFDBCC),
    onPrimaryContainerLight = Color(0xFF3B0A00),
    secondaryLight = Color(0xFF8B4B35),
    lightBackground = Color(0xFFFFF8F5),
    lightSurface = Color(0xFFFBEBE4),
    lightSurfaceVariant = Color(0xFFF3DDD3),
    lightOnBackground = Color(0xFF2B1912),
    lightOnSurfaceVariant = Color(0xFF5B4237),
    lightOutline = Color(0xFF8F7467),
    outline = Color(0xFFA0847B),
)

private val GoldPalette = YummyTvPalette(
    background = Color(0xFF0F0E08),
    onBackground = Color(0xFFF6F2E0),
    surface = Color(0xFF191810),
    surfaceVariant = Color(0xFF27251A),
    onSurfaceVariant = Color(0xFFD6D0B4),
    primary = Color(0xFFE8C85A),
    onPrimary = Color(0xFF3A2F00),
    primaryContainer = Color(0xFF4E4210),
    onPrimaryContainer = Color(0xFFFFF0A8),
    secondary = Color(0xFFD9CFA0),
    primaryLight = Color(0xFF7A6200),
    onPrimaryLight = Color(0xFFFFFFFF),
    primaryContainerLight = Color(0xFFF7E58A),
    onPrimaryContainerLight = Color(0xFF231B00),
    secondaryLight = Color(0xFF6B5F2E),
    lightBackground = Color(0xFFFFFCF0),
    lightSurface = Color(0xFFF6F1DC),
    lightSurfaceVariant = Color(0xFFECE5C8),
    lightOnBackground = Color(0xFF221E0C),
    lightOnSurfaceVariant = Color(0xFF52492A),
    lightOutline = Color(0xFF85795A),
    outline = Color(0xFF938C70),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
        BackgroundStyle.DARK, BackgroundStyle.AMOLED -> true
    }
    val amoled = backgroundStyle == BackgroundStyle.AMOLED
    val context = LocalContext.current
    val palette = appTheme.palette
    val colorScheme = when {
        appTheme == AppTheme.DYNAMIC && isDynamicColorSupported ->
            if (darkTheme) {
                dynamicDarkColorScheme(context).withAmoled(amoled)
            } else {
                dynamicLightColorScheme(context)
            }

        darkTheme -> palette.toDarkColorScheme().withAmoled(amoled)
        else -> palette.toLightColorScheme()
    }

    // Иконки статус-бара и навигационной полосы: тёмные на светлом фоне, светлые на тёмном.
    // Тема — единственное место, знающее выбранный BackgroundStyle, поэтому синхронизируем здесь.
    val view = LocalView.current
    if (!view.isInEditMode) {
        val lightBars = !darkTheme
        val applyBars = {
            view.context.findActivity()?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = lightBars
                    isAppearanceLightNavigationBars = lightBars
                }
            }
            Unit
        }
        DisposableEffect(lightBars, view) {
            applyBars()
            onDispose { }
        }
        // Возврат из плеера/фона/PiP может сбросить флаги окна — ключ lightBars при этом не менялся.
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { applyBars() }
    }

    if (useTvTypography) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = YummyTvTypography,
            shapes = YummyShapes,
            content = content,
        )
    } else {
        // Мобильная тема: более округлые формы шкалы M3 Expressive.
        MaterialTheme(
            colorScheme = colorScheme,
            typography = YummyMobileTypography,
            shapes = YummyShapes,
            motionScheme = MotionScheme.expressive(),
            content = content,
        )
    }
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

/**
 * Акцентный цвет темы для превью в списке выбора: тот, что получит `primary` после выбора,
 * с учётом светлого/тёмного фона текущего экрана. Для [AppTheme.DYNAMIC] — системный акцент.
 */
@Composable
fun AppTheme.previewColor(): Color {
    val darkSurface = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val context = LocalContext.current
    return when {
        this == AppTheme.DYNAMIC && isDynamicColorSupported ->
            if (darkSurface) dynamicDarkColorScheme(context).primary else dynamicLightColorScheme(context).primary

        darkSurface -> palette.primary
        else -> palette.primaryLight
    }
}

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
        AppTheme.LAVENDER -> LavenderPalette
        AppTheme.RUBY -> RubyPalette
        AppTheme.FOREST -> ForestPalette
        AppTheme.SUNSET -> SunsetPalette
        AppTheme.GOLD -> GoldPalette
    }

private fun YummyTvPalette.toDarkColorScheme() = darkColorScheme(
    background = background,
    onBackground = onBackground,
    surface = surface,
    onSurface = onBackground,
    surfaceVariant = surfaceVariant,
    onSurfaceVariant = onSurfaceVariant,
    surfaceContainerLowest = background,
    surfaceContainerLow = lerp(background, surface, 0.6f),
    surfaceContainer = surface,
    surfaceContainerHigh = lerp(surface, surfaceVariant, 0.6f),
    surfaceContainerHighest = surfaceVariant,
    surfaceBright = lerp(surfaceVariant, onSurfaceVariant, 0.12f),
    surfaceDim = background,
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

/**
 * AMOLED: фон и самые тёмные контейнеры — чистый чёрный (пиксели OLED гаснут), ближайшие
 * поверхности чуть выше, чтобы карточки и шторки не сливались с фоном.
 */
private fun ColorScheme.withAmoled(enabled: Boolean): ColorScheme =
    if (!enabled) {
        this
    } else {
        copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceDim = Color.Black,
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = AmoledSurfaceLow,
            surfaceContainer = AmoledSurface,
            surfaceContainerHigh = lerp(AmoledSurface, surfaceVariant, 0.6f),
            surfaceVariant = lerp(AmoledSurface, surfaceVariant, 0.8f),
            surfaceContainerHighest = surfaceVariant,
        )
    }

private val AmoledSurfaceLow = Color(0xFF070707)
private val AmoledSurface = Color(0xFF0E0E0E)

private fun YummyTvPalette.toLightColorScheme() = lightColorScheme(
    background = lightBackground,
    onBackground = lightOnBackground,
    surface = lightSurface,
    onSurface = lightOnBackground,
    surfaceVariant = lightSurfaceVariant,
    onSurfaceVariant = lightOnSurfaceVariant,
    surfaceContainerLowest = lightBackground,
    surfaceContainerLow = lerp(lightBackground, lightSurface, 0.55f),
    surfaceContainer = lightSurface,
    surfaceContainerHigh = lightSurfaceVariant,
    surfaceContainerHighest = lerp(lightSurfaceVariant, lightOnSurfaceVariant, 0.12f),
    surfaceBright = lightBackground,
    surfaceDim = lerp(lightSurfaceVariant, lightOnSurfaceVariant, 0.2f),
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onPrimaryLight,
    // Акцентная «таблетка» выделения (напр. активная вкладка нижнего меню) несёт оттенок палитры.
    secondaryContainer = primaryContainerLight,
    onSecondaryContainer = onPrimaryContainerLight,
    tertiary = secondaryLight,
    onTertiary = onPrimaryLight,
    tertiaryContainer = lerp(lightSurfaceVariant, primaryContainerLight, 0.5f),
    onTertiaryContainer = onPrimaryContainerLight,
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = lightOutline,
    outlineVariant = lerp(lightSurfaceVariant, lightOutline, 0.35f),
    scrim = Color.Black,
    inverseSurface = lightOnBackground,
    inverseOnSurface = lightBackground,
    inversePrimary = primaryContainerLight,
)
