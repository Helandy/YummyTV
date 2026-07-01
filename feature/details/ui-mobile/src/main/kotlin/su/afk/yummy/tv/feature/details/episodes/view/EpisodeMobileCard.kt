package su.afk.yummy.tv.feature.details.episodes.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.utils.KodikThumbnailExtractor
import su.afk.yummy.tv.domain.anime.model.AnimeVideo
import su.afk.yummy.tv.feature.details.episodes.EpisodesState
import su.afk.yummy.tv.feature.details.episodes.model.EpisodeMobileWatchStatus
import su.afk.yummy.tv.feature.details.episodes.utils.formatDuration
import su.afk.yummy.tv.feature.details.episodes.utils.timingLabel
import su.afk.yummy.tv.feature.details.mobile.R
import su.afk.yummy.tv.feature.details.view.DetailsMediaCard

private val InProgressColor = Color(0xFF4CAF50)

@Composable
internal fun EpisodeMobileCard(
    video: AnimeVideo,
    watchStatus: EpisodeMobileWatchStatus,
    kodikIframeUrl: String?,
    downloadStatus: EpisodesState.EpisodeDownloadUiState?,
    downloadResolving: Boolean,
    onInfoClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onClick: () -> Unit,
) {
    val thumbnailUrl by produceState<String?>(null, kodikIframeUrl) {
        value = kodikIframeUrl?.let { KodikThumbnailExtractor.extract(it) }
    }
    DetailsMediaCard(
        title = stringResource(R.string.details_mobile_episode, video.episode),
        subtitle = video.durationSeconds?.formatDuration(),
        footerText = watchStatus.timingLabel(),
        footerTextColor = InProgressColor,
        imageUrl = thumbnailUrl,
        badge = video.episode,
        mediaProgress = (watchStatus as? EpisodeMobileWatchStatus.InProgress)?.progress,
        mediaProgressColor = InProgressColor,
        mediaTopEndContent = when (watchStatus) {
            EpisodeMobileWatchStatus.None -> null
            else -> {
                { EpisodeMobileWatchIndicator(watchStatus = watchStatus) }
            }
        },
        trailingAction = {
            Column {
                IconButton(onClick = onInfoClick) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = stringResource(R.string.details_mobile_episode_dubbings_action),
                    )
                }
                EpisodeDownloadButton(
                    status = downloadStatus,
                    resolving = downloadResolving,
                    onClick = onDownloadClick,
                )
            }
        },
        onClick = onClick,
    )
}

@Composable
private fun EpisodeDownloadButton(
    status: EpisodesState.EpisodeDownloadUiState?,
    resolving: Boolean,
    onClick: () -> Unit,
) {
    val busy = resolving ||
            status?.status == EpisodesState.EpisodeDownloadUiStatus.Queued ||
            status?.status == EpisodesState.EpisodeDownloadUiStatus.Downloading
    IconButton(
        enabled = !busy && status?.status != EpisodesState.EpisodeDownloadUiStatus.Downloaded,
        onClick = onClick,
    ) {
        when {
            busy -> CircularProgressIndicator(
                progress = { status?.progress ?: 0f },
                strokeWidth = 2.dp,
                modifier = Modifier.size(22.dp),
            )

            status?.status == EpisodesState.EpisodeDownloadUiStatus.Downloaded ->
                Icon(
                    imageVector = Icons.Filled.Done,
                    contentDescription = stringResource(R.string.details_mobile_episode_downloaded_action),
                )

            status?.status == EpisodesState.EpisodeDownloadUiStatus.Failed ->
                Icon(
                    imageVector = Icons.Filled.ErrorOutline,
                    contentDescription = stringResource(R.string.details_mobile_episode_download_action),
                )

            else ->
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = stringResource(R.string.details_mobile_episode_download_action),
                )
        }
    }
}

@Composable
private fun EpisodeMobileWatchIndicator(
    watchStatus: EpisodeMobileWatchStatus,
) {
    when (watchStatus) {
        EpisodeMobileWatchStatus.None -> Unit
        is EpisodeMobileWatchStatus.InProgress -> Box(
            modifier = Modifier
                .size(8.dp)
                .background(InProgressColor, CircleShape),
        )

        is EpisodeMobileWatchStatus.Watched -> Box(
            modifier = Modifier
                .size(24.dp)
                .background(Color.Black.copy(alpha = 0.72f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.VisibilityOff,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
