package su.afk.yummy.tv.core.designsystem.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Цвет для иконок, текста и столбцов прямо на фоне темы. Семантические цвета подобраны под тёмный
 * фон, и на светлом пастельные оттенки (зелёный, янтарный) сливаются с ним. Там цвет становится
 * темнее, но насыщеннее: простое смешивание с чёрным даёт тусклый грязный тон.
 * Почти серые цвета (статус «брошено») насыщенность не получают, чтобы не окрашиваться.
 */
@Composable
fun Color.onThemeBackground(): Color =
    if (MaterialTheme.colorScheme.background.luminance() > LIGHT_BACKGROUND_LUMINANCE) {
        deepenForLightBackground()
    } else {
        this
    }

private fun Color.deepenForLightBackground(): Color {
    val hsv = FloatArray(HSV_SIZE)
    android.graphics.Color.colorToHSV(toArgbOpaque(), hsv)
    if (hsv[SATURATION_INDEX] > GRAY_SATURATION_LIMIT) {
        hsv[SATURATION_INDEX] = hsv[SATURATION_INDEX].coerceAtLeast(MIN_SATURATION)
    }
    hsv[VALUE_INDEX] = hsv[VALUE_INDEX].coerceAtMost(MAX_VALUE)
    return Color(android.graphics.Color.HSVToColor((alpha * ALPHA_MAX).toInt(), hsv))
}

private fun Color.toArgbOpaque(): Int =
    android.graphics.Color.rgb(
        (red * ALPHA_MAX).toInt(),
        (green * ALPHA_MAX).toInt(),
        (blue * ALPHA_MAX).toInt(),
    )

private const val LIGHT_BACKGROUND_LUMINANCE = 0.5f
private const val HSV_SIZE = 3
private const val SATURATION_INDEX = 1
private const val VALUE_INDEX = 2
private const val GRAY_SATURATION_LIMIT = 0.2f
private const val MIN_SATURATION = 0.85f
private const val MAX_VALUE = 0.68f
private const val ALPHA_MAX = 255f
