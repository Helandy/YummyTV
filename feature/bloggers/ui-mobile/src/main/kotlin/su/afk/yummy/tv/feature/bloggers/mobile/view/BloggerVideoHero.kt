package su.afk.yummy.tv.feature.bloggers.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideo
import su.afk.yummy.tv.feature.bloggers.mobile.R

@Composable
internal fun BloggerVideoHero(
    video: BloggerVideo,
    onWatch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onWatch,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 3.dp,
    ) {
        Box(Modifier.fillMaxSize()) {
            AsyncImage(
                model = video.previewUrl,
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp)),
            )
            Surface(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.95f),
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                shadowElevation = 8.dp,
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.blogger_video_watch),
                    modifier = Modifier
                        .padding(15.dp)
                        .size(34.dp),
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                HeroBadge(video.category.title, isError = false)
                if (video.hasSpoiler) {
                    HeroBadge(stringResource(R.string.blogger_video_spoiler_short), isError = true)
                }
            }
        }
    }
}

@Composable
private fun HeroBadge(label: String, isError: Boolean) {
    val color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Surface(
        color = color.copy(alpha = 0.92f),
        contentColor = if (isError) MaterialTheme.colorScheme.onError
        else MaterialTheme.colorScheme.onPrimary,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}
