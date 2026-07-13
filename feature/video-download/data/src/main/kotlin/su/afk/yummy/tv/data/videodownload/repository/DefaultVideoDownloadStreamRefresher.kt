package su.afk.yummy.tv.data.videodownload.repository

import kotlinx.coroutines.CancellationException
import su.afk.yummy.tv.data.videodownload.strategy.DownloadPlayerStrategyResolver
import su.afk.yummy.tv.domain.player.model.PlayerSourceVideo
import su.afk.yummy.tv.domain.player.model.PlayerStreamRequest
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.domain.player.repository.PlayerSourceRepository
import su.afk.yummy.tv.domain.player.usecase.ResolvePlayerStreamUseCase
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadRestartStream
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadStreamRefreshResult
import su.afk.yummy.tv.domain.videodownload.repository.VideoDownloadStreamRefresher
import su.afk.yummy.tv.domain.videodownload.usecase.PrepareVideoDownloadQualityOptionsUseCase
import javax.inject.Inject

class DefaultVideoDownloadStreamRefresher @Inject internal constructor(
    private val resolvePlayerStream: ResolvePlayerStreamUseCase,
    private val playerSourceRepository: PlayerSourceRepository,
    private val prepareDownloadQualities: PrepareVideoDownloadQualityOptionsUseCase,
    private val strategyResolver: DownloadPlayerStrategyResolver,
) : VideoDownloadStreamRefresher {

    override suspend fun refresh(
        item: VideoDownloadItem,
        autoQualityLabel: String,
    ): VideoDownloadStreamRefreshResult {
        val normalizedAutoLabel = autoQualityLabel.ifBlank { DEFAULT_AUTO_QUALITY_LABEL }
        val resolveQualityLabel = item.qualityLabel
            .takeIf { it.hasVideoQualityNumber() }
            ?: normalizedAutoLabel
        return runCatching {
            val source = item.refreshSource()
            val refreshedItem = item.copy(
                videoId = source.episode.id,
                playerName = source.playerName,
                playerId = source.episode.playerId,
                dubbing = source.dubbing,
                iframeUrl = source.episode.iframeUrl,
            )
            when (val result = resolvePlayerStream(
                PlayerStreamRequest(
                    iframeUrl = refreshedItem.iframeUrl,
                    autoQualityLabel = resolveQualityLabel,
                )
            )) {
                is PlayerStreamResolveResult.Stream -> result.toRefreshResult(
                    item = refreshedItem,
                    autoQualityLabel = normalizedAutoLabel,
                )

                is PlayerStreamResolveResult.KodikBlocked -> VideoDownloadStreamRefreshResult.Failure(
                    result.message ?: REFRESH_ERROR_MESSAGE,
                )

                is PlayerStreamResolveResult.Unavailable -> VideoDownloadStreamRefreshResult.Failure(
                    result.message ?: DUBBING_UNAVAILABLE_MESSAGE,
                )

                PlayerStreamResolveResult.Failed -> VideoDownloadStreamRefreshResult.Failure(
                    REFRESH_ERROR_MESSAGE,
                )

                PlayerStreamResolveResult.Unsupported -> VideoDownloadStreamRefreshResult.Failure(
                    UNSUPPORTED_REFRESH_MESSAGE,
                )
            }
        }.getOrElse { throwable ->
            if (throwable is CancellationException) throw throwable
            VideoDownloadStreamRefreshResult.Failure(
                throwable.message.takeIf { message ->
                    message == DUBBING_UNAVAILABLE_MESSAGE
                } ?: REFRESH_ERROR_MESSAGE,
            )
        }
    }

    private suspend fun VideoDownloadItem.refreshSource(): RefreshedDownloadSource {
        val videos = playerSourceRepository.getSources(
            animeId = animeId,
            forceRefreshVideos = true,
        ).videos
        val candidates = videos.filter { candidate ->
            candidate.episode == episode &&
                    candidate.player == playerName &&
                    candidate.dubbing == dubbing &&
                    candidate.iframeUrl.isNotBlank()
        }
        val refreshedEpisode = candidates.firstOrNull { it.id == videoId }
            ?: playerId?.let { previousPlayerId ->
                candidates.firstOrNull { it.playerId == previousPlayerId }
            }
            ?: candidates.firstOrNull()
            ?: error(DUBBING_UNAVAILABLE_MESSAGE)
        return RefreshedDownloadSource(
            playerName = refreshedEpisode.player,
            dubbing = refreshedEpisode.dubbing,
            episode = refreshedEpisode,
        )
    }

    private fun String.hasVideoQualityNumber(): Boolean =
        VIDEO_QUALITY_REGEX.containsMatchIn(this)

    private fun String.videoQualityNumber(): Int? =
        VIDEO_QUALITY_REGEX.find(this)
            ?.value
            ?.filter(Char::isDigit)
            ?.toIntOrNull()

    private fun PlayerStreamResolveResult.Stream.toRefreshResult(
        item: VideoDownloadItem,
        autoQualityLabel: String,
    ): VideoDownloadStreamRefreshResult {
        if (url.isBlank()) {
            return VideoDownloadStreamRefreshResult.Failure(BLANK_REFRESH_MESSAGE)
        }

        val strategy = strategyResolver.resolve(item)
        val availableQualities = prepareDownloadQualities(
            streamUrl = url,
            qualityMap = qualities,
            qualityHeaders = qualityHeaders,
            numericQualitiesOnly = strategy.numericQualitiesOnly,
        )
        val requestedQualityNumber = item.qualityLabel.videoQualityNumber()
        val preferredQuality = availableQualities.firstOrNull { quality ->
            quality.label == item.qualityLabel
        } ?: requestedQualityNumber?.let { requested ->
            availableQualities.firstOrNull { quality ->
                quality.label.videoQualityNumber() == requested
            }
        } ?: availableQualities.lastOrNull().takeIf { strategy.allowsQualityFallbackToHighest }
        val fallbackHeaders = if (strategy.reusesHeadersOnRefresh) item.headers else emptyMap()

        val stream = if (preferredQuality != null) {
            val refreshedHeaders = preferredQuality.headers
                .ifEmpty { headers }
                .withReusableFallbackHeaders(fallbackHeaders)
            VideoDownloadRestartStream(
                videoId = item.videoId,
                playerName = item.playerName,
                playerId = item.playerId,
                dubbing = item.dubbing,
                iframeUrl = item.iframeUrl,
                qualityLabel = preferredQuality.label,
                url = preferredQuality.url,
                headers = refreshedHeaders,
            )
        } else {
            val refreshedHeaders = headers.withReusableFallbackHeaders(fallbackHeaders)
            VideoDownloadRestartStream(
                videoId = item.videoId,
                playerName = item.playerName,
                playerId = item.playerId,
                dubbing = item.dubbing,
                iframeUrl = item.iframeUrl,
                qualityLabel = item.qualityLabel.takeIf { requestedQualityNumber != null }
                    ?: autoQualityLabel,
                url = url,
                headers = refreshedHeaders,
            )
        }

        return VideoDownloadStreamRefreshResult.Success(stream)
    }

    private fun Map<String, String>.withReusableFallbackHeaders(
        fallbackHeaders: Map<String, String>,
    ): Map<String, String> {
        if (fallbackHeaders.isEmpty()) return this
        return buildMap {
            putAll(this@withReusableFallbackHeaders)
            fallbackHeaders.forEach { (key, value) ->
                if (
                    value.isNotBlank() &&
                    key.isReusableDownloadHeader() &&
                    keys.none { existing -> existing.equals(key, ignoreCase = true) }
                ) {
                    put(key, value)
                }
            }
        }
    }

    private fun String.isReusableDownloadHeader(): Boolean =
        !equals(ACCESS_CONTROL_REQUEST_HEADERS_HEADER, ignoreCase = true) &&
                !equals(ACCESS_CONTROL_REQUEST_METHOD_HEADER, ignoreCase = true) &&
                !equals(HOST_HEADER, ignoreCase = true) &&
                !equals(RANGE_HEADER, ignoreCase = true) &&
                !startsWith(SEC_FETCH_HEADER_PREFIX, ignoreCase = true)

    private companion object {
        const val DEFAULT_AUTO_QUALITY_LABEL = "Auto"
        const val REFRESH_ERROR_MESSAGE = "Could not refresh download link"
        const val UNSUPPORTED_REFRESH_MESSAGE =
            "Download link refresh is unsupported for this player"
        const val DUBBING_UNAVAILABLE_MESSAGE =
            "Sorry, the selected dubbing is unavailable. Try another one."
        const val BLANK_REFRESH_MESSAGE = "Resolved download link is empty"
        const val ACCESS_CONTROL_REQUEST_HEADERS_HEADER = "Access-Control-Request-Headers"
        const val ACCESS_CONTROL_REQUEST_METHOD_HEADER = "Access-Control-Request-Method"
        const val HOST_HEADER = "Host"
        const val RANGE_HEADER = "Range"
        const val SEC_FETCH_HEADER_PREFIX = "Sec-Fetch-"
        val VIDEO_QUALITY_REGEX = Regex("""\d{3,4}p?""", RegexOption.IGNORE_CASE)
    }

    private data class RefreshedDownloadSource(
        val playerName: String,
        val dubbing: String,
        val episode: PlayerSourceVideo,
    )
}
