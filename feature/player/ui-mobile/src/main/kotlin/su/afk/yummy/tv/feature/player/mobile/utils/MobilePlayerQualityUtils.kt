package su.afk.yummy.tv.feature.player.mobile.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight

private val Quality4kColor = Color(0xFFFFD54F)
private val Quality1440Color = Color(0xFFCE93D8)
private val Quality1080Color = Color(0xFF81C784)
private val Quality720Color = Color(0xFF64B5F6)
private val Quality480Color = Color(0xFFFFB74D)
private val QualityLowColor = Color(0xFFE57373)

/**
 * Акцентный цвет названия качества по высоте кадра («1080p» → зелёный), чтобы пункты списка не
 * сливались. Для нераспознанных названий возвращает null — используется обычный цвет текста.
 */
internal fun qualityAccentColor(quality: String): Color? {
    val upper = quality.uppercase()
    val height = if ("4K" in upper) {
        2160
    } else {
        Regex("\\d{3,4}").find(upper)?.value?.toIntOrNull()
    } ?: return null
    return when {
        height >= 2160 -> Quality4kColor
        height >= 1440 -> Quality1440Color
        height >= 1080 -> Quality1080Color
        height >= 720 -> Quality720Color
        height >= 480 -> Quality480Color
        else -> QualityLowColor
    }
}

/**
 * Подсвечивает в уже отформатированной строке («Сейчас: 720p · лимит: Лучшее») значения [current] и
 * [limit] их цветами; значение без цвета остаётся обычным. Поиск идёт слева направо, поэтому
 * одинаковые значения подсвечиваются каждое на своём месте.
 */
internal fun buildQualityStatusText(
    text: String,
    current: String,
    currentColor: Color?,
    limit: String,
    limitColor: Color?,
): AnnotatedString = buildAnnotatedString {
    append(text)
    var from = 0
    listOf(current to currentColor, limit to limitColor).forEach { (value, color) ->
        if (value.isEmpty()) return@forEach
        val start = text.indexOf(value, from)
        if (start < 0) return@forEach
        from = start + value.length
        if (color != null) {
            addStyle(SpanStyle(color = color, fontWeight = FontWeight.SemiBold), start, from)
        }
    }
}
