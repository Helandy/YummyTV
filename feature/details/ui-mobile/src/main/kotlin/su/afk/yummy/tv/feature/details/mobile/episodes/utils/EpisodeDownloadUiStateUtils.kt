package su.afk.yummy.tv.feature.details.mobile.episodes.utils

import su.afk.yummy.tv.feature.details.episodes.EpisodesState

internal fun EpisodesState.EpisodeDownloadUiState?.isDownloadBusy(): Boolean =
    this?.status == EpisodesState.EpisodeDownloadUiStatus.Queued ||
            this?.status == EpisodesState.EpisodeDownloadUiStatus.Downloading

internal fun EpisodesState.EpisodeDownloadUiState?.isDownloaded(): Boolean =
    this?.status == EpisodesState.EpisodeDownloadUiStatus.Downloaded

internal fun EpisodesState.EpisodeDownloadUiState?.isPaused(): Boolean =
    this?.status == EpisodesState.EpisodeDownloadUiStatus.Paused

internal fun EpisodesState.EpisodeDownloadUiState?.blocksNewDownload(): Boolean =
    isDownloadBusy() || isDownloaded() || isPaused()
