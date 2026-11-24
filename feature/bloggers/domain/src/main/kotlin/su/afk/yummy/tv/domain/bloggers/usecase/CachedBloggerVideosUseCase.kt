package su.afk.yummy.tv.domain.bloggers.usecase

import su.afk.yummy.tv.domain.bloggers.model.BloggerVideoSort
import su.afk.yummy.tv.domain.bloggers.repository.BloggerVideosRepository
import javax.inject.Inject

/** Читает сохранённую ленту. */
class CachedBloggerVideosUseCase @Inject constructor(private val repository: BloggerVideosRepository) {
    suspend operator fun invoke(category: String, bloggerId: Int?, sort: BloggerVideoSort, limit: Int, offset: Int) = repository.cachedVideos(category, bloggerId, sort, limit, offset)
}
