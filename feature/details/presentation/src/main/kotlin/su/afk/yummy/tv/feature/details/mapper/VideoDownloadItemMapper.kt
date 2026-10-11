package su.afk.yummy.tv.feature.details.mapper

import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadStatus
import su.afk.yummy.tv.feature.details.episodes.EpisodesState

internal fun VideoDownloadItem.toUiState(): EpisodesState.EpisodeDownloadUiState =
    EpisodesState.EpisodeDownloadUiState(
        downloadId = id,
        dubbing = dubbing.ifBlank { playerName },
        playerName = playerName,
        qualityLabel = qualityLabel,
        bytesDownloaded = bytesDownloaded,
        status = when (status) {
            VideoDownloadStatus.Queued -> EpisodesState.EpisodeDownloadUiStatus.Queued

            // Resolving идёт уже в занятом слоте (обновление ссылки), это не ожидание очереди.
            VideoDownloadStatus.Resolving,
            VideoDownloadStatus.Downloading,
            VideoDownloadStatus.Deleting,
            -> EpisodesState.EpisodeDownloadUiStatus.Downloading

            VideoDownloadStatus.Paused -> EpisodesState.EpisodeDownloadUiStatus.Paused
            VideoDownloadStatus.Downloaded -> EpisodesState.EpisodeDownloadUiStatus.Downloaded
            VideoDownloadStatus.Failed -> EpisodesState.EpisodeDownloadUiStatus.Failed
            VideoDownloadStatus.Idle,
            VideoDownloadStatus.Deleted,
            -> EpisodesState.EpisodeDownloadUiStatus.Failed
        },
        progress = progress.coerceIn(0f, 1f),
        errorMessage = errorMessage?.takeIf { it.isNotBlank() },
    )
