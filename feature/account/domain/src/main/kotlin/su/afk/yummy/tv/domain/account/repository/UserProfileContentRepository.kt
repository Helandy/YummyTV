package su.afk.yummy.tv.domain.account.repository

import su.afk.yummy.tv.domain.account.model.AnimeCollectionSummary
import su.afk.yummy.tv.domain.account.model.UserFriend
import su.afk.yummy.tv.domain.account.model.UserPostSummary
import su.afk.yummy.tv.domain.account.model.UserReviewSummary

interface UserProfileContentRepository {
    suspend fun getFriends(userId: Int, limit: Int, offset: Int): List<UserFriend>
    suspend fun getReviews(userId: Int, limit: Int, offset: Int): List<UserReviewSummary>
    suspend fun getPosts(userId: Int, limit: Int, offset: Int): List<UserPostSummary>
    suspend fun getCollections(userId: Int, limit: Int, offset: Int): List<AnimeCollectionSummary>
}
