package su.afk.yummy.tv.domain.posts.usecase

import su.afk.yummy.tv.domain.posts.repository.PostsRepository
import javax.inject.Inject

/** Читает сохранённую ленту. */
class CachedPostsUseCase @Inject constructor(private val repository: PostsRepository) {
    suspend operator fun invoke(category: String?, sort: String, limit: Int, skip: Int) = repository.cachedPosts(category, sort, limit, skip)
}
