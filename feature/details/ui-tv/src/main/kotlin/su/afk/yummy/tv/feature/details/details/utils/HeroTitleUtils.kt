package su.afk.yummy.tv.feature.details.details.utils

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import su.afk.yummy.tv.feature.details.details.model.HeroTitleLines

/**
 * Делит название на строки hero: влезает в [largeMaxLines] крупных — целиком; иначе первая строка
 * крупная, вторая мелкая, остаток — бегущая третья.
 */
internal fun TextMeasurer.splitHeroTitle(
    title: String,
    style: TextStyle,
    tailStyle: TextStyle,
    widthPx: Int,
    largeMaxLines: Int,
): HeroTitleLines {
    if (widthPx <= 0) return HeroTitleLines(title)
    val constraints = Constraints(maxWidth = widthPx)
    val large = measure(title, style, constraints = constraints)
    if (large.lineCount <= largeMaxLines) return HeroTitleLines(title)

    val headEnd = large.getLineEnd(0, visibleEnd = true)
    val head = title.substring(0, headEnd).trimEnd()
    val rest = title.substring(headEnd).trimStart()
    val small = measure(rest, tailStyle, constraints = constraints)
    if (small.lineCount <= 1) return HeroTitleLines(head, rest)
    val secondEnd = small.getLineEnd(0, visibleEnd = true)
    val second = rest.substring(0, secondEnd).trimEnd()
    val third = rest.substring(secondEnd).trimStart()
    return HeroTitleLines(head, second, third.takeIf { it.isNotEmpty() })
}
