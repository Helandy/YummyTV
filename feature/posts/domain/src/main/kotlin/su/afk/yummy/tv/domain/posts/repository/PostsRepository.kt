package su.afk.yummy.tv.domain.posts.repository

import su.afk.yummy.tv.domain.posts.model.PostCategory
import su.afk.yummy.tv.domain.posts.model.PostDetails
import su.afk.yummy.tv.domain.posts.model.PostReaction
import su.afk.yummy.tv.domain.posts.model.PostSummary
import su.afk.yummy.tv.domain.posts.model.PostVote

interface PostsRepository {
    suspend fun categories(): List<PostCategory>
    suspend fun posts(category: String?, sort: String, limit: Int, skip: Int): List<PostSummary>
    suspend fun details(postId: Int): PostDetails
    suspend fun vote(postId: Int, vote: PostVote): PostReaction
    suspend fun removeVote(postId: Int): PostReaction
}
