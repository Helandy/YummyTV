package su.afk.yummy.tv.domain.bloggers.usecase

import su.afk.yummy.tv.domain.bloggers.repository.BloggerVideosRepository
import javax.inject.Inject

/** Загружает страницу видео блогеров, связанных с выбранным аниме. */
class GetAnimeBloggerVideosUseCase @Inject constructor(private val repository: BloggerVideosRepository) {
    suspend operator fun invoke(animeId: Int, limit: Int = 24, offset: Int = 0) =
        repository.getAnimeVideos(animeId, limit, offset)
}
