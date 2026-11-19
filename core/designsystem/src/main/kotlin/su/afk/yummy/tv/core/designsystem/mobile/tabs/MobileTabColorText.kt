package su.afk.yummy.tv.core.designsystem.mobile.tabs

import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

/**
 * Текст вкладки с чтением цвета в фазе отрисовки ([color]).
 *
 * Позволяет плавно интерполировать цвет подписи/счётчика по [MobileTabTransition.fraction],
 * не вызывая рекомпозицию и повторную вёрстку на каждом кадре свайпа или анимации тапа.
 */
@Composable
fun MobileTabColorText(
    text: String,
    color: ColorProducer,
    style: TextStyle,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign = TextAlign.Unspecified,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    val baseStyle = LocalTextStyle.current.merge(style)
    val resolvedStyle = baseStyle.copy(
        fontWeight = fontWeight ?: baseStyle.fontWeight,
        textAlign = if (textAlign != TextAlign.Unspecified) textAlign else baseStyle.textAlign,
    )
    BasicText(
        text = text,
        modifier = modifier,
        style = resolvedStyle,
        overflow = overflow,
        softWrap = true,
        maxLines = maxLines,
        color = color,
    )
}
