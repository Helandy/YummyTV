package su.afk.yummy.tv.core.designsystem.presenter.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import su.afk.yummy.tv.core.designsystem.R

val OutfitFontFamily = FontFamily(
    Font(R.font.outfit_regular, FontWeight.Normal),
    Font(R.font.outfit_bold, FontWeight.Bold),
    Font(R.font.outfit_extrabold, FontWeight.ExtraBold),
)

private fun outfit(size: Int, lineHeight: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = OutfitFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
)

/** Compact cinematic scale for touch devices. */
val YummyMobileTypography = Typography(
    displayLarge = outfit(57, 64, FontWeight.ExtraBold),
    displayMedium = outfit(45, 52, FontWeight.ExtraBold),
    displaySmall = outfit(36, 44, FontWeight.Bold),
    headlineLarge = outfit(32, 40, FontWeight.Bold),
    headlineMedium = outfit(28, 36, FontWeight.Bold),
    headlineSmall = outfit(24, 32, FontWeight.Bold),
    titleLarge = outfit(22, 28, FontWeight.Bold),
    titleMedium = outfit(16, 24, FontWeight.Bold),
    titleSmall = outfit(14, 20, FontWeight.Bold),
    bodyLarge = outfit(16, 24),
    bodyMedium = outfit(14, 20),
    bodySmall = outfit(12, 16),
    labelLarge = outfit(14, 20, FontWeight.Bold),
    labelMedium = outfit(12, 16, FontWeight.Bold),
    labelSmall = outfit(11, 16),
)

/** Larger scale intended for ten-foot TV interfaces. */
val YummyTvTypography = Typography(
    displayLarge = outfit(64, 72, FontWeight.ExtraBold),
    displayMedium = outfit(52, 60, FontWeight.ExtraBold),
    displaySmall = outfit(40, 48, FontWeight.Bold),
    headlineLarge = outfit(36, 44, FontWeight.Bold),
    headlineMedium = outfit(32, 40, FontWeight.Bold),
    headlineSmall = outfit(28, 36, FontWeight.Bold),
    titleLarge = outfit(24, 32, FontWeight.Bold),
    titleMedium = outfit(20, 28, FontWeight.Bold),
    titleSmall = outfit(16, 24, FontWeight.Bold),
    bodyLarge = outfit(18, 28),
    bodyMedium = outfit(16, 24),
    bodySmall = outfit(14, 20),
    labelLarge = outfit(16, 24, FontWeight.Bold),
    labelMedium = outfit(14, 20, FontWeight.Bold),
    labelSmall = outfit(12, 16),
)
