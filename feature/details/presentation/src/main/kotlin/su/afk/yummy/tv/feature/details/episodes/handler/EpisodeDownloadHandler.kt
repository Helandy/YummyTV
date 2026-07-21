package su.afk.yummy.tv.feature.details.episodes.handler

import kotlinx.coroutines.CancellationException
import su.afk.yummy.tv.core.error.StringProvider
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.anime.kodikThumbnailIframeUrl
import su.afk.yummy.tv.core.utils.ResolveKodikThumbnailUrlUseCase
import su.afk.yummy.tv.domain.player.model.PlayerStreamRequest
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.domain.player.usecase.ResolvePlayerStreamUseCase
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadQualityOption
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadRequest
import su.afk.yummy.tv.domain.videodownload.usecase.CancelOrDeleteVideoDownloadUseCase
import su.afk.yummy.tv.domain.videodownload.usecase.EnqueueVideoDownloadUseCase
import su.afk.yummy.tv.domain.videodownload.usecase.PrepareVideoDownloadQualityOptionsUseCase
import su.afk.yummy.tv.feature.details.episodes.EpisodesState
import su.afk.yummy.tv.feature.details.presentation.R
import su.afk.yummy.tv.feature.player.isAllohaPlayerUrl
import su.afk.yummy.tv.feature.player.playerDisplayOrderPriority
import javax.inject.Inject

/** Owns source resolution, quality selection and enqueue state for episode downloads. */
internal class EpisodeDownloadHandler @Inject constructor(
    private val resolvePlayerStream: ResolvePlayerStreamUseCase,
    private val prepareDownloadQualities: PrepareVideoDownloadQualityOptionsUseCase,
    private val enqueueVideoDownload: EnqueueVideoDownloadUseCase,
    private val cancelOrDeleteVideoDownload: CancelOrDeleteVideoDownloadUseCase,
    private val resolveKodikThumbnailUrl: ResolveKodikThumbnailUrlUseCase,
    private val strings: StringProvider,
) {
    private var pendingCandidate: DownloadCandidate? = null
    private var replacementDownloadId: Long? = null

    fun beginNewDownload() {
        replacementDownloadId = null
    }

    fun beginReplacement(downloadId: Long) {
        replacementDownloadId = downloadId
    }

    fun clearPending() {
        pendingCandidate = null
        replacementDownloadId = null
    }

    fun dismissSourcePicker() {
        replacementDownloadId = null
    }

    fun dubbingSelection(
        videos: List<AnimeVideo>,
        statuses: Map<String, EpisodesState.EpisodeDownloadUiState>,
        resolvingKeys: Set<String>,
        excludedDubbing: String? = null,
    ): EpisodesState.EpisodeDownloadDubbingSelection {
        val availableVideos = videos.filter { video ->
            excludedDubbing == null || video.toDownloadDubbingName() != excludedDubbing
        }
        val options = availableVideos
            .groupBy { it.toDownloadDubbingName() }
            .entries
            .sortedWith(
                compareByDescending<Map.Entry<String, List<AnimeVideo>>> { (_, group) ->
                    group.sumOf { it.views ?: 0 }
                }.thenBy { (dubbing, _) -> dubbing }
            )
            .map { (dubbing, group) ->
                EpisodesState.EpisodeDownloadDubbingOption(
                    videos = group,
                    title = dubbing,
                    subtitle = group.map { it.player }
                        .distinct()
                        .joinToString(" / ")
                        .takeIf { it.isNotBlank() && it != dubbing },
                    status = group.aggregateDubbingDownloadStatus(statuses),
                    resolving = group.all { it.toDownloadStatusKey() in resolvingKeys },
                )
            }
        return EpisodesState.EpisodeDownloadDubbingSelection(
            episode = videos.firstOrNull()?.episode.orEmpty(),
            options = options,
            hasAlternativeDubbings = excludedDubbing != null,
        )
    }

    fun balancerSelection(
        videos: List<AnimeVideo>,
        statuses: Map<String, EpisodesState.EpisodeDownloadUiState>,
        resolvingKeys: Set<String>,
    ): EpisodesState.EpisodeDownloadBalancerSelection {
        val options = videos
            .sortedWith(
                compareByDescending<AnimeVideo> { it.views ?: 0 }
                    .thenBy {
                        minOf(
                            it.player.playerDisplayOrderPriority(),
                            it.iframeUrl.playerDisplayOrderPriority(),
                        )
                    }
                    .thenBy { it.player }
                    .thenBy { it.playerId ?: Int.MAX_VALUE }
                    .thenBy { it.id }
            )
            .map { video ->
                val key = video.toDownloadStatusKey()
                EpisodesState.EpisodeDownloadBalancerOption(
                    video = video,
                    title = video.player.ifBlank { video.dubbing },
                    subtitle = null,
                    status = statuses[key],
                    resolving = key in resolvingKeys,
                )
            }
        val firstVideo = videos.firstOrNull()
        return EpisodesState.EpisodeDownloadBalancerSelection(
            episode = firstVideo?.episode.orEmpty(),
            dubbing = firstVideo?.dubbing?.ifBlank { firstVideo.player }.orEmpty(),
            options = options,
        )
    }

    suspend fun prepare(video: AnimeVideo): EpisodeDownloadPrepareResult {
        val key = video.toDownloadStatusKey()
        return try {
            when (val result = resolvePlayerStream(
                PlayerStreamRequest(
                    iframeUrl = video.iframeUrl,
                    autoQualityLabel = strings.get(R.string.details_quality_auto),
                )
            )) {
                is PlayerStreamResolveResult.Stream -> prepareQualitySelection(video, result)
                is PlayerStreamResolveResult.KodikBlocked -> EpisodeDownloadPrepareResult.Failure(
                    key,
                    result.message ?: strings.get(R.string.details_download_resolve_error),
                )

                is PlayerStreamResolveResult.Unavailable -> EpisodeDownloadPrepareResult.Failure(
                    key,
                    result.message ?: strings.get(R.string.details_download_dubbing_unavailable),
                )

                PlayerStreamResolveResult.Failed,
                PlayerStreamResolveResult.Unsupported -> EpisodeDownloadPrepareResult.Failure(
                    key,
                    strings.get(R.string.details_download_resolve_error),
                )
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            EpisodeDownloadPrepareResult.Failure(
                key,
                error.localizedMessage ?: strings.get(R.string.details_download_resolve_error),
            )
        }
    }

    suspend fun enqueue(
        option: EpisodesState.EpisodeDownloadQualityOption,
        animeId: Int,
        animeTitle: String,
        posterUrl: String,
        episodeVideos: List<AnimeVideo>,
    ): EpisodeDownloadEnqueueResult {
        val candidate = pendingCandidate ?: return EpisodeDownloadEnqueueResult.MissingCandidate
        val quality = candidate.options.firstOrNull {
            it.label == option.label && it.url == option.url
        } ?: return EpisodeDownloadEnqueueResult.MissingCandidate
        val video = candidate.video
        val screenshotUrl = episodeVideos.kodikThumbnailIframeUrl()
            ?.let { resolveKodikThumbnailUrl(it) }
            .orEmpty()
        val request = VideoDownloadRequest(
            animeId = animeId,
            animeTitle = animeTitle,
            posterUrl = posterUrl,
            episode = video.episode,
            videoId = video.id,
            playerName = video.player,
            playerId = video.playerId,
            dubbing = video.dubbing,
            iframeUrl = video.iframeUrl,
            screenshotUrl = screenshotUrl,
            quality = quality,
            headers = quality.headers.ifEmpty { candidate.headers },
        )
        replacementDownloadId?.let { downloadId ->
            if (mutationFailed { cancelOrDeleteVideoDownload(downloadId) }) {
                return EpisodeDownloadEnqueueResult.ReplacementDeleteFailed
            }
        }
        val enqueueFailed = mutationFailed { enqueueVideoDownload(request) }
        clearPending()
        return if (enqueueFailed) {
            EpisodeDownloadEnqueueResult.EnqueueFailed
        } else {
            EpisodeDownloadEnqueueResult.Success
        }
    }

    suspend fun delete(downloadId: Long) {
        cancelOrDeleteVideoDownload(downloadId)
    }

    fun downloadStatusKey(video: AnimeVideo): String = video.toDownloadStatusKey()

    fun downloadDubbingName(video: AnimeVideo): String = video.toDownloadDubbingName()

    private fun prepareQualitySelection(
        video: AnimeVideo,
        result: PlayerStreamResolveResult.Stream,
    ): EpisodeDownloadPrepareResult {
        val key = video.toDownloadStatusKey()
        val isAlloha = video.player.isAllohaPlayerUrl() || video.iframeUrl.isAllohaPlayerUrl()
        val options = prepareDownloadQualities(
            streamUrl = result.url,
            qualityMap = result.qualities,
            qualityHeaders = result.qualityHeaders,
            numericQualitiesOnly = isAlloha,
        )
        if (options.isEmpty()) {
            pendingCandidate = null
            return EpisodeDownloadPrepareResult.Failure(
                key,
                strings.get(R.string.details_download_resolve_error),
            )
        }
        pendingCandidate = DownloadCandidate(video, options, result.headers)
        return EpisodeDownloadPrepareResult.Ready(
            key = key,
            selection = EpisodesState.EpisodeDownloadQualitySelection(
                videoId = video.id,
                episode = video.episode,
                options = options.map {
                    EpisodesState.EpisodeDownloadQualityOption(
                        it.label,
                        it.url
                    )
                },
            ),
        )
    }

    private suspend fun mutationFailed(block: suspend () -> Unit): Boolean = try {
        block()
        false
    } catch (error: CancellationException) {
        throw error
    } catch (_: Throwable) {
        true
    }

    private data class DownloadCandidate(
        val video: AnimeVideo,
        val options: List<VideoDownloadQualityOption>,
        val headers: Map<String, String>,
    )

    private companion object {
        fun AnimeVideo.toDownloadStatusKey(): String =
            listOf(id.toString(), iframeUrl).joinToString("|")

        fun AnimeVideo.toDownloadDubbingName(): String = dubbing.ifBlank { player }

        fun List<AnimeVideo>.aggregateDubbingDownloadStatus(
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
    }
}

internal sealed interface EpisodeDownloadPrepareResult {
    val key: String

    data class Ready(
        override val key: String,
        val selection: EpisodesState.EpisodeDownloadQualitySelection,
    ) : EpisodeDownloadPrepareResult

    data class Failure(
        override val key: String,
        val message: String,
    ) : EpisodeDownloadPrepareResult
}

internal enum class EpisodeDownloadEnqueueResult {
    Success,
    ReplacementDeleteFailed,
    EnqueueFailed,
    MissingCandidate,
}
