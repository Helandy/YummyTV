package su.afk.yummy.tv.feature.details.model

import su.afk.yummy.tv.core.common.episode.episodeGroupKey
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.anime.AnimeWatchProgress
import su.afk.yummy.tv.core.model.anime.isContinueWatchingProgress
import su.afk.yummy.tv.core.model.anime.isMeaningfulProgress
import su.afk.yummy.tv.core.model.anime.mergeEpisodeProgress
import su.afk.yummy.tv.core.model.anime.toServerWatchProgress
import su.afk.yummy.tv.feature.player.PlayerVideoSource

data class DetailsWatchProgressIndex(
    val entries: List<AnimeWatchProgress> = emptyList(),
) {
    fun resumeFromMsFor(video: AnimeVideo): Long =
        bestFor(listOf(video)).resumeFromMs()

    fun resumeFromMsFor(video: PlayerVideoSource): Long =
        matchedFor(
            videoId = video.id,
            episodeUrl = video.iframeUrl,
            episode = video.episode,
        ).bestMeaningful().resumeFromMs()

    fun bestFor(videos: List<AnimeVideo>): AnimeWatchProgress? {
        val matched = videos.flatMap { video ->
            matchedFor(
                videoId = video.id,
                episodeUrl = video.iframeUrl,
                episode = video.episode,
            )
        }
        return matched.bestMeaningful()
    }

    private fun matchedFor(
        videoId: Int,
        episodeUrl: String,
        episode: String,
    ): List<AnimeWatchProgress> =
        listOfNotNull(
            entries.firstOrNull { videoId > 0 && it.videoId == videoId },
            entries.firstOrNull { episodeUrl.isNotBlank() && it.episodeUrl == episodeUrl },
            entries.firstOrNull {
                it.episode.episodeGroupKey() == episode.episodeGroupKey()
            },
        )

    private fun List<AnimeWatchProgress>.bestMeaningful(): AnimeWatchProgress? =
        distinct()
            .filter { it.isMeaningfulProgress() }
            .maxWithOrNull(compareBy<AnimeWatchProgress> {
                it.updatedAt
            }.thenBy {
                it.positionMs
            })

    fun latestMeaningful(animeId: Int): AnimeWatchProgress? {
        val meaningful = entries.filter { it.animeId == animeId && it.isMeaningfulProgress() }
        // Незавершённый эпизод важнее уже просмотренного, даже если у просмотренного
        // updatedAt новее (например, из-за повторного открытия или серверной синхронизации) —
        // иначе "продолжить просмотр" откатывается на давно закрытую серию.
        val inProgress = meaningful.filter { it.isContinueWatchingProgress() }
        return inProgress.maxByOrNull { it.updatedAt } ?: meaningful.maxByOrNull { it.updatedAt }
    }

    companion object {
        val Empty = DetailsWatchProgressIndex()

        fun merge(
            animeId: Int,
            localEntries: List<AnimeWatchProgress>,
            videos: List<AnimeVideo>,
        ): DetailsWatchProgressIndex =
            DetailsWatchProgressIndex(
                mergeEpisodeProgress(
                    serverEntries = videos.mapNotNull { it.toServerWatchProgress(animeId) },
                    localEntries = localEntries,
                ),
            )
    }
}

private fun AnimeWatchProgress?.resumeFromMs(): Long =
    this?.positionMs?.takeIf { isContinueWatchingProgress() } ?: 0L
