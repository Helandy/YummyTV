package su.afk.yummy.tv.domain.bloggers.usecase

import su.afk.yummy.tv.domain.bloggers.model.BloggerVideoSort
import su.afk.yummy.tv.domain.bloggers.repository.BloggerVideosRepository
import javax.inject.Inject

/** Загружает страницу видео блогеров с выбранными фильтрами и сортировкой. */
class GetBloggerVideosUseCase @Inject constructor(private val repository: BloggerVideosRepository) {
    suspend operator fun invoke(
        category: String = "all",
        bloggerId: Int? = null,
        sort: BloggerVideoSort = BloggerVideoSort.NEW,
        limit: Int = 20,
        offset: Int = 0,
    ) = repository.getVideos(category, bloggerId, sort, limit, offset)
}
