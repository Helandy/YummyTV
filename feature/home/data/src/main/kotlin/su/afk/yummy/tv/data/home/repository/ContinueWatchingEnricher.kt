package su.afk.yummy.tv.data.home.repository

import kotlinx.coroutines.CancellationException
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.domain.anime.model.AnimeDetails
import su.afk.yummy.tv.domain.anime.model.AnimePoster
import su.afk.yummy.tv.domain.anime.model.AnimeVideo
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeVideosUseCase
import su.afk.yummy.tv.domain.anime.usecase.GetCachedAnimeDetailsUseCase
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.model.HomePoster
import javax.inject.Inject
import kotlin.math.abs

class ContinueWatchingEnricher @Inject constructor(
    private val getAnimeVideos: GetAnimeVideosUseCase,
    private val getCachedAnimeDetails: GetCachedAnimeDetailsUseCase,
) {
    suspend fun enrich(
        items: List<HomeContinueWatchingItem>,
        watchEntries: List<WatchProgressEntry>,
    ): List<HomeContinueWatchingItem> {
        if (items.isEmpty()) return items

        val animeIds = items
            .map { it.animeId }
            .filter { it > 0 }
            .distinct()
        val detailsByAnimeId = animeIds.associateWithCachedDetails()
        val videoEnrichmentAnimeIds = items
            .sortedByDescending { it.updatedAt }
            .asSequence()
            .map { it.animeId }
            .filter { it > 0 }
            .distinct()
            .take(VIDEO_ENRICH_LIMIT)
            .toSet()
        val videosByAnimeId = videoEnrichmentAnimeIds.associateWithVideos()

        return items.map { item ->
            runCatching {
                val details = detailsByAnimeId[item.animeId]
                val videos = videosByAnimeId[item.animeId].orEmpty()
                if (item.animeId !in videoEnrichmentAnimeIds) {
                    return@map item.withDetailsPoster(details)
                }
                val matched = watchEntries.match(item)
                val playbackVideo = videos.selectPlaybackVideo(item, matched)
                val thumbnailVideo = videos.selectThumbnailVideo(item, playbackVideo)
                item.enrichedWith(
                    playbackVideo = playbackVideo,
                    thumbnailVideo = thumbnailVideo,
                    matched = matched,
                    details = details,
                )
            }.getOrElse { item }
        }
    }

    private suspend fun List<Int>.associateWithCachedDetails(): Map<Int, AnimeDetails?> {
        val result = linkedMapOf<Int, AnimeDetails?>()
        forEach { animeId ->
            result[animeId] = loadCachedDetails(animeId)
        }
        return result
    }

    private suspend fun Set<Int>.associateWithVideos(): Map<Int, List<AnimeVideo>> {
        val result = linkedMapOf<Int, List<AnimeVideo>>()
        forEach { animeId ->
            result[animeId] = loadVideos(animeId)
        }
        return result
    }

    private suspend fun loadVideos(animeId: Int): List<AnimeVideo> =
        try {
            getAnimeVideos(animeId)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            emptyList()
        }

    private suspend fun loadCachedDetails(animeId: Int): AnimeDetails? =
        try {
            getCachedAnimeDetails(animeId)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            null
        }

    private fun HomeContinueWatchingItem.withDetailsPoster(details: AnimeDetails?): HomeContinueWatchingItem =
        copy(poster = poster ?: details?.poster?.toHomePoster())

    private fun HomeContinueWatchingItem.enrichedWith(
        playbackVideo: AnimeVideo?,
        thumbnailVideo: AnimeVideo?,
        matched: WatchProgressEntry?,
        details: AnimeDetails?,
    ): HomeContinueWatchingItem {
        val resolvedEpisode = episode.ifBlank {
            playbackVideo?.episode
                ?: matched?.episode
                ?: ""
        }
        val resolvedEpisodeUrl = episodeUrl.ifBlank {
            playbackVideo?.iframeUrl
                ?: matched?.episodeUrl
                ?: ""
        }
        val resolvedDurationMs = durationMs.takeIf { it > 0L }
            ?: playbackVideo?.durationSeconds?.takeIf { it > 0 }?.secondsToMillis()
            ?: matched?.durationMs?.takeIf { it > 0L }
            ?: thumbnailVideo?.durationSeconds?.takeIf { it > 0 }?.secondsToMillis()
            ?: 0L
        val kodikThumbnailSource = thumbnailVideo?.iframeUrl?.takeIf { it.isKodikSourceUrl() }
        val resolvedScreenshotUrl = kodikThumbnailSource
            ?: screenshotUrl.takeIf { it.isNotBlank() }
            ?: details?.screenshotForEpisode(resolvedEpisode)
            ?: matched?.screenshotUrl.orEmpty()

        return copy(
            videoId = playbackVideo?.id?.takeIf { it > 0 }
                ?: videoId.takeIf { it > 0 }
                ?: matched?.videoId
                ?: 0,
            poster = poster ?: details?.poster?.toHomePoster(),
            episode = resolvedEpisode,
            episodeUrl = resolvedEpisodeUrl,
            durationMs = resolvedDurationMs,
            playerName = playerName.ifBlank {
                playbackVideo?.player
                    ?: matched?.playerName
                    ?: ""
            },
            dubbing = dubbing.ifBlank {
                playbackVideo?.dubbing
                    ?: matched?.dubbing
                    ?: ""
            },
            screenshotUrl = resolvedScreenshotUrl,
        )
    }

    private fun List<WatchProgressEntry>.match(item: HomeContinueWatchingItem): WatchProgressEntry? {
        val episodeKey = item.episode.episodeKey()
        return firstOrNull { item.videoId > 0 && it.videoId == item.videoId }
            ?: firstOrNull {
                it.animeId == item.animeId &&
                        episodeKey.isNotBlank() &&
                        it.episode.episodeKey() == episodeKey
            }
    }

    private fun List<AnimeVideo>.selectPlaybackVideo(
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
        if (candidates.isEmpty()) return selectServerWatchedVideo(item)
        return candidates
            .takeIf { it.isNotEmpty() }
            ?.maxWithOrNull(compareBy<AnimeVideo> {
                it.matchesPreferredVideo(item, matched)
            }.thenBy {
                it.matchesPreferredSource(item, matched)
            }.thenBy {
                it.iframeUrl.isKodikSourceUrl()
            }.thenBy {
                it.iframeUrl.isSupportedSourceUrl()
            }.thenBy {
                it.views ?: 0
            })
    }

    private fun List<AnimeVideo>.selectServerWatchedVideo(item: HomeContinueWatchingItem): AnimeVideo? {
        if (!item.needsServerWatchedResolution()) return null
        val watchedCandidates = filter { video ->
            video.watchedDateSeconds != null && video.iframeUrl.isSupportedSourceUrl()
        }
        val itemDateSeconds = item.updatedAt.takeIf { it > 0L }?.div(1_000L)
        val exactDateMatches = watchedCandidates.filter { video ->
            itemDateSeconds != null && video.watchedDateSeconds == itemDateSeconds
        }
        val exactPositionMatches = watchedCandidates.filter { video ->
            item.positionMs > 0L && video.watchedEndTimeSeconds?.secondsToMillis() == item.positionMs
        }
        val candidates = exactDateMatches.ifEmpty { exactPositionMatches }
        return candidates.minWithOrNull(compareBy<AnimeVideo> {
            it.watchedDateDistanceSeconds(item)
        }.thenBy {
            it.watchedEndTimeDistanceMs(item)
        }.thenByDescending {
            it.iframeUrl.isKodikSourceUrl()
        }.thenByDescending {
            it.views ?: 0
        })
    }

    private fun List<AnimeVideo>.selectThumbnailVideo(
        item: HomeContinueWatchingItem,
        playbackVideo: AnimeVideo?,
    ): AnimeVideo? {
        if (playbackVideo?.iframeUrl?.isKodikSourceUrl() == true) return playbackVideo

        val playbackEpisodeKey = playbackVideo?.episode.orEmpty().episodeKey()
        val episodeKodikVideo = takeIf { playbackEpisodeKey.isNotBlank() }
            ?.filter { video ->
                video.episode.episodeKey() == playbackEpisodeKey &&
                        video.iframeUrl.isKodikSourceUrl()
            }
            ?.bestThumbnailCandidate()

        return episodeKodikVideo ?: guessCandidates(item).bestThumbnailCandidate()
    }

    private fun List<AnimeVideo>.bestThumbnailCandidate(): AnimeVideo? =
        maxWithOrNull(compareBy<AnimeVideo> {
            it.iframeUrl.isKodikSourceUrl()
        }.thenBy {
            it.views ?: 0
        })

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

    private fun List<AnimeVideo>.guessCandidates(item: HomeContinueWatchingItem): List<AnimeVideo> {
        if (item.animeId <= 0 || item.positionMs <= 0L) return emptyList()
        return filter { video ->
            video.iframeUrl.isSupportedSourceUrl() &&
                    video.durationSeconds.fitsPosition(item.positionMs)
        }
    }

    private fun Int?.fitsPosition(positionMs: Long): Boolean =
        this == null || positionMs <= secondsToMillis()

    private fun HomeContinueWatchingItem.needsServerWatchedResolution(): Boolean =
        videoId <= 0 && episode.isBlank() && episodeUrl.isBlank() && positionMs > 0L

    private fun AnimeVideo.watchedDateDistanceSeconds(item: HomeContinueWatchingItem): Long {
        val itemDateSeconds = item.updatedAt.takeIf { it > 0L }?.div(1_000L) ?: return 0L
        return abs((watchedDateSeconds ?: return Long.MAX_VALUE) - itemDateSeconds)
    }

    private fun AnimeVideo.watchedEndTimeDistanceMs(item: HomeContinueWatchingItem): Long {
        if (item.positionMs <= 0L) return 0L
        val watchedPositionMs = watchedEndTimeSeconds?.secondsToMillis() ?: return Long.MAX_VALUE
        return abs(watchedPositionMs - item.positionMs)
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

    private companion object {
        const val VIDEO_ENRICH_LIMIT = 10
    }
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
