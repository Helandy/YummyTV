package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import su.afk.yummy.tv.feature.details.details.utils.splitHeroTitle

/**
 * Название тайтла. Влезает в две строки крупным шрифтом — так и показываем. Не влезает — первая
 * строка крупная, остальное мельче; если и мелким не уместилось в две строки, третья бежит.
 */
@Composable
internal fun DetailsHeroTitle(
    title: String,
    style: TextStyle,
    tailFontSize: TextUnit,
    tailLineHeight: TextUnit,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    var widthPx by remember { mutableIntStateOf(0) }
    val tailStyle = style.copy(fontSize = tailFontSize, lineHeight = tailLineHeight)
    val lines = remember(title, style, tailStyle, widthPx) {
        textMeasurer.splitHeroTitle(title, style, tailStyle, widthPx, LARGE_MAX_LINES)
    }
    Column(modifier = modifier.onSizeChanged { widthPx = it.width }) {
        if (lines.tail == null) {
            Text(
                text = lines.head,
                style = style,
                maxLines = LARGE_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
            return@Column
        }
        Text(text = lines.head, style = style, maxLines = 1)
        Text(text = lines.tail, style = tailStyle, maxLines = 1)
        lines.marquee?.let { rest ->
            Text(
                text = rest,
                style = tailStyle,
                maxLines = 1,
                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
            )
        }
    }
}

private const val LARGE_MAX_LINES = 2
