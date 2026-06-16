package su.afk.yummy.tv.feature.details.details

import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStore
import su.afk.yummy.tv.domain.anime.model.AnimeVideo
import su.afk.yummy.tv.feature.player.PlayerVideoSource

data class DetailsWatchProgressIndex(
    val entries: List<WatchProgressEntry> = emptyList(),
) {
    fun resumeFromMsFor(video: AnimeVideo): Long =
        bestFor(listOf(video)).resumeFromMs()

    fun resumeFromMsFor(video: PlayerVideoSource): Long =
        matchedFor(
            videoId = video.id,
            episodeUrl = video.iframeUrl,
            episode = video.episode,
        ).bestMeaningful().resumeFromMs()

    fun bestFor(videos: List<AnimeVideo>): WatchProgressEntry? {
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
    ): List<WatchProgressEntry> =
        listOfNotNull(
            entries.firstOrNull { videoId > 0 && it.videoId == videoId },
            entries.firstOrNull { episodeUrl.isNotBlank() && it.episodeUrl == episodeUrl },
            entries.firstOrNull {
                it.episode.episodeKey() == episode.episodeKey()
            },
        )

    private fun List<WatchProgressEntry>.bestMeaningful(): WatchProgressEntry? =
        distinct()
            .filter { WatchProgressStore.isMeaningfulProgressEntry(it) }
            .maxWithOrNull(compareBy<WatchProgressEntry> {
                it.updatedAt
            }.thenBy {
                it.positionMs
            })

    fun latestMeaningful(animeId: Int): WatchProgressEntry? =
        entries
            .filter { it.animeId == animeId && WatchProgressStore.isMeaningfulProgressEntry(it) }
            .maxByOrNull { it.updatedAt }

    companion object {
        val Empty = DetailsWatchProgressIndex()

        fun merge(
            animeId: Int,
            localEntries: List<WatchProgressEntry>,
            videos: List<AnimeVideo>,
        ): DetailsWatchProgressIndex {
            val result = linkedMapOf<String, WatchProgressEntry>()
            (videos.mapNotNull { it.toServerProgressEntry(animeId) } + localEntries)
                .forEach { entry ->
                    val key = entry.mergeKey()
                    val current = result[key]
                    if (current == null || entry.updatedAt > current.updatedAt) {
                        result[key] = entry
                    }
                }
            return DetailsWatchProgressIndex(result.values.toList())
        }

        private fun AnimeVideo.toServerProgressEntry(animeId: Int): WatchProgressEntry? {
            val positionSeconds = watchedEndTimeSeconds?.takeIf { it >= 0 } ?: return null
            val dateSeconds = watchedDateSeconds?.takeIf { it > 0L } ?: return null
            val durationMs = durationSeconds?.takeIf { it > 0 }?.times(1_000L) ?: return null
            return WatchProgressEntry(
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

        private fun WatchProgressEntry.mergeKey(): String =
            when {
                animeId > 0 && episode.isNotBlank() -> "$animeId:episode:${episode.episodeKey()}"
                videoId > 0 -> "video:$videoId"
                episodeUrl.isNotBlank() -> "url:$episodeUrl"
                else -> "entry:${hashCode()}"
            }
    }
}

private fun WatchProgressEntry?.resumeFromMs(): Long =
    this?.positionMs?.takeIf { WatchProgressStore.isContinueWatchingEntry(this) } ?: 0L

private fun String.episodeKey(): String =
    trim()
        .trimStart('0')
        .ifEmpty { trim() }
        .lowercase()
