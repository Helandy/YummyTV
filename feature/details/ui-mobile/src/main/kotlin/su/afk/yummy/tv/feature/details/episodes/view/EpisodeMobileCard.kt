package su.afk.yummy.tv.feature.details.episodes.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.utils.KodikThumbnailExtractor
import su.afk.yummy.tv.domain.anime.model.AnimeVideo
import su.afk.yummy.tv.feature.details.episodes.utils.formatDuration
import su.afk.yummy.tv.feature.details.mobile.R
import su.afk.yummy.tv.feature.details.view.DetailsMediaCard

@Composable
internal fun EpisodeMobileCard(
    video: AnimeVideo,
    onClick: () -> Unit,
) {
    val thumbnailUrl by produceState<String?>(null, video.iframeUrl) {
        value = KodikThumbnailExtractor.extract(video.iframeUrl)
    }
    DetailsMediaCard(
        title = stringResource(R.string.details_mobile_episode, video.episode),
        subtitle = listOf(
            video.dubbing,
            video.player,
            video.durationSeconds?.formatDuration(),
        ).filterNot { it.isNullOrBlank() }.joinToString(" • "),
        imageUrl = thumbnailUrl,
        badge = video.episode,
        onClick = onClick,
    )
}
