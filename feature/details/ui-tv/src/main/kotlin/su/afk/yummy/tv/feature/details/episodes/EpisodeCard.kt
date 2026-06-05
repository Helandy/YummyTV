package su.afk.yummy.tv.feature.details.episodes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusableClick
import su.afk.yummy.tv.core.utils.KodikThumbnailExtractor
import su.afk.yummy.tv.domain.anime.model.AnimeVideo
import su.afk.yummy.tv.feature.details.R
import su.afk.yummy.tv.feature.details.view.common.formatDuration

private val InProgressColor = Color(0xFF4CAF50)

private val CardWidth = 220.dp
private val ThumbnailHeight = 124.dp  // 16:9 for 220dp width

@Composable
internal fun EpisodeCard(
    video: AnimeVideo,
    watchStatus: EpisodeWatchStatus = EpisodeWatchStatus.None,
    kodikIframeUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val screenshotUrl by produceState<String?>(null, kodikIframeUrl) {
        if (kodikIframeUrl != null) {
            value = KodikThumbnailExtractor.extract(kodikIframeUrl)
        }
    }
    val shape = RoundedCornerShape(8.dp)
    Card(
        modifier = modifier
            .width(CardWidth)
            .tvFocusableClick(onClick = onClick, shape = shape),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column {
            // Thumbnail area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ThumbnailHeight)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                if (screenshotUrl != null) {
                    AsyncImage(
                        model = screenshotUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    // Placeholder with episode number
                    Text(
                        text = video.episode,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                // Watch status indicator (top-right)
                when (watchStatus) {
                    EpisodeWatchStatus.Watched -> Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(24.dp)
                            .background(Color.Black.copy(alpha = 0.72f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VisibilityOff,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    is EpisodeWatchStatus.InProgress -> Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(7.dp)
                            .size(8.dp)
                            .background(InProgressColor, CircleShape),
                    )
                    EpisodeWatchStatus.None -> Unit
                }

                // Progress bar at bottom of thumbnail
                if (watchStatus is EpisodeWatchStatus.InProgress) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(InProgressColor.copy(alpha = 0.25f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(watchStatus.progress)
                                .height(3.dp)
                                .background(InProgressColor),
                        )
                    }
                }
            }

            // Info row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.details_episode_number, video.episode),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    video.durationSeconds?.let {
                        Text(
                            text = it.formatDuration(),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.60f),
                        )
                    }
                }
            }
        }
    }
}
