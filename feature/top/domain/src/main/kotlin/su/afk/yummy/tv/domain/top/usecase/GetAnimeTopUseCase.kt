package su.afk.yummy.tv.domain.top.usecase

import su.afk.yummy.tv.domain.top.model.AnimeTopPage
import su.afk.yummy.tv.domain.top.model.AnimeTopType
import su.afk.yummy.tv.domain.top.repository.AnimeTopRepository
import javax.inject.Inject

/** Загружает страницу топа аниме для выбранного рейтингового типа. */
class GetAnimeTopUseCase @Inject constructor(
    private val repository: AnimeTopRepository,
) {
    suspend operator fun invoke(type: AnimeTopType, limit: Int, offset: Int): AnimeTopPage =
        repository.getTopAnime(type, limit, offset)
}
