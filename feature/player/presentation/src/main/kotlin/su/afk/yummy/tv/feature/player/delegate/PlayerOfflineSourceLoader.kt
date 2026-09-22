package su.afk.yummy.tv.feature.player.delegate

import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import su.afk.yummy.tv.domain.videodownload.usecase.GetVideoDownloadUseCase
import su.afk.yummy.tv.feature.player.PlayerSourceBalancer
import su.afk.yummy.tv.feature.player.PlayerSourceDubbing
import su.afk.yummy.tv.feature.player.PlayerSourceEpisode
import su.afk.yummy.tv.feature.player.PlayerSourceGraph
import su.afk.yummy.tv.feature.player.PlayerSourceSelection
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.presentation.R
import javax.inject.Inject

/**
 * Источники без сети: скачанная серия и локальный файл, открытый извне.
 *
 * Оба идут офлайн-маршрутом ([PlayerState.State.isOfflinePlayback]), чтобы не запускать сетевые
 * ретраи и резолв source-graph; граф собирается из единственного эпизода.
 */
internal class PlayerOfflineSourceLoader @Inject constructor(
    private val getVideoDownload: GetVideoDownloadUseCase,
    private val strings: StringProvider,
) {
    /** Скачанная серия, готовая к воспроизведению, или null. */
    suspend fun findDownloaded(downloadId: Long): VideoDownloadItem? =
        getVideoDownload(downloadId)?.takeIf { it.status.name == "Downloaded" }

    fun missingDownload(state: PlayerState.State): PlayerState.State = state.copy(
        playerError = strings.get(R.string.player_download_missing),
        streamUrl = null,
    )

    fun downloaded(state: PlayerState.State, item: VideoDownloadItem): PlayerState.State = state.copy(
        animeTitle = item.animeTitle,
        animeId = item.animeId,
        posterUrl = item.posterUrl,
        sourceGraph = singleEpisodeGraph(
            balancer = item.playerName,
            dubbing = item.dubbing,
            episode = PlayerSourceEpisode(
                id = item.videoId,
                playerId = item.playerId,
                number = item.episode,
                iframeUrl = item.iframeUrl,
                screenshotUrl = item.screenshotUrl,
            ),
        ),
        sourceSelection = PlayerSourceSelection(),
        streamUrl = item.streamUrl,
        streamHeaders = item.headers,
        selectedQuality = item.qualityLabel,
        allohaAudioTracks = emptyList(),
        selectedAllohaAudioId = null,
        allohaSubtitles = emptyList(),
        selectedAllohaSubtitleIndex = null,
        isOfflinePlayback = true,
        offlineCacheKey = item.cacheKey,
        offlineCacheKeyScheme = item.cacheKeyScheme.storageValue,
        playerError = null,
    )

    /** Локальный файл (ACTION_VIEW, content://) — с отдельным data-source ([PlayerState.State.isLocalFile]). */
    fun localFile(state: PlayerState.State, uri: String, title: String): PlayerState.State = state.copy(
        animeTitle = title.takeIf(String::isNotBlank)
            ?: strings.get(R.string.player_local_file_title),
        animeId = 0,
        posterUrl = "",
        sourceGraph = singleEpisodeGraph(
            balancer = "",
            dubbing = "",
            episode = PlayerSourceEpisode(
                id = 0,
                playerId = null,
                number = "",
                iframeUrl = uri,
                screenshotUrl = "",
            ),
        ),
        sourceSelection = PlayerSourceSelection(),
        streamUrl = uri,
        streamHeaders = emptyMap(),
        streamQualityMap = null,
        selectedQuality = null,
        allohaAudioTracks = emptyList(),
        selectedAllohaAudioId = null,
        allohaSubtitles = emptyList(),
        selectedAllohaSubtitleIndex = null,
        isOfflinePlayback = true,
        isLocalFile = true,
        offlineCacheKey = null,
        playerError = null,
    )

    private fun singleEpisodeGraph(
        balancer: String,
        dubbing: String,
        episode: PlayerSourceEpisode,
    ) = PlayerSourceGraph(
        balancers = listOf(
            PlayerSourceBalancer(
                name = balancer,
                dubbings = listOf(PlayerSourceDubbing(name = dubbing, episodes = listOf(episode))),
            ),
        ),
    )
}
