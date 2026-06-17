package su.afk.yummy.tv.data.account.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.account.AccountStorageStore
import su.afk.yummy.tv.core.storage.account.isFresh
import su.afk.yummy.tv.data.account.mapper.toCollectionSummary
import su.afk.yummy.tv.data.account.mapper.toUserFriend
import su.afk.yummy.tv.data.account.mapper.toUserPostSummary
import su.afk.yummy.tv.data.account.mapper.toUserReviewSummary
import su.afk.yummy.tv.data.account.network.YaniAccountApi
import su.afk.yummy.tv.data.account.storage.mapper.toCollectionSummaries
import su.afk.yummy.tv.data.account.storage.mapper.toCollectionsPageCache
import su.afk.yummy.tv.data.account.storage.mapper.toUserFriends
import su.afk.yummy.tv.data.account.storage.mapper.toUserFriendsPageCache
import su.afk.yummy.tv.data.account.storage.mapper.toUserPosts
import su.afk.yummy.tv.data.account.storage.mapper.toUserPostsPageCache
import su.afk.yummy.tv.data.account.storage.mapper.toUserReviews
import su.afk.yummy.tv.data.account.storage.mapper.toUserReviewsPageCache
import su.afk.yummy.tv.domain.account.model.AnimeCollectionSummary
import su.afk.yummy.tv.domain.account.model.UserFriend
import su.afk.yummy.tv.domain.account.model.UserPostSummary
import su.afk.yummy.tv.domain.account.model.UserReviewSummary
import su.afk.yummy.tv.domain.account.repository.UserProfileContentRepository

class YaniUserProfileContentRepository(
    private val api: YaniAccountApi,
    private val accountStorage: AccountStorageStore,
    private val settingsStore: SettingsStore,
) : UserProfileContentRepository {
    override suspend fun getFriends(userId: Int, limit: Int, offset: Int): List<UserFriend> =
        withContext(Dispatchers.IO) {
            val languageCode = settingsStore.yaniContentLanguage.first().apiCode
            val stored = accountStorage.getUserFriends(userId, languageCode, limit, offset)
            if (stored?.isFresh(ACCOUNT_MEDIUM_TTL_MS) == true) {
                return@withContext stored.toUserFriends()
            }

            try {
                api.getUserFriends(userId, limit, offset)
                    .mapNotNull { it.toUserFriend() }
                    .also { friends ->
                        accountStorage.saveUserFriends(
                            friends.toUserFriendsPageCache(
                                userId = userId,
                                language = languageCode,
                                limit = limit,
                                offset = offset,
                                cachedAt = System.currentTimeMillis(),
                            )
                        )
                    }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                stored?.toUserFriends()
                    ?: throw error
            }
        }

    override suspend fun getReviews(userId: Int, limit: Int, offset: Int): List<UserReviewSummary> =
        withContext(Dispatchers.IO) {
            val languageCode = settingsStore.yaniContentLanguage.first().apiCode
            val stored = accountStorage.getUserReviews(userId, languageCode, limit, offset)
            if (stored?.isFresh(ACCOUNT_MEDIUM_TTL_MS) == true) {
                return@withContext stored.toUserReviews()
            }

            try {
                api.getUserReviews(userId, limit, offset)
                    .mapNotNull { it.toUserReviewSummary() }
                    .also { reviews ->
                        accountStorage.saveUserReviews(
                            reviews.toUserReviewsPageCache(
                                userId = userId,
                                language = languageCode,
                                limit = limit,
                                offset = offset,
                                cachedAt = System.currentTimeMillis(),
                            )
                        )
                    }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                stored?.toUserReviews()
                    ?: throw error
            }
        }

    override suspend fun getPosts(userId: Int, limit: Int, offset: Int): List<UserPostSummary> =
        withContext(Dispatchers.IO) {
            val languageCode = settingsStore.yaniContentLanguage.first().apiCode
            val stored = accountStorage.getUserPosts(userId, languageCode, limit, offset)
            if (stored?.isFresh(ACCOUNT_MEDIUM_TTL_MS) == true) {
                return@withContext stored.toUserPosts()
            }

            try {
                api.getUserPosts(userId, limit, offset)
                    .mapNotNull { it.toUserPostSummary() }
                    .also { posts ->
                        accountStorage.saveUserPosts(
                            posts.toUserPostsPageCache(
                                userId = userId,
                                language = languageCode,
                                limit = limit,
                                offset = offset,
                                cachedAt = System.currentTimeMillis(),
                            )
                        )
                    }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                stored?.toUserPosts()
                    ?: throw error
            }
        }

    override suspend fun getCollections(
        userId: Int,
        limit: Int,
        offset: Int,
    ): List<AnimeCollectionSummary> =
        withContext(Dispatchers.IO) {
            val languageCode = settingsStore.yaniContentLanguage.first().apiCode
            val pageKey = userCollectionsPageKey(userId, limit, offset, languageCode)
            val stored = accountStorage.getCollections(pageKey)
            if (stored?.isFresh(ACCOUNT_MEDIUM_TTL_MS) == true) {
                return@withContext stored.toCollectionSummaries()
            }

            try {
                api.getUserCollections(userId, limit, offset)
                    .mapNotNull { it.toCollectionSummary() }
                    .also { collections ->
                        val cachedAt = System.currentTimeMillis()
                        accountStorage.saveCollections(
                            collections.toCollectionsPageCache(
                                pageKey = pageKey,
                                language = languageCode,
                                cachedAt = cachedAt,
                            ),
                            prunePagesCachedBefore = cachedAt - ACCOUNT_PAGE_CACHE_RETENTION_MS,
                        )
                    }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                stored?.toCollectionSummaries()
                    ?: throw error
            }
        }

    private fun userCollectionsPageKey(
        userId: Int,
        limit: Int,
        offset: Int,
        language: String,
    ): String =
        "user:$userId:collections:$limit:$offset:$language"
}
