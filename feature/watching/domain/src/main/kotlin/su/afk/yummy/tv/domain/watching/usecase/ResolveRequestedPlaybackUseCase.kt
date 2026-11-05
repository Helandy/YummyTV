package su.afk.yummy.tv.domain.watching.usecase

import su.afk.yummy.tv.core.common.coroutines.runSuspendCatching
import su.afk.yummy.tv.core.model.anime.bestUrl
import su.afk.yummy.tv.core.model.watching.ContinueWatchingLaunch
import su.afk.yummy.tv.core.model.watching.ContinueWatchingPlaybackVideo
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeDetailsUseCase
import su.afk.yummy.tv.domain.watching.utils.selectRequestedVideo
import javax.inject.Inject

/**
 * Разрешает явный запрос на воспроизведение (диплинк `yummytv://play/...`): в списке видео тайтла
 * ищет серию, балансер и озвучку, заданные в запросе, и возвращает запуск с нужной позиции.
 * Возвращает null, если подходящего видео нет: вызывающий открывает детали тайтла.
 */
class ResolveRequestedPlaybackUseCase @Inject internal constructor(
    private val loadContinueWatchingVideos: LoadContinueWatchingVideosUseCase,
    private val getAnimeDetails: GetAnimeDetailsUseCase,
) {

    suspend operator fun invoke(
        animeId: Int,
        episode: String?,
        player: String?,
        dubbing: String?,
        positionMs: Long,
    ): ContinueWatchingLaunch? {
        val video = loadContinueWatchingVideos(animeId = animeId, refresh = true)
            .selectRequestedVideo(episode = episode, player = player, dubbing = dubbing)
            ?: return null
        val details = runSuspendCatching { getAnimeDetails(animeId) }.getOrNull()
        return ContinueWatchingLaunch(
            animeId = animeId,
            animeTitle = details?.title.orEmpty(),
            posterUrl = details?.poster.bestUrl().orEmpty(),
            video = ContinueWatchingPlaybackVideo(
                id = video.id,
                episode = video.episode,
                dubbing = video.dubbing,
                player = video.player,
                playerId = video.playerId,
                iframeUrl = video.iframeUrl,
                views = video.views,
                skips = video.skips,
            ),
            resumeFromMs = positionMs.coerceAtLeast(0L),
        )
    }
}
