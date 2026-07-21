package su.afk.yummy.tv.domain.posts.usecase

import su.afk.yummy.tv.domain.posts.repository.PostsRepository
import javax.inject.Inject

/** Удаляет реакцию текущего пользователя с выбранного поста. */
class RemovePostVoteUseCase @Inject constructor(private val repository: PostsRepository) {
    suspend operator fun invoke(postId: Int) = repository.removeVote(postId)
}
