package su.afk.yummy.tv.domain.bloggers.usecase

import su.afk.yummy.tv.domain.bloggers.repository.BloggerVideosRepository
import javax.inject.Inject

/** Загружает подробные данные выбранного видео блогера. */
class GetBloggerVideoDetailsUseCase @Inject constructor(private val repository: BloggerVideosRepository) {
    suspend operator fun invoke(id: Int) = repository.getVideo(id)
}
