package su.afk.yummy.tv.source.anime4k.data.source

import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.source.anime4k.data.flag.Anime4kFeatureGate
import su.afk.yummy.tv.core.network.yummy.SiteRequestSigner
import su.afk.yummy.tv.source.anime4k.data.utils.toSourceUrl
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kLookup
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kTarget
import su.afk.yummy.tv.source.anime4k.domain.usecase.GetAnime4kTitleUseCase
import su.afk.yummy.tv.domain.anime.repository.ExtraVideoRepository
import javax.inject.Inject

/** Добавляет в выдачу источник «4K»: только если сервер подтвердил 4K у тайтла. Ссылок на файлы здесь нет. */
internal class Anime4kExtraVideoRepository @Inject constructor(
    private val getTitle: GetAnime4kTitleUseCase,
    private val featureGate: Anime4kFeatureGate,
    private val signers: Set<@JvmSuppressWildcards SiteRequestSigner>,
) : ExtraVideoRepository {

    override val sourceId: String = "anime4k"

    override suspend fun videos(details: AnimeDetails): List<AnimeVideo> {
        if (!featureGate.isEnabled() || signers.none { it.isAvailable }) {
            return emptyList()
        }
        val malId = details.malId ?: return emptyList()
        val lookup = Anime4kLookup(
            malId = malId,
            title = details.title,
            otherTitles = details.otherTitles,
            year = details.year,
            type = details.type,
            episodes = details.episodes?.count,
        )
        val title = getTitle(lookup) ?: return emptyList()

        return title.episodes.flatMap { episode ->
            // Сервер может отдать одну озвучку серии дважды (два 2160p-плеера у студии): одинаковый URL даёт одинаковый id.
            episode.dubbings.distinct().map { team ->
                val url = Anime4kTarget(malId, episode.season, episode.number, team).toSourceUrl()
                AnimeVideo(
                    // Отрицательный id: серверу Yani такие видео неизвестны, прогресс остаётся локальным.
                    id = -(url.hashCode() and Int.MAX_VALUE) - 1,
                    episode = episode.number,
                    dubbing = team,
                    player = if (title.isPossible) PLAYER_NAME_POSSIBLE else PLAYER_NAME,
                    playerId = null,
                    iframeUrl = url,
                    durationSeconds = null,
                )
            }
        }
    }

    private companion object {
        const val PLAYER_NAME = "4K"

        /** 4K сервер больше не подтверждает, но серии помнит: возможно, вернётся. */
        const val PLAYER_NAME_POSSIBLE = "4K?"
    }
}
