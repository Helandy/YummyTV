package su.afk.yummy.tv.domain.posts.usecase

import su.afk.yummy.tv.domain.posts.model.PostVote
import su.afk.yummy.tv.domain.posts.repository.PostsRepository
import javax.inject.Inject

class PostsUseCases @Inject constructor(private val repository: PostsRepository) {
    suspend fun categories() = repository.categories()
    suspend fun page(category: String?, sort: String, limit: Int, skip: Int) =
        repository.posts(category, sort, limit, skip)

    suspend fun details(postId: Int) = repository.details(postId)
    suspend fun vote(postId: Int, vote: PostVote) = repository.vote(postId, vote)
    suspend fun removeVote(postId: Int) = repository.removeVote(postId)
}
