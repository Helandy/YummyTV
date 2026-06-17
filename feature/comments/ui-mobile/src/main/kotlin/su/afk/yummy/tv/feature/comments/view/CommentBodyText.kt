package su.afk.yummy.tv.feature.comments.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.comments.mobile.R

private val spoilerRegex = Regex(
    pattern = "\\[спойлер(?:=\"([^\"]*)\")?](.*?)\\[/спойлер]",
    options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
)
private val bbCodeRegex = Regex("\\[/?[^\\]]+]")

@Composable
internal fun CommentBodyText(text: String) {
    val spoilers = remember(text) { mutableStateMapOf<Int, Boolean>() }
    val parts = remember(text) { splitSpoilers(text) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        parts.forEachIndexed { index, part ->
            when (part) {
                is CommentTextPart.Plain -> Text(
                    text = part.text.stripBbCode(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                is CommentTextPart.Spoiler -> {
                    val visible = spoilers[index] == true
                    Text(
                        text = if (visible) {
                            part.title
                        } else {
                            stringResource(R.string.comments_spoiler_hidden, part.title)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f))
                            .clickable { spoilers[index] = !visible }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                    if (visible) {
                        Text(
                            text = part.text.stripBbCode(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

private sealed interface CommentTextPart {
    data class Plain(val text: String) : CommentTextPart
    data class Spoiler(val title: String, val text: String) : CommentTextPart
}

private fun splitSpoilers(text: String): List<CommentTextPart> {
    val result = mutableListOf<CommentTextPart>()
    var cursor = 0
    spoilerRegex.findAll(text).forEach { match ->
        val before = text.substring(cursor, match.range.first)
        if (before.isNotBlank()) result += CommentTextPart.Plain(before)
        result += CommentTextPart.Spoiler(
            title = match.groups[1]?.value?.takeIf { it.isNotBlank() } ?: "Спойлер",
            text = match.groups[2]?.value.orEmpty(),
        )
        cursor = match.range.last + 1
    }
    val tail = text.substring(cursor)
    if (tail.isNotBlank()) result += CommentTextPart.Plain(tail)
    return result.ifEmpty { listOf(CommentTextPart.Plain(text)) }
}

private fun String.stripBbCode(): String =
    replace(bbCodeRegex, "")
        .replace("&nbsp;", " ")
        .trim()
