package su.afk.yummy.tv.feature.posts.view

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.theme.YummySemanticColors

@Composable
internal fun PostVoteButton(
    isLike: Boolean,
    count: Int,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val voteColor = if (isLike) YummySemanticColors.Like else YummySemanticColors.Dislike
    val tint = if (selected) voteColor else voteColor.copy(alpha = 0.72f)
    OutlinedButton(
        modifier = modifier.width(112.dp),
        enabled = enabled,
        onClick = onClick,
    ) {
        Icon(
            imageVector = if (isLike) Icons.Default.ThumbUp else Icons.Default.ThumbDown,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(18.dp)
                .padding(end = 4.dp),
        )
        Text(
            text = count.toString(),
            color = tint,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}
