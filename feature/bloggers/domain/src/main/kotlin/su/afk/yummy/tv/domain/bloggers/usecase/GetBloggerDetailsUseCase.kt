package su.afk.yummy.tv.domain.bloggers.usecase

import su.afk.yummy.tv.domain.bloggers.repository.BloggerVideosRepository
import javax.inject.Inject

/** Загружает профиль выбранного блогера. */
class GetBloggerDetailsUseCase @Inject constructor(private val repository: BloggerVideosRepository) {
    suspend operator fun invoke(id: Int) = repository.getBlogger(id)
}
