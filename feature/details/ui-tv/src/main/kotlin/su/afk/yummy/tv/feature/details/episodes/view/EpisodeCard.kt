package su.afk.yummy.tv.feature.details.episodes.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import su.afk.yummy.tv.core.designsystem.focus.tvFocusableClick
import su.afk.yummy.tv.core.designsystem.theme.YummySemanticColors
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.utils.kodik.KodikThumbnail
import su.afk.yummy.tv.feature.details.episodes.model.EpisodeWatchStatus
import su.afk.yummy.tv.feature.details.episodes.utils.durationLabel
import su.afk.yummy.tv.feature.details.presentation.R

private val InProgressColor = YummySemanticColors.InProgress

private val CardWidth = 220.dp
private val ThumbnailHeight = 124.dp  // 16:9 for 220dp width

@Composable
internal fun EpisodeCard(
    video: AnimeVideo,
    watchStatus: EpisodeWatchStatus = EpisodeWatchStatus.None,
    episodeTitle: String? = null,
    kodikIframeUrl: String?,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    episodeNumber: String = video.episode,
) {
    val durationLabel = watchStatus.durationLabel(video.durationSeconds)
    val shape = RoundedCornerShape(8.dp)
    Card(
        modifier = modifier
            .width(CardWidth)
            .tvFocusableClick(onClick = onClick, shape = shape, onLongClick = onLongClick),
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
                // Placeholder with episode number, виден пока превью грузится или его нет
                Text(
                    text = video.episode,
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier.align(Alignment.Center),
                )
                val previewModel = kodikIframeUrl?.let(::KodikThumbnail)
                if (previewModel != null) {
                    AsyncImage(
                        model = previewModel,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                // Watch status indicator (top-right)
                when (watchStatus) {
                    is EpisodeWatchStatus.Watched -> Box(
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = if (episodeTitle.isNullOrBlank()) 58.dp else 88.dp)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.details_episode_number, episodeNumber),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (!durationLabel.isNullOrBlank()) {
                            Text(
                                text = durationLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (watchStatus == EpisodeWatchStatus.None) {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.60f)
                                } else {
                                    InProgressColor
                                },
                            )
                        }
                    }
                    if (!episodeTitle.isNullOrBlank()) {
                        Text(
                            text = episodeTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
