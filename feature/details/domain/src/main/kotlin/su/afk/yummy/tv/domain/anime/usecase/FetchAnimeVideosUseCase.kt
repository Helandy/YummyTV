package su.afk.yummy.tv.domain.anime.usecase

import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.domain.anime.repository.AnimeRepository
import javax.inject.Inject

/**
 * Загружает видео аниме строго из сети и сохраняет их в кеш. В отличие от
 * [RefreshAnimeVideosUseCase] не откатывается на кеш: ошибка сети пробрасывается, так что
 * результат — заведомо актуальный список с сервера.
 */
class FetchAnimeVideosUseCase @Inject constructor(private val repository: AnimeRepository) {
    suspend operator fun invoke(animeId: Int): List<AnimeVideo> =
        repository.fetchAnimeVideos(animeId)
}
