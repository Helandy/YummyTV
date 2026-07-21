package su.afk.yummy.tv.domain.bloggers.usecase

import su.afk.yummy.tv.domain.bloggers.repository.BloggerVideosRepository
import javax.inject.Inject

/** Подписывает текущего пользователя на блогера или отменяет подписку. */
class SetBloggerSubscribedUseCase @Inject constructor(private val repository: BloggerVideosRepository) {
    suspend operator fun invoke(id: Int, subscribed: Boolean) =
        repository.setSubscribed(id, subscribed)
}
