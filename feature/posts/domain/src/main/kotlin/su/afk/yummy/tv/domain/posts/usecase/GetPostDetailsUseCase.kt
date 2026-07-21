package su.afk.yummy.tv.domain.posts.usecase

import su.afk.yummy.tv.domain.posts.repository.PostsRepository
import javax.inject.Inject

/** Загружает подробные данные выбранного поста. */
class GetPostDetailsUseCase @Inject constructor(private val repository: PostsRepository) {
    suspend operator fun invoke(postId: Int) = repository.details(postId)
}
