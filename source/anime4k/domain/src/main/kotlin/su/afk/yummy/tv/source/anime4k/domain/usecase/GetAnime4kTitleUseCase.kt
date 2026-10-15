package su.afk.yummy.tv.source.anime4k.domain.usecase

import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kTitle
import su.afk.yummy.tv.source.anime4k.domain.repository.Anime4kRepository
import javax.inject.Inject

/** Спрашивает сервер, есть ли у тайтла 4K-озвучки, и возвращает серии и названия озвучек без ссылок. */
class GetAnime4kTitleUseCase @Inject constructor(
    private val repository: Anime4kRepository,
) {
    suspend operator fun invoke(malId: Int): Anime4kTitle? = repository.getTitle(malId)
}
