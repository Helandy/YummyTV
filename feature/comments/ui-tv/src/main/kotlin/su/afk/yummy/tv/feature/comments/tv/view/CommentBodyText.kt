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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusableClick
import su.afk.yummy.tv.feature.comments.tv.R
import su.afk.yummy.tv.feature.comments.tv.model.CommentTextPart
import su.afk.yummy.tv.feature.comments.tv.utils.parseCommentText

@Composable
internal fun CommentBodyText(text: String) {
    val defaultSpoilerTitle = stringResource(R.string.comments_spoiler_title)
    val parts = remember(text, defaultSpoilerTitle) {
        parseCommentText(text, defaultSpoilerTitle)
    }
    val visibleSpoilers = remember(text) { mutableStateMapOf<Int, Boolean>() }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        parts.forEachIndexed { index, part ->
            when (part) {
                is CommentTextPart.Plain -> Text(
                    text = part.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )

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
                        Text(
                            text = part.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}
