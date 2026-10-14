package su.afk.yummy.tv.feature.details.episodes.utils

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import su.afk.yummy.tv.core.common.episode.episodeGroupKey
import su.afk.yummy.tv.core.common.episode.episodeNumberOrNull
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import su.afk.yummy.tv.feature.details.episodes.EpisodesState

internal fun AnimeVideo.toDownloadStatusKey(): String =
    listOf(id.toString(), iframeUrl).joinToString("|")

internal fun AnimeVideo.toDownloadDubbingName(): String = dubbing.ifBlank { player }

internal fun buildEpisodeGroups(videos: List<AnimeVideo>): ImmutableList<EpisodesState.EpisodeGroup> =
    videos
        .groupBy { it.episode.episodeGroupKey() }
        .entries
        .sortedBy { it.key.episodeNumberOrNull() ?: Double.MAX_VALUE }
        .map { (episode, groupVideos) ->
            EpisodesState.EpisodeGroup(episode, groupVideos.toImmutableList())
        }
        .toImmutableList()

/** Приоритет статуса загрузки серии: busy > paused > downloaded > failed. */
internal fun resolveDownloadStatuses(
    episodeGroups: List<EpisodesState.EpisodeGroup>,
    downloadStatuses: Map<String, EpisodesState.EpisodeDownloadUiState>,
): ImmutableMap<String, EpisodesState.EpisodeDownloadUiState?> =
    episodeGroups.associate { group ->
        val statuses = group.videos.mapNotNull { downloadStatuses[it.toDownloadStatusKey()] }
        group.episode to (
            statuses.firstOrNull {
                it.status == EpisodesState.EpisodeDownloadUiStatus.Queued ||
                    it.status == EpisodesState.EpisodeDownloadUiStatus.Downloading
            }
                ?: statuses.firstOrNull { it.status == EpisodesState.EpisodeDownloadUiStatus.Paused }
                ?: statuses.firstOrNull { it.status == EpisodesState.EpisodeDownloadUiStatus.Downloaded }
                ?: statuses.firstOrNull { it.status == EpisodesState.EpisodeDownloadUiStatus.Failed }
            )
    }.toImmutableMap()

internal val VideoDownloadItem.uiStatusKey: String
    get() = listOf(videoId.toString(), iframeUrl).joinToString("|")

internal val EpisodesState.EpisodeDownloadUiStatus.isActive: Boolean
    get() = this != EpisodesState.EpisodeDownloadUiStatus.Failed

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
