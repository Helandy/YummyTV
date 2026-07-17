package su.afk.yummy.tv.domain.bloggers.usecase

import su.afk.yummy.tv.domain.bloggers.model.BloggerVideoSort
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideoVote
import su.afk.yummy.tv.domain.bloggers.repository.BloggerVideosRepository
import javax.inject.Inject

class GetBloggerVideosUseCase @Inject constructor(private val repository: BloggerVideosRepository) {
    suspend operator fun invoke(
        category: String = "all",
        bloggerId: Int? = null,
        sort: BloggerVideoSort = BloggerVideoSort.NEW,
        limit: Int = 20,
        offset: Int = 0,
    ) = repository.getVideos(category, bloggerId, sort, limit, offset)
}

class GetAnimeBloggerVideosUseCase @Inject constructor(private val repository: BloggerVideosRepository) {
    suspend operator fun invoke(animeId: Int, limit: Int = 24, offset: Int = 0) =
        repository.getAnimeVideos(animeId, limit, offset)
}

class GetBloggersDirectoryUseCase @Inject constructor(private val repository: BloggerVideosRepository) {
    suspend operator fun invoke() = repository.getDirectory()
}

class GetBloggerDetailsUseCase @Inject constructor(private val repository: BloggerVideosRepository) {
    suspend operator fun invoke(id: Int) = repository.getBlogger(id)
}

class GetBloggerVideoDetailsUseCase @Inject constructor(private val repository: BloggerVideosRepository) {
    suspend operator fun invoke(id: Int) = repository.getVideo(id)
}

class SetBloggerSubscribedUseCase @Inject constructor(private val repository: BloggerVideosRepository) {
    suspend operator fun invoke(id: Int, subscribed: Boolean) =
        repository.setSubscribed(id, subscribed)
}

class SetBloggerVideoVoteUseCase @Inject constructor(private val repository: BloggerVideosRepository) {
    suspend operator fun invoke(id: Int, vote: BloggerVideoVote) = repository.setVideoVote(id, vote)
}
