package su.afk.yummy.tv.domain.posts.usecase

import su.afk.yummy.tv.domain.posts.repository.PostsRepository
import javax.inject.Inject

/** Загружает доступные категории постов. */
class GetPostCategoriesUseCase @Inject constructor(private val repository: PostsRepository) {
    suspend operator fun invoke() = repository.categories()
}
