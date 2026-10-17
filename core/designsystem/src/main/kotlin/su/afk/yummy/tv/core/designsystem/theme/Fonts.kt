package su.afk.yummy.tv.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import su.afk.yummy.tv.core.designsystem.R

/** Onest: geometric grotesque with full Cyrillic (incl. Ukrainian) coverage. */
val YummyFontFamily = FontFamily(
    Font(R.font.onest_regular, FontWeight.Normal),
    Font(R.font.onest_medium, FontWeight.Medium),
    Font(R.font.onest_semibold, FontWeight.SemiBold),
)

private const val TABULAR_NUMERALS = "tnum"

/** Equal-width digits so clocks, ratings and counters do not jitter as values change. */
fun TextStyle.tabularNumerals(): TextStyle = copy(fontFeatureSettings = TABULAR_NUMERALS)

/** For short ALL-CAPS labels: capitals need extra tracking to stay readable. */
fun TextStyle.overline(): TextStyle = copy(letterSpacing = 0.8.sp)

private fun yummy(
    size: Int,
    lineHeight: Int,
    weight: FontWeight = FontWeight.Normal,
    tabular: Boolean = false,
) = TextStyle(
    fontFamily = YummyFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    fontFeatureSettings = if (tabular) TABULAR_NUMERALS else null,
)

/** Compact cinematic scale for touch devices. */
val YummyMobileTypography = Typography(
    displayLarge = yummy(57, 64, FontWeight.SemiBold),
    displayMedium = yummy(45, 52, FontWeight.SemiBold),
    displaySmall = yummy(36, 44, FontWeight.SemiBold),
    headlineLarge = yummy(32, 40, FontWeight.SemiBold),
    headlineMedium = yummy(28, 36, FontWeight.SemiBold),
    headlineSmall = yummy(24, 32, FontWeight.SemiBold),
    titleLarge = yummy(22, 28, FontWeight.SemiBold),
    titleMedium = yummy(16, 24, FontWeight.SemiBold),
    titleSmall = yummy(14, 20, FontWeight.SemiBold),
    bodyLarge = yummy(16, 24),
    bodyMedium = yummy(14, 20),
    bodySmall = yummy(12, 16),
    labelLarge = yummy(14, 20, FontWeight.SemiBold),
    labelMedium = yummy(12, 16, FontWeight.SemiBold, tabular = true),
    labelSmall = yummy(11, 16, FontWeight.Medium, tabular = true),
)

/** Larger scale intended for ten-foot TV interfaces. */
val YummyTvTypography = Typography(
    displayLarge = yummy(64, 72, FontWeight.SemiBold),
    displayMedium = yummy(52, 60, FontWeight.SemiBold),
    displaySmall = yummy(40, 48, FontWeight.SemiBold),
    headlineLarge = yummy(36, 44, FontWeight.SemiBold),
    headlineMedium = yummy(32, 40, FontWeight.SemiBold),
    headlineSmall = yummy(28, 36, FontWeight.SemiBold),
    titleLarge = yummy(24, 32, FontWeight.SemiBold),
    titleMedium = yummy(20, 28, FontWeight.SemiBold),
    titleSmall = yummy(16, 24, FontWeight.SemiBold),
    bodyLarge = yummy(18, 28),
    bodyMedium = yummy(16, 24),
    bodySmall = yummy(14, 20),
    labelLarge = yummy(16, 24, FontWeight.SemiBold),
    labelMedium = yummy(14, 20, FontWeight.SemiBold, tabular = true),
    labelSmall = yummy(12, 16, FontWeight.Medium, tabular = true),
)
