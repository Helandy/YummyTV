package su.afk.yummy.tv.feature.comments.mobile.view

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
import su.afk.yummy.tv.feature.comments.mobile.model.CommentTextPart
import su.afk.yummy.tv.feature.comments.mobile.utils.splitSpoilers
import su.afk.yummy.tv.feature.comments.mobile.utils.stripBbCode

@Composable
internal fun CommentBodyText(text: String) {
    val spoilers = remember(text) { mutableStateMapOf<Int, Boolean>() }
    val defaultSpoilerTitle = stringResource(R.string.comments_spoiler_title)
    val parts = remember(text, defaultSpoilerTitle) {
        splitSpoilers(text, defaultSpoilerTitle)
    }
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
