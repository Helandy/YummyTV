package su.afk.yummy.tv.feature.comments.tv.view

import androidx.compose.foundation.background
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.focus.tvFocusHighlight
import su.afk.yummy.tv.core.designsystem.focus.tvFocusableClick
import su.afk.yummy.tv.core.utils.formatting.toParagraphs
import su.afk.yummy.tv.feature.comments.model.CommentTextPart
import su.afk.yummy.tv.feature.comments.presentation.R
import su.afk.yummy.tv.feature.comments.utils.parseCommentText

/**
 * @param nextFocusRequester куда должен уйти фокус по DOWN с последнего фокус-стопа текста —
 * без этого пространственный focus-search у Compose попадает не на первую кнопку (лайк), а на
 * произвольную, т.к. текст растянут на всю ширину карточки, а кнопки — нет.
 */
@Composable
internal fun CommentBodyText(text: String, nextFocusRequester: FocusRequester) {
    val defaultSpoilerTitle = stringResource(R.string.comments_spoiler_title)
    val parts = remember(text, defaultSpoilerTitle) {
        parseCommentText(text, defaultSpoilerTitle)
    }
    val visibleSpoilers = remember(text) { mutableStateMapOf<Int, Boolean>() }
    val focusStopCount = parts.indices.sumOf { index ->
        when (val part = parts[index]) {
            is CommentTextPart.Plain -> part.text.toParagraphs().size
            is CommentTextPart.Spoiler ->
                1 + if (visibleSpoilers[index] == true) part.text.toParagraphs().size else 0
        }
    }
    var focusStopIndex = 0
    fun Modifier.lastFocusStopDown(): Modifier {
        focusStopIndex++
        return if (focusStopIndex == focusStopCount) {
            focusProperties { down = nextFocusRequester }
        } else {
            this
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        parts.forEachIndexed { index, part ->
            when (part) {
                is CommentTextPart.Plain -> part.text.toParagraphs().forEach { paragraph ->
                    Text(
                        text = paragraph,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .lastFocusStopDown()
                            .tvFocusHighlight(),
                    )
                }

                is CommentTextPart.Spoiler -> {
                    val visible = visibleSpoilers[index] == true
                    Text(
                        text = if (visible) part.title else stringResource(
                            R.string.comments_spoiler_hidden,
                            part.title,
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .lastFocusStopDown()
                            .tvFocusableClick(
                                onClick = { visibleSpoilers[index] = !visible },
                                shape = RoundedCornerShape(8.dp),
                                focusedScale = 1.01f,
                            )
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.76f),
                                RoundedCornerShape(8.dp),
                            )
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                    if (visible) {
                        part.text.toParagraphs().forEach { paragraph ->
                            Text(
                                text = paragraph,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .lastFocusStopDown()
                                    .tvFocusHighlight(),
                            )
                        }
                    }
                }
            }
        }
    }
}
