package su.afk.yummy.tv.domain.posts.usecase

import su.afk.yummy.tv.domain.posts.repository.PostsRepository
import javax.inject.Inject

/** Загружает страницу постов для выбранной категории и сортировки. */
class GetPostsUseCase @Inject constructor(private val repository: PostsRepository) {
    suspend operator fun invoke(category: String?, sort: String, limit: Int, skip: Int) =
        repository.posts(category, sort, limit, skip)
}
