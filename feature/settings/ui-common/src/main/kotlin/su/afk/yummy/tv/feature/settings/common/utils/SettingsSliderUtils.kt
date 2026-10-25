package su.afk.yummy.tv.feature.settings.common.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

private val NumberRegex = Regex("""\d+([.,]\d+)?""")

/** Подсвечивает числа в подписи значения слайдера, чтобы цифра читалась с первого взгляда. */
fun String.highlightNumbers(color: Color): AnnotatedString = buildAnnotatedString {
    var last = 0
    for (match in NumberRegex.findAll(this@highlightNumbers)) {
        append(substring(last, match.range.first))
        withStyle(SpanStyle(color = color, fontWeight = FontWeight.SemiBold)) { append(match.value) }
        last = match.range.last + 1
    }
    append(substring(last))
}
