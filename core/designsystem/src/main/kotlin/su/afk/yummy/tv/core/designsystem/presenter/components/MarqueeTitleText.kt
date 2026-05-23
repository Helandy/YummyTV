package su.afk.yummy.tv.core.designsystem.presenter.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MarqueeTitleText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    fontWeight: FontWeight? = null,
    minLines: Int = 2,
    maxLines: Int = 2,
    isFocused: Boolean = false,
    marqueeWhenOverflow: Boolean = false,
) {
    val shouldMarquee = isFocused || marqueeWhenOverflow

    // Single-line variant: marquee the whole line on overflow
    if (maxLines == 1) {
        var overflows by remember(text) { mutableStateOf(false) }
        Text(
            text = text,
            style = style,
            fontWeight = fontWeight,
            minLines = minLines,
            maxLines = 1,
            overflow = if (overflows && shouldMarquee) TextOverflow.Clip else TextOverflow.Ellipsis,
            modifier = modifier.then(if (overflows && shouldMarquee) Modifier.basicMarquee(iterations = Int.MAX_VALUE) else Modifier),
            onTextLayout = { if (!overflows && it.hasVisualOverflow) overflows = true },
        )
        return
    }

    // Multi-line variant: lines 1..(maxLines-1) are static, the last line marquees the remainder
    var lineEndIndices by remember(text) { mutableStateOf<List<Int>>(emptyList()) }
    var hasOverflow by remember(text) { mutableStateOf(false) }
    var measured by remember(text) { mutableStateOf(false) }

    if (measured && hasOverflow && lineEndIndices.size >= maxLines - 1) {
        Column(modifier = modifier) {
            var prev = 0
            lineEndIndices.forEach { endIdx ->
                Text(
                    text = text.substring(prev, endIdx).trimEnd(),
                    style = style,
                    fontWeight = fontWeight,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
                prev = endIdx
            }
            Text(
                text = text.substring(prev).trimStart(),
                style = style,
                fontWeight = fontWeight,
                maxLines = 1,
                overflow = if (shouldMarquee) TextOverflow.Clip else TextOverflow.Ellipsis,
                modifier = if (shouldMarquee) Modifier.basicMarquee(iterations = Int.MAX_VALUE) else Modifier,
            )
        }
    } else {
        Text(
            text = text,
            style = style,
            fontWeight = fontWeight,
            minLines = minLines,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
            onTextLayout = { layout ->
                if (!measured) {
                    measured = true
                    hasOverflow = layout.hasVisualOverflow
                    if (hasOverflow) {
                        lineEndIndices = (0 until minOf(maxLines - 1, layout.lineCount))
                            .map { layout.getLineEnd(it, visibleEnd = false) }
                    }
                }
            },
        )
    }
}
