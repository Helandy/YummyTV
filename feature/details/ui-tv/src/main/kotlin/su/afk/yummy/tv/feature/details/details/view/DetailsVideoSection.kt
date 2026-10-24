package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.VideoCameraFront
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.model.anime.AnimeTrailer
import su.afk.yummy.tv.feature.details.presentation.R

/** Блок «Видео»: карточки «Трейлеры» (с числом) и «Обзоры» — каждая только если такие видео есть. */
@Composable
internal fun DetailsVideoSection(
    trailers: List<AnimeTrailer>,
    bloggerVideosImageModels: List<Any>,
    hasBloggerVideos: Boolean,
    onTrailersClick: () -> Unit,
    onBloggerVideosClick: () -> Unit,
    modifier: Modifier = Modifier,
    itemFocusModifier: @Composable (index: Int) -> Modifier = { Modifier },
) {
    if (trailers.isEmpty() && !hasBloggerVideos) return
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        DetailsSectionHeader(
            title = stringResource(R.string.details_mobile_video_section),
            modifier = Modifier.padding(horizontal = DetailsSectionHorizontalPadding),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = DetailsSectionHorizontalPadding, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            var index = 0
            if (trailers.isNotEmpty()) {
                val itemIndex = index++
                item(key = "trailers") {
                    DetailsVideoCard(
                        title = stringResource(R.string.details_trailers),
                        icon = Icons.Filled.Movie,
                        imageModels = listOfNotNull(trailers.firstNotNullOfOrNull { it.youtubeThumbnailUrl }),
                        subtitle = stringResource(R.string.details_mobile_trailers_hint),
                        badge = trailers.size.toString(),
                        onClick = onTrailersClick,
                        modifier = Modifier.then(itemFocusModifier(itemIndex)),
                    )
                }
            }
            if (hasBloggerVideos) {
                val itemIndex = index++
                item(key = "blogger_videos") {
                    DetailsVideoCard(
                        title = stringResource(R.string.details_mobile_blogger_videos),
                        icon = Icons.Filled.VideoCameraFront,
                        imageModels = bloggerVideosImageModels,
                        subtitle = stringResource(R.string.details_mobile_blogger_videos_hint),
                        imageTint = MaterialTheme.colorScheme.tertiaryContainer,
                        onClick = onBloggerVideosClick,
                        modifier = Modifier.then(itemFocusModifier(itemIndex)),
                    )
                }
            }
        }
    }
}
