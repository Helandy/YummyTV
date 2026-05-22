package su.afk.yummy.tv.core.designsystem.presenter.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import su.afk.yummy.tv.core.designsystem.R

val OutfitFontFamily = FontFamily(
    Font(R.font.outfit_regular, FontWeight.Normal),
    Font(R.font.outfit_bold, FontWeight.Bold),
    Font(R.font.outfit_extrabold, FontWeight.ExtraBold),
)

private val _base = Typography()

val YummyTvTypography = Typography(
    displayLarge   = _base.displayLarge.copy(fontFamily = OutfitFontFamily),
    displayMedium  = _base.displayMedium.copy(fontFamily = OutfitFontFamily),
    displaySmall   = _base.displaySmall.copy(fontFamily = OutfitFontFamily),
    headlineLarge  = _base.headlineLarge.copy(fontFamily = OutfitFontFamily),
    headlineMedium = _base.headlineMedium.copy(fontFamily = OutfitFontFamily),
    headlineSmall  = _base.headlineSmall.copy(fontFamily = OutfitFontFamily),
    titleLarge     = _base.titleLarge.copy(fontFamily = OutfitFontFamily),
    titleMedium    = _base.titleMedium.copy(fontFamily = OutfitFontFamily),
    titleSmall     = _base.titleSmall.copy(fontFamily = OutfitFontFamily),
    bodyLarge      = _base.bodyLarge.copy(fontFamily = OutfitFontFamily),
    bodyMedium     = _base.bodyMedium.copy(fontFamily = OutfitFontFamily),
    bodySmall      = _base.bodySmall.copy(fontFamily = OutfitFontFamily),
    labelLarge     = _base.labelLarge.copy(fontFamily = OutfitFontFamily),
    labelMedium    = _base.labelMedium.copy(fontFamily = OutfitFontFamily),
    labelSmall     = _base.labelSmall.copy(fontFamily = OutfitFontFamily),
)
