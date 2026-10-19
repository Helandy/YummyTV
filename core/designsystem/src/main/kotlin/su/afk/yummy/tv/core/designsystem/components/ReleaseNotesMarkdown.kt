package su.afk.yummy.tv.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.utils.markdown.MarkdownBlock
import su.afk.yummy.tv.core.utils.markdown.MarkdownSpan
import su.afk.yummy.tv.core.utils.markdown.parseMarkdown

private val ListIndent = 14.dp
private val BulletWidth = 14.dp

/**
 * Описание релиза из GitHub в разметке markdown: заголовки, списки с вложенностью, цитаты, код,
 * жирный/курсив/зачёркнутый и ссылки. Рисуется обычной [Column], поэтому живёт внутри уже
 * прокручиваемых контейнеров (диалог обновления, шторка и диалог истории).
 */
@Composable
fun ReleaseNotesMarkdown(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    val blocks = remember(markdown) { markdown.parseMarkdown() }
    val colors = MarkdownColors(
        text = MaterialTheme.colorScheme.onSurface,
        muted = MaterialTheme.colorScheme.onSurfaceVariant,
        accent = MaterialTheme.colorScheme.primary,
        codeBackground = MaterialTheme.colorScheme.surfaceVariant,
    )
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        blocks.forEachIndexed { index, block ->
            when (block) {
                is MarkdownBlock.Heading -> Text(
                    text = block.spans.toAnnotated(colors),
                    style = if (block.level <= 2) {
                        MaterialTheme.typography.titleMedium
                    } else {
                        MaterialTheme.typography.titleSmall
                    },
                    fontWeight = FontWeight.SemiBold,
                    color = colors.accent,
                    modifier = Modifier.padding(top = if (index == 0) 0.dp else 6.dp),
                )

                is MarkdownBlock.Paragraph -> Text(
                    text = block.spans.toAnnotated(colors),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.text,
                )

                is MarkdownBlock.Bullet -> ListItem(block.depth, "•", block.spans, colors)

                is MarkdownBlock.Numbered -> ListItem(block.depth, "${block.number}.", block.spans, colors)

                is MarkdownBlock.Quote -> Row(
                    modifier = Modifier.height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        Modifier
                            .width(3.dp)
                            .fillMaxHeight()
                            .background(colors.accent.copy(alpha = 0.6f), RoundedCornerShape(2.dp)),
                    )
                    Text(
                        text = block.spans.toAnnotated(colors),
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = colors.muted,
                    )
                }

                is MarkdownBlock.Code -> Text(
                    text = block.text,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = colors.text,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.codeBackground.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )

                MarkdownBlock.Divider -> HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = colors.muted.copy(alpha = 0.4f),
                )
            }
        }
    }
}

@Composable
private fun ListItem(
    depth: Int,
    marker: String,
    spans: List<MarkdownSpan>,
    colors: MarkdownColors,
) {
    Row(modifier = Modifier.padding(start = ListIndent * depth)) {
        Text(
            text = marker,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.accent,
            modifier = Modifier.widthIn(min = BulletWidth),
        )
        Text(
            text = spans.toAnnotated(colors),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.text,
            modifier = Modifier.weight(1f),
        )
    }
}

private class MarkdownColors(
    val text: Color,
    val muted: Color,
    val accent: Color,
    val codeBackground: Color,
)

private fun List<MarkdownSpan>.toAnnotated(colors: MarkdownColors): AnnotatedString =
    buildAnnotatedString {
        forEach { span ->
            val style = SpanStyle(
                fontWeight = if (span.bold) FontWeight.Bold else null,
                fontStyle = if (span.italic) FontStyle.Italic else null,
                textDecoration = if (span.strike) TextDecoration.LineThrough else null,
                fontFamily = if (span.code) FontFamily.Monospace else null,
                background = if (span.code) colors.codeBackground else Color.Unspecified,
            )
            val link = span.link
            if (link != null) {
                val linkStyle = TextLinkStyles(
                    SpanStyle(color = colors.accent, textDecoration = TextDecoration.Underline),
                )
                withLink(LinkAnnotation.Url(link, linkStyle)) {
                    withStyle(style) { append(span.text) }
                }
            } else {
                withStyle(style) { append(span.text) }
            }
        }
    }
