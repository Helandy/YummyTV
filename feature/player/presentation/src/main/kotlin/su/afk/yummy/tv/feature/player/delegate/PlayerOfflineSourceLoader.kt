package su.afk.yummy.tv.feature.player.delegate

import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.core.model.anime.isContinueWatchingProgress
import su.afk.yummy.tv.domain.player.usecase.GetWatchProgressUseCase
import su.afk.yummy.tv.domain.videodownload.usecase.GetDownloadedEpisodesUseCase
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
 * ретраи и резолв source-graph; граф собирается из скачанных серий тайтла (для локального файла — из одной).
 */
internal class PlayerOfflineSourceLoader @Inject constructor(
    private val getVideoDownload: GetVideoDownloadUseCase,
    private val getDownloadedEpisodes: GetDownloadedEpisodesUseCase,
    private val getWatchProgress: GetWatchProgressUseCase,
    private val strings: StringProvider,
) {
    /** Скачанная серия, готовая к воспроизведению, или null. */
    suspend fun findDownloaded(downloadId: Long): VideoDownloadItem? =
        getVideoDownload(downloadId)?.takeIf { it.status.name == "Downloaded" }

    /** Скачанные серии того же тайтла и озвучки: по ним строится граф для перехода «Далее». */
    suspend fun findSiblings(item: VideoDownloadItem): List<VideoDownloadItem> =
        getDownloadedEpisodes(item.animeId, item.dubbing)

    /** Сохранённая позиция серии (как у онлайн-серий), или null, если продолжать нечего. */
    suspend fun resumePositionMs(item: VideoDownloadItem): Long? {
        if (item.animeId <= 0) return null
        val progress = getWatchProgress(item.animeId, item.episode) ?: return null
        return progress.positionMs.takeIf { progress.isContinueWatchingProgress() }
    }

    fun missingDownload(state: PlayerState.State): PlayerState.State = state.copy(
        playerError = strings.get(R.string.player_download_missing),
        streamUrl = null,
    )

    fun downloaded(
        state: PlayerState.State,
        item: VideoDownloadItem,
        siblings: List<VideoDownloadItem> = emptyList(),
        resumeFromMs: Long = 0L,
    ): PlayerState.State {
        val episodes = siblings.takeIf { list -> list.any { it.id == item.id } } ?: listOf(item)
        return state.copy(
            animeTitle = item.animeTitle,
            animeId = item.animeId,
            posterUrl = item.posterUrl,
            sourceGraph = episodesGraph(
                balancer = item.playerName,
                dubbing = item.dubbing,
                episodes = episodes.map { download ->
                    PlayerSourceEpisode(
                        id = download.videoId,
                        playerId = download.playerId,
                        number = download.episode,
                        iframeUrl = download.iframeUrl,
                        screenshotUrl = download.screenshotUrl,
                        downloadId = download.id,
                    )
                },
            ),
            sourceSelection = PlayerSourceSelection(episodeIndex = episodes.indexOfFirst { it.id == item.id }),
            resumeFromMs = resumeFromMs.coerceAtLeast(0L),
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
    }

    /** Локальный файл (ACTION_VIEW, content://) — с отдельным data-source ([PlayerState.State.isLocalFile]). */
    fun localFile(state: PlayerState.State, uri: String, title: String): PlayerState.State = state.copy(
        animeTitle = title.takeIf(String::isNotBlank)
            ?: strings.get(R.string.player_local_file_title),
        animeId = 0,
        posterUrl = "",
        sourceGraph = episodesGraph(
            balancer = "",
            dubbing = "",
            episodes = listOf(
                PlayerSourceEpisode(
                    id = 0,
                    playerId = null,
                    number = "",
                    iframeUrl = uri,
                    screenshotUrl = "",
                ),
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

    private fun episodesGraph(
        balancer: String,
        dubbing: String,
        episodes: List<PlayerSourceEpisode>,
    ) = PlayerSourceGraph(
        balancers = listOf(
            PlayerSourceBalancer(
                name = balancer,
                dubbings = listOf(PlayerSourceDubbing(name = dubbing, episodes = episodes)),
            ),
        ),
    )
}
