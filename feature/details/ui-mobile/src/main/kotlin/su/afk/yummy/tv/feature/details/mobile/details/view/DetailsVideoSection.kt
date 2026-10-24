package su.afk.yummy.tv.feature.details.mobile.details.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.VideoCameraFront
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.mobile.MobileSectionHeader
import su.afk.yummy.tv.core.designsystem.skeleton.SkeletonBlock
import su.afk.yummy.tv.core.designsystem.skeleton.rememberSkeletonShimmer
import su.afk.yummy.tv.core.model.anime.AnimeTrailer
import su.afk.yummy.tv.feature.details.presentation.R

@Composable
internal fun DetailsVideoSection(
    trailers: List<AnimeTrailer>,
    bloggerVideosImageModels: List<Any>,
    isTrailersLoading: Boolean,
    hasBloggerVideos: Boolean,
    onTrailersClick: () -> Unit,
    onBloggerVideosClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Наличие видео блогеров известно из деталей сразу, ждать загрузку extras не нужно.
    val showBloggerVideos = hasBloggerVideos
    if (trailers.isEmpty() && !isTrailersLoading && !showBloggerVideos) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MobileSectionHeader(title = stringResource(R.string.details_mobile_video_section))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (isTrailersLoading && trailers.isEmpty()) {
                SkeletonBlock(
                    shimmer = rememberSkeletonShimmer(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(118.dp)
                        .testTag("details_trailers_skeleton"),
                )
            } else if (trailers.isNotEmpty()) {
                DetailsVideoCard(
                    title = stringResource(R.string.details_trailers),
                    icon = Icons.Filled.Movie,
                    imageModels = listOfNotNull(trailers.firstNotNullOfOrNull { it.youtubeThumbnailUrl }),
                    subtitle = stringResource(R.string.details_mobile_trailers_hint),
                    badge = trailers.size.toString(),
                    onClick = onTrailersClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("details_trailers_card"),
                )
            }
            if (showBloggerVideos) {
                DetailsVideoCard(
                    title = stringResource(R.string.details_mobile_blogger_videos),
                    icon = Icons.Filled.VideoCameraFront,
                    imageModels = bloggerVideosImageModels,
                    subtitle = stringResource(R.string.details_mobile_blogger_videos_hint),
                    imageTint = MaterialTheme.colorScheme.tertiaryContainer,
                    onClick = onBloggerVideosClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("details_blogger_videos_card"),
                )
            }
        }
    }
}
