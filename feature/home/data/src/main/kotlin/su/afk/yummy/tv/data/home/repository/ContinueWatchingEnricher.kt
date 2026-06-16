package su.afk.yummy.tv.data.home.repository

import kotlinx.coroutines.CancellationException
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.domain.anime.model.AnimeDetails
import su.afk.yummy.tv.domain.anime.model.AnimePoster
import su.afk.yummy.tv.domain.anime.model.AnimeVideo
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeVideosUseCase
import su.afk.yummy.tv.domain.anime.usecase.GetCachedAnimeDetailsUseCase
import su.afk.yummy.tv.domain.anime.usecase.GetCachedAnimeVideosUseCase
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.model.HomePoster
import javax.inject.Inject

class ContinueWatchingEnricher @Inject constructor(
    private val getCachedAnimeVideos: GetCachedAnimeVideosUseCase,
    private val getCachedAnimeDetails: GetCachedAnimeDetailsUseCase,
    private val getAnimeVideos: GetAnimeVideosUseCase,
) {
    suspend fun enrich(
        items: List<HomeContinueWatchingItem>,
        watchEntries: List<WatchProgressEntry>,
    ): List<HomeContinueWatchingItem> {
        if (items.isEmpty()) return items

        val sources = items
            .filter { it.animeId > 0 }
            .groupBy { it.animeId }
            .mapValues { (animeId, animeItems) ->
                loadSource(
                    animeId = animeId,
                    items = animeItems,
                    watchEntries = watchEntries,
                )
            }

        return items.map { item ->
            runCatching {
                val source = sources[item.animeId] ?: return@map item
                val matched = watchEntries.match(item)
                val video = source.videos.selectVideo(item, matched)
                item.enrichedWith(
                    video = video,
                    matched = matched,
                    details = source.details,
                )
            }.getOrElse { item }
        }
    }

    private suspend fun loadSource(
        animeId: Int,
        items: List<HomeContinueWatchingItem>,
        watchEntries: List<WatchProgressEntry>,
    ): ContinueWatchingEnrichmentSource {
        val cachedVideos = loadCachedVideos(animeId)
        val videos = if (items.any { it.needsVideoEnrichment(cachedVideos, watchEntries) }) {
            loadVideos(animeId, cachedVideos)
        } else {
            cachedVideos
        }

        return ContinueWatchingEnrichmentSource(
            videos = videos,
            details = loadCachedDetails(animeId),
        )
    }

    private suspend fun loadCachedVideos(animeId: Int): List<AnimeVideo> =
        try {
            getCachedAnimeVideos(animeId).orEmpty()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            emptyList()
        }

    private suspend fun loadVideos(
        animeId: Int,
        fallback: List<AnimeVideo>,
    ): List<AnimeVideo> =
        try {
            getAnimeVideos(animeId)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            fallback
        }

    private suspend fun loadCachedDetails(animeId: Int): AnimeDetails? =
        try {
            getCachedAnimeDetails(animeId)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            null
        }

    private fun HomeContinueWatchingItem.needsVideoEnrichment(
        videos: List<AnimeVideo>,
        watchEntries: List<WatchProgressEntry>,
    ): Boolean {
        if (videos.isNotEmpty()) return false
        val matched = watchEntries.match(this)
        return videoId <= 0 ||
                episode.isBlank() ||
                episodeUrl.isBlank() ||
                durationMs <= 0L ||
                playerName.isBlank() ||
                dubbing.isBlank() ||
                (screenshotUrl.isBlank() && matched?.screenshotUrl.isNullOrBlank())
    }

    private fun HomeContinueWatchingItem.enrichedWith(
        video: AnimeVideo?,
        matched: WatchProgressEntry?,
        details: AnimeDetails?,
    ): HomeContinueWatchingItem {
        val resolvedEpisode = episode.ifBlank {
            video?.episode
                ?: matched?.episode
                ?: ""
        }
        val resolvedEpisodeUrl = episodeUrl.ifBlank {
            video?.iframeUrl
                ?: matched?.episodeUrl
                ?: ""
        }
        val resolvedDurationMs = durationMs.takeIf { it > 0L }
            ?: video?.durationSeconds?.takeIf { it > 0 }?.secondsToMillis()
            ?: matched?.durationMs?.takeIf { it > 0L }
            ?: 0L
        val resolvedScreenshotUrl = screenshotUrl.ifBlank {
            video?.iframeUrl?.takeIf { it.isKodikSourceUrl() }
                ?: details?.screenshotForEpisode(resolvedEpisode)
                ?: matched?.screenshotUrl.orEmpty()
        }

        return copy(
            videoId = video?.id?.takeIf { it > 0 }
                ?: videoId.takeIf { it > 0 }
                ?: matched?.videoId
                ?: 0,
            poster = poster ?: details?.poster?.toHomePoster(),
            episode = resolvedEpisode,
            episodeUrl = resolvedEpisodeUrl,
            durationMs = resolvedDurationMs,
            playerName = playerName.ifBlank {
                video?.player
                    ?: matched?.playerName
                    ?: ""
            },
            dubbing = dubbing.ifBlank {
                video?.dubbing
                    ?: matched?.dubbing
                    ?: ""
            },
            screenshotUrl = resolvedScreenshotUrl,
        )
    }

    private fun List<WatchProgressEntry>.match(item: HomeContinueWatchingItem): WatchProgressEntry? {
        val episodeKey = item.episode.episodeKey()
        return firstOrNull {
            it.animeId == item.animeId &&
                    episodeKey.isNotBlank() &&
                    it.episode.episodeKey() == episodeKey
        }
            ?: firstOrNull { item.videoId > 0 && it.videoId == item.videoId }
    }

    private fun List<AnimeVideo>.selectVideo(
        item: HomeContinueWatchingItem,
        matched: WatchProgressEntry?,
    ): AnimeVideo? {
        val episodeKey = item.episode.ifBlank { matched?.episode.orEmpty() }.episodeKey()
        val episodeMatches = if (episodeKey.isNotBlank()) {
            filter { it.episode.episodeKey() == episodeKey }
        } else {
            emptyList()
        }
        val exactFallback = filter {
            val expectedVideoId = item.videoId.takeIf { id -> id > 0 } ?: matched?.videoId ?: 0
            val expectedUrl = item.episodeUrl.ifBlank { matched?.episodeUrl.orEmpty() }
            (expectedVideoId > 0 && it.id == expectedVideoId) ||
                    (expectedUrl.isNotBlank() && it.iframeUrl == expectedUrl)
        }
        val candidates = episodeMatches
            .ifEmpty { exactFallback }
            .ifEmpty { this }
        return candidates
            .takeIf { it.isNotEmpty() }
            ?.maxWithOrNull(compareBy<AnimeVideo> {
                it.matchesPreferredVideo(item, matched)
            }.thenBy {
                it.matchesPreferredSource(item, matched)
            }.thenBy {
                it.iframeUrl.isSupportedSourceUrl()
            }.thenBy {
                it.views ?: 0
            })
    }

    private fun AnimeVideo.matchesPreferredVideo(
        item: HomeContinueWatchingItem,
        matched: WatchProgressEntry?,
    ): Boolean {
        val expectedVideoId = item.videoId.takeIf { it > 0 } ?: matched?.videoId ?: 0
        return expectedVideoId > 0 && id == expectedVideoId
    }

    private fun AnimeVideo.matchesPreferredSource(
        item: HomeContinueWatchingItem,
        matched: WatchProgressEntry?,
    ): Boolean {
        val expectedPlayer = item.playerName.ifBlank { matched?.playerName.orEmpty() }
        val expectedDubbing = item.dubbing.ifBlank { matched?.dubbing.orEmpty() }
        val expectedUrl = item.episodeUrl.ifBlank { matched?.episodeUrl.orEmpty() }
        return (expectedUrl.isNotBlank() && iframeUrl == expectedUrl) ||
                (expectedPlayer.isNotBlank() && expectedDubbing.isNotBlank() &&
                        player == expectedPlayer && dubbing == expectedDubbing)
    }

    private fun AnimeDetails.screenshotForEpisode(episode: String): String? {
        val episodeKey = episode.episodeKey()
        if (episodeKey.isBlank()) return null
        return screenshots
            .firstOrNull { it.episode?.episodeKey() == episodeKey }
            ?.let { it.full ?: it.small }
    }

    private fun AnimePoster.toHomePoster(): HomePoster =
        HomePoster(
            small = small,
            medium = medium,
            big = big,
            fullsize = fullsize,
            mega = mega,
        )

    private data class ContinueWatchingEnrichmentSource(
        val videos: List<AnimeVideo>,
        val details: AnimeDetails?,
    )
}

private fun Int.secondsToMillis(): Long = this * 1_000L

private fun String.episodeKey(): String =
    trim()
        .trimStart('0')
        .ifEmpty { trim() }
        .lowercase()

private fun String.isKodikSourceUrl(): Boolean =
    contains("kodik", ignoreCase = true)

private fun String.isSupportedSourceUrl(): Boolean =
    contains("kodik", ignoreCase = true) ||
            contains("aksor.tv", ignoreCase = true) ||
            contains("iframecvh", ignoreCase = true) ||
            contains("alloha", ignoreCase = true) ||
            contains("vk.com", ignoreCase = true) ||
            contains("vkvideo", ignoreCase = true) ||
            contains("video_ext.php", ignoreCase = true) ||
            contains("iframevk", ignoreCase = true) ||
            contains("rutube.ru", ignoreCase = true)
