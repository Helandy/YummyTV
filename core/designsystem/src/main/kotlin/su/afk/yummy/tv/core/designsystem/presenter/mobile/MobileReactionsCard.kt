package su.afk.yummy.tv.core.designsystem.presenter.mobile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.theme.YummySemanticColors
import su.afk.yummy.tv.core.utils.toCompactCount

enum class MobileReactionSelection {
    LIKE,
    DISLIKE,
    NONE,
}

@Composable
fun MobileReactionsCard(
    title: String,
    likes: Int,
    dislikes: Int,
    selection: MobileReactionSelection,
    enabled: Boolean,
    onLikeClick: () -> Unit,
    onDislikeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MobileReactionButton(
                    icon = Icons.Filled.ThumbUp,
                    count = likes,
                    color = YummySemanticColors.Like,
                    selected = selection == MobileReactionSelection.LIKE,
                    enabled = enabled,
                    onClick = onLikeClick,
                    modifier = Modifier.weight(1f),
                )
                MobileReactionButton(
                    icon = Icons.Filled.ThumbDown,
                    count = dislikes,
                    color = YummySemanticColors.Dislike,
                    selected = selection == MobileReactionSelection.DISLIKE,
                    enabled = enabled,
                    onClick = onDislikeClick,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MobileReactionButton(
    icon: ImageVector,
    count: Int,
    color: Color,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        color = if (selected) color.copy(alpha = 0.18f)
        else MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = color,
        shape = RoundedCornerShape(12.dp),
        border = if (selected) BorderStroke(1.dp, color) else null,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(
                text = count.toCompactCount(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
