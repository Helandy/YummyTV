package su.afk.yummy.tv.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.theme.YummySemanticColors
import java.text.NumberFormat

/** Плашка с числом поверх коллажа коллекции: иконка и значение на тёмной подложке. */
@Composable
fun CollectionStatBadge(
    icon: ImageVector,
    value: Int,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.62f))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = remember(value) { NumberFormat.getIntegerInstance().format(value) },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = tint,
        )
    }
}

@Composable
fun CollectionLikesBadge(likesCount: Int, modifier: Modifier = Modifier) =
    CollectionStatBadge(Icons.Filled.ThumbUp, likesCount, YummySemanticColors.InProgress, modifier)

/** Лайки и просмотры в одной строке поверх коллажа; пустые значения не показываются. */
@Composable
fun CollectionStatBadges(
    likes: Int?,
    views: Int?,
    modifier: Modifier = Modifier,
) {
    if ((likes ?: 0) <= 0 && (views ?: 0) <= 0) return
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        views?.takeIf { it > 0 }?.let { CollectionViewsBadge(it) }
        likes?.takeIf { it > 0 }?.let { CollectionLikesBadge(it) }
    }
}

@Composable
fun CollectionViewsBadge(viewsCount: Int, modifier: Modifier = Modifier) =
    CollectionStatBadge(Icons.Filled.Visibility, viewsCount, Color.White, modifier)
