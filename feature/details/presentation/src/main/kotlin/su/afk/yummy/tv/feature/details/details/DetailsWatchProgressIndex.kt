package su.afk.yummy.tv.feature.details.details

import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.anime.AnimeWatchProgress
import su.afk.yummy.tv.core.model.anime.isContinueWatchingProgress
import su.afk.yummy.tv.core.model.anime.isMeaningfulProgress
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
                it.episode.episodeKey() == episode.episodeKey()
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

    fun latestMeaningful(animeId: Int): AnimeWatchProgress? =
        entries
            .filter { it.animeId == animeId && it.isMeaningfulProgress() }
            .maxByOrNull { it.updatedAt }

    companion object {
        val Empty = DetailsWatchProgressIndex()

        fun merge(
            animeId: Int,
            localEntries: List<AnimeWatchProgress>,
            videos: List<AnimeVideo>,
        ): DetailsWatchProgressIndex {
            val result = linkedMapOf<String, AnimeWatchProgress>()
            (videos.mapNotNull { it.toServerProgress(animeId) } + localEntries)
                .forEach { entry ->
                    val key = entry.mergeKey()
                    val current = result[key]
                    if (current == null || entry.updatedAt > current.updatedAt) {
                        result[key] = entry
                    }
                }
            return DetailsWatchProgressIndex(result.values.toList())
        }

        private fun AnimeVideo.toServerProgress(animeId: Int): AnimeWatchProgress? {
            val positionSeconds = watchedEndTimeSeconds?.takeIf { it >= 0 } ?: return null
            val dateSeconds = watchedDateSeconds?.takeIf { it > 0L } ?: return null
            val durationMs = durationSeconds?.takeIf { it > 0 }?.times(1_000L) ?: return null
            return AnimeWatchProgress(
                animeId = animeId,
                episode = episode,
                videoId = id,
                episodeUrl = iframeUrl,
                positionMs = positionSeconds * 1_000L,
                durationMs = durationMs,
                updatedAt = dateSeconds * 1_000L,
                playerName = player,
                dubbing = dubbing,
            )
        }

        private fun AnimeWatchProgress.mergeKey(): String =
            when {
                animeId > 0 && episode.isNotBlank() -> "$animeId:episode:${episode.episodeKey()}"
                videoId > 0 -> "video:$videoId"
                episodeUrl.isNotBlank() -> "url:$episodeUrl"
                else -> "entry:${hashCode()}"
            }
    }
}

private fun AnimeWatchProgress?.resumeFromMs(): Long =
    this?.positionMs?.takeIf { isContinueWatchingProgress() } ?: 0L

private fun String.episodeKey(): String =
    trim()
        .trimStart('0')
        .ifEmpty { trim() }
        .lowercase()
