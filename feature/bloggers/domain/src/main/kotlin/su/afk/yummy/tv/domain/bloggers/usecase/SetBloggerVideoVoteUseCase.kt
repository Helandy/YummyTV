package su.afk.yummy.tv.domain.bloggers.usecase

import su.afk.yummy.tv.domain.bloggers.model.BloggerVideoVote
import su.afk.yummy.tv.domain.bloggers.repository.BloggerVideosRepository
import javax.inject.Inject

/** Сохраняет реакцию текущего пользователя на видео блогера. */
class SetBloggerVideoVoteUseCase @Inject constructor(private val repository: BloggerVideosRepository) {
    suspend operator fun invoke(id: Int, vote: BloggerVideoVote) = repository.setVideoVote(id, vote)
}
