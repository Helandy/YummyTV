package su.afk.yummy.tv.domain.posts.usecase

import su.afk.yummy.tv.domain.posts.model.PostVote
import su.afk.yummy.tv.domain.posts.repository.PostsRepository
import javax.inject.Inject

/** Сохраняет реакцию текущего пользователя на выбранный пост. */
class VotePostUseCase @Inject constructor(private val repository: PostsRepository) {
    suspend operator fun invoke(postId: Int, vote: PostVote) = repository.vote(postId, vote)
}
