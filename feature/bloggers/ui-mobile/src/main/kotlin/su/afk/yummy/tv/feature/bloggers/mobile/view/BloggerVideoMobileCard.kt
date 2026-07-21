package su.afk.yummy.tv.feature.bloggers.mobile.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import su.afk.yummy.tv.core.designsystem.presenter.theme.YummySemanticColors
import su.afk.yummy.tv.core.utils.formatFeedDateTime
import su.afk.yummy.tv.core.utils.toCompactCount
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideo
import su.afk.yummy.tv.feature.bloggers.mobile.R

@Composable
fun BloggerVideoMobileCard(
    video: BloggerVideo,
    onClick: () -> Unit,
    onBloggerClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            AsyncImage(
                model = video.previewUrl,
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Surface(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.94f),
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                shadowElevation = 6.dp,
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.blogger_video_watch),
                    modifier = Modifier
                        .padding(12.dp)
                        .size(28.dp),
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                VideoBadge(video.category.title, MaterialTheme.colorScheme.primary)
                if (video.hasSpoiler) {
                    VideoBadge(
                        stringResource(R.string.blogger_video_spoiler_short),
                        MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AsyncImage(
                    model = video.creator.avatarUrl,
                    contentDescription = video.creator.nickname,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(onClick = onBloggerClick),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onBloggerClick),
                ) {
                    Text(
                        text = video.creator.nickname,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = video.publishedAt.formatFeedDateTime(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                VideoMetric(Icons.Filled.Visibility, video.views.toCompactCount())
                VideoMetric(
                    Icons.Filled.ThumbUp,
                    video.reaction.likes.toCompactCount(),
                    YummySemanticColors.Like,
                )
            }
        }
    }
}

@Composable
private fun VideoBadge(label: String, color: androidx.compose.ui.graphics.Color) {
    Surface(
        color = color.copy(alpha = 0.92f),
        contentColor = if (color == MaterialTheme.colorScheme.primary) {
            MaterialTheme.colorScheme.onPrimary
        } else MaterialTheme.colorScheme.onError,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun VideoMetric(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(value, style = MaterialTheme.typography.labelMedium, color = color)
    }
}
