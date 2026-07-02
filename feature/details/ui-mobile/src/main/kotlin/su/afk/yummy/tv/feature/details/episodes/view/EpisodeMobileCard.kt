package su.afk.yummy.tv.feature.details.episodes.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.utils.KodikThumbnailExtractor
import su.afk.yummy.tv.domain.anime.model.AnimeVideo
import su.afk.yummy.tv.feature.details.episodes.EpisodesState
import su.afk.yummy.tv.feature.details.episodes.model.EpisodeMobileWatchStatus
import su.afk.yummy.tv.feature.details.episodes.utils.blocksNewDownload
import su.afk.yummy.tv.feature.details.episodes.utils.formatDuration
import su.afk.yummy.tv.feature.details.episodes.utils.isDownloadBusy
import su.afk.yummy.tv.feature.details.episodes.utils.timingLabel
import su.afk.yummy.tv.feature.details.mobile.R
import su.afk.yummy.tv.feature.details.view.DetailsMediaCard
import kotlin.math.roundToInt

private val InProgressColor = Color(0xFF4CAF50)
private val DownloadErrorColor = Color(0xFFE53935)
private val DownloadResolvingColor = Color(0xFFFFC107)

@Composable
internal fun EpisodeMobileCard(
    video: AnimeVideo,
    watchStatus: EpisodeMobileWatchStatus,
    kodikIframeUrl: String?,
    downloadStatus: EpisodesState.EpisodeDownloadUiState?,
    downloadResolving: Boolean,
    downloadAwaitingQualitySelection: Boolean,
    onInfoClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onClick: () -> Unit,
) {
    val thumbnailUrl by produceState<String?>(null, kodikIframeUrl) {
        value = kodikIframeUrl?.let { KodikThumbnailExtractor.extract(it) }
    }
    val downloadStatusText = downloadStatusText(
        status = downloadStatus,
        resolving = downloadResolving,
        awaitingQualitySelection = downloadAwaitingQualitySelection,
    )
    val downloadStatusColor = when {
        downloadResolving || downloadAwaitingQualitySelection -> DownloadResolvingColor
        downloadStatus?.status == EpisodesState.EpisodeDownloadUiStatus.Failed -> DownloadErrorColor
        else -> MaterialTheme.colorScheme.primary
    }
    DetailsMediaCard(
        title = stringResource(R.string.details_mobile_episode, video.episode),
        subtitle = video.durationSeconds?.formatDuration(),
        footerText = watchStatus.timingLabel(),
        footerTextColor = InProgressColor,
        secondaryFooterText = downloadStatusText,
        secondaryFooterTextColor = downloadStatusColor,
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
                    awaitingQualitySelection = downloadAwaitingQualitySelection,
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
    awaitingQualitySelection: Boolean,
    onClick: () -> Unit,
) {
    val waitingForUser = resolving || awaitingQualitySelection
    Box(
        modifier = Modifier.size(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(
            enabled = !waitingForUser && (
                    status?.status == EpisodesState.EpisodeDownloadUiStatus.Downloaded ||
                            !status.blocksNewDownload()
                    ),
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
        ) {
            when {
                waitingForUser ->
                    Icon(
                        imageVector = Icons.Filled.HourglassEmpty,
                        contentDescription = stringResource(
                            if (resolving) {
                                R.string.details_mobile_episode_download_resolving_quality
                            } else {
                                R.string.details_mobile_download_quality_prompt
                            }
                        ),
                        tint = DownloadResolvingColor,
                    )

                status?.status == EpisodesState.EpisodeDownloadUiStatus.Downloaded ->
                    Icon(
                        imageVector = Icons.Filled.Done,
                        contentDescription = stringResource(R.string.details_mobile_episode_downloaded_action),
                    )

                status?.status == EpisodesState.EpisodeDownloadUiStatus.Failed ->
                    Icon(
                        imageVector = Icons.Filled.RestartAlt,
                        contentDescription = stringResource(R.string.details_mobile_episode_download_restart_action),
                    )

                else ->
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = stringResource(R.string.details_mobile_episode_download_action),
                    )
            }
        }

        if (status.isDownloadBusy()) {
            LinearProgressIndicator(
                progress = { status?.progress ?: 0f },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(width = 30.dp, height = 3.dp)
                    .clip(CircleShape),
            )
        }
    }
}

@Composable
private fun downloadStatusText(
    status: EpisodesState.EpisodeDownloadUiState?,
    resolving: Boolean,
    awaitingQualitySelection: Boolean,
): String? {
    val uiStatus = status?.status
    return when {
        resolving -> stringResource(R.string.details_mobile_episode_download_resolving_quality)
        awaitingQualitySelection -> stringResource(R.string.details_mobile_download_quality_prompt)

        uiStatus == EpisodesState.EpisodeDownloadUiStatus.Queued ||
                uiStatus == EpisodesState.EpisodeDownloadUiStatus.Downloading -> {
            val percent = (status.progress.coerceIn(0f, 1f) * 100).roundToInt()
            stringResource(R.string.details_mobile_episode_download_progress, percent)
        }

        uiStatus == EpisodesState.EpisodeDownloadUiStatus.Paused -> {
            val percent = (status.progress.coerceIn(0f, 1f) * 100).roundToInt()
            stringResource(R.string.details_mobile_episode_download_paused, percent)
        }

        uiStatus == EpisodesState.EpisodeDownloadUiStatus.Failed -> {
            val message = status.errorMessage
            if (message.isNullOrBlank()) {
                stringResource(R.string.details_mobile_episode_download_error_unknown)
            } else {
                stringResource(R.string.details_mobile_episode_download_error, message)
            }
        }

        else -> null
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
