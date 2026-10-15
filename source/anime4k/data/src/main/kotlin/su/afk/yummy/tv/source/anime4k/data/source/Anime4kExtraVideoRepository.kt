package su.afk.yummy.tv.source.anime4k.data.source

import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.source.anime4k.data.flag.Anime4kFeatureGate
import su.afk.yummy.tv.source.anime4k.data.signing.RequestSigner
import su.afk.yummy.tv.source.anime4k.data.utils.parseAnime4kSourceUrl
import su.afk.yummy.tv.source.anime4k.data.utils.toSourceUrl
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kTarget
import su.afk.yummy.tv.source.anime4k.domain.repository.RewardedAdRepository
import su.afk.yummy.tv.source.anime4k.domain.usecase.GetAnime4kTitleUseCase
import su.afk.yummy.tv.domain.anime.repository.ExtraVideoRepository
import javax.inject.Inject

/** Добавляет в выдачу источник «4K»: только если сервер подтвердил 4K у тайтла. Ссылок на файлы здесь нет. */
internal class Anime4kExtraVideoRepository @Inject constructor(
    private val getTitle: GetAnime4kTitleUseCase,
    private val ads: RewardedAdRepository,
    private val featureGate: Anime4kFeatureGate,
    private val signer: RequestSigner,
) : ExtraVideoRepository {

    override suspend fun videos(details: AnimeDetails): List<AnimeVideo> {
        if (!featureGate.isEnabled() || !ads.isAvailable || !signer.isAvailable) {
            return emptyList()
        }
        val malId = details.malId ?: return emptyList()
        val title = getTitle(malId) ?: return emptyList()

        return title.episodes.flatMap { episode ->
            episode.dubbings.map { team ->
                val url = Anime4kTarget(malId, episode.season, episode.number, team).toSourceUrl()
                AnimeVideo(
                    // Отрицательный id: серверу Yani такие видео неизвестны, прогресс остаётся локальным.
                    id = -(url.hashCode() and Int.MAX_VALUE) - 1,
                    episode = episode.number,
                    dubbing = team,
                    player = PLAYER_NAME,
                    playerId = null,
                    iframeUrl = url,
                    durationSeconds = null,
                )
            }
        }
    }

    override suspend fun confirmOpen(video: AnimeVideo): Boolean {
        if (parseAnime4kSourceUrl(video.iframeUrl) == null) return true
        return ads.confirmBeforeOpen()
    }

    private companion object {
        const val PLAYER_NAME = "4K"
    }
}
