package su.afk.yummy.tv.data.account.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.account.AccountStorageStore
import su.afk.yummy.tv.core.storage.account.isFresh
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
                val cache = api.getUserFriends(userId, limit, offset).toUserFriendsPageCache(
                    userId = userId,
                    language = languageCode,
                    limit = limit,
                    offset = offset,
                    cachedAt = System.currentTimeMillis(),
                )
                accountStorage.saveUserFriends(cache)
                cache.toUserFriends()
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
                val cache = api.getUserReviews(userId, limit, offset).toUserReviewsPageCache(
                    userId = userId,
                    language = languageCode,
                    limit = limit,
                    offset = offset,
                    cachedAt = System.currentTimeMillis(),
                )
                accountStorage.saveUserReviews(cache)
                cache.toUserReviews()
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
                val cache = api.getUserPosts(userId, limit, offset).toUserPostsPageCache(
                    userId = userId,
                    language = languageCode,
                    limit = limit,
                    offset = offset,
                    cachedAt = System.currentTimeMillis(),
                )
                accountStorage.saveUserPosts(cache)
                cache.toUserPosts()
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
                val cachedAt = System.currentTimeMillis()
                val cache = api.getUserCollections(userId, limit, offset).toCollectionsPageCache(
                    pageKey = pageKey,
                    language = languageCode,
                    cachedAt = cachedAt,
                )
                accountStorage.saveCollections(
                    cache,
                    prunePagesCachedBefore = cachedAt - ACCOUNT_PAGE_CACHE_RETENTION_MS,
                )
                cache.toCollectionSummaries()
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
