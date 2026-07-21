package su.afk.yummy.tv.feature.details.episodes.utils

import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.feature.details.episodes.EpisodesState

internal fun AnimeVideo.toDownloadStatusKey(): String =
    listOf(id.toString(), iframeUrl).joinToString("|")

internal fun AnimeVideo.toDownloadDubbingName(): String = dubbing.ifBlank { player }

internal fun List<AnimeVideo>.aggregateDubbingDownloadStatus(
    statuses: Map<String, EpisodesState.EpisodeDownloadUiState>,
): EpisodesState.EpisodeDownloadUiState? {
    val states = map { statuses[it.toDownloadStatusKey()] }
    if (states.isEmpty() || states.any { it == null }) return null
    val present = states.filterNotNull()
    return when {
        present.all {
            it.status == EpisodesState.EpisodeDownloadUiStatus.Queued ||
                    it.status == EpisodesState.EpisodeDownloadUiStatus.Downloading
        } -> present.first()

        present.all { it.status == EpisodesState.EpisodeDownloadUiStatus.Downloaded } -> present.first()
        present.all { it.status == EpisodesState.EpisodeDownloadUiStatus.Paused } -> present.first()
        present.all { it.status == EpisodesState.EpisodeDownloadUiStatus.Failed } -> present.first()
        else -> null
    }
}
