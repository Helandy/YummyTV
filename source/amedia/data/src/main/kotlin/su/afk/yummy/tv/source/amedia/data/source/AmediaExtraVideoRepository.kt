package su.afk.yummy.tv.source.amedia.data.source

import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.domain.anime.repository.ExtraVideoRepository
import su.afk.yummy.tv.source.amedia.domain.usecase.FindAmediaTitleUseCase
import su.afk.yummy.tv.source.amedia.domain.usecase.GetAmediaEpisodesUseCase
import javax.inject.Inject

/**
 * Заглушка балансера «Amedia» (amd.online): отключён, поиск не выполняется, видео не отдаёт.
 */
internal class AmediaExtraVideoRepository @Inject constructor(
    @Suppress("unused") private val findTitle: FindAmediaTitleUseCase,
    @Suppress("unused") private val getEpisodes: GetAmediaEpisodesUseCase,
) : ExtraVideoRepository {

    @Suppress("UnusedParameter")
    override suspend fun videos(details: AnimeDetails): List<AnimeVideo> {
        // Балансер временно отключён: поиск по amd.online не выполняется, серий нет.
        return emptyList()
    }
}
